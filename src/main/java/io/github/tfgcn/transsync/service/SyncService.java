package io.github.tfgcn.transsync.service;

import com.google.gson.reflect.TypeToken;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.I18n;
import io.github.tfgcn.transsync.paratranz.api.StringsApi;
import io.github.tfgcn.transsync.paratranz.error.ApiException;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.StageEnum;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.FileUploadRespDto;
import io.github.tfgcn.transsync.paratranz.model.files.TranslationDto;
import io.github.tfgcn.transsync.paratranz.model.strings.StringItem;
import io.github.tfgcn.transsync.service.model.*;
import io.github.tfgcn.transsync.utils.JsonUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * desc: 上传原文服务
 *
 * @author yanmaoyuan
 */
@Slf4j
public class SyncService {

    @Setter
    private StringsApi stringsApi;
    @Setter
    private FilesApi filesApi;
    @Setter
    private Integer projectId;
    private String workDir;
    @Setter
    private List<FileScanRule> rules;

    private List<FilesDto> remoteFiles;
    private Map<String, FilesDto> remoteFilesMap;

    private final FileScanService fileScanService;

    public SyncService() {
        this.remoteFiles = Collections.emptyList();
        this.remoteFilesMap = Collections.emptyMap();

        this.fileScanService = new FileScanService();
    }

    /**
     * 设置工作目录
     *
     * @param workspace 工作目录
     */
    public void setWorkspace(String workspace) throws IOException {
        File workspaceFolder = new File(workspace);
        if (!workspaceFolder.exists()) {
            log.warn("folder not found: {}", workspace);
            throw new IOException(Constants.MSG_FOLDER_NOT_FOUND + ":" + workspace);
        }
        if (!workspaceFolder.isDirectory()) {
            log.warn("not a directory: {}", workspace);
            throw new IOException(Constants.MSG_FOLDER_INVALID);
        }
        this.workDir = workspaceFolder.getCanonicalPath().replace("\\", SEPARATOR);
        log.info("set workdir to:{}", workDir);
    }

    /**
     * 获取远程文件列表
     *
     */
    public List<FilesDto> fetchRemoteFiles() throws IOException, ApiException {
        log.info("Fetching remote files...");
        // 查询已有的文件列表
        List<FilesDto> fileList = filesApi.getFiles(projectId).execute().body();
        if (fileList == null || fileList.isEmpty()) {
            log.info("No remote files found");
            remoteFilesMap = Collections.emptyMap();
            remoteFiles = Collections.emptyList();
        } else {
            remoteFiles = new ArrayList<>(fileList.size());
            remoteFilesMap = new HashMap<>();
            for (FilesDto file : fileList) {
                remoteFiles.add(file);
                remoteFilesMap.put(file.getName(), file);
                log.debug("{}", file.getName());
            }
            log.info("Found remote files: {}", fileList.size());
        }
        return remoteFiles;
    }

    /**
     * 获取待上传的文件列表
     */
    public List<FileScanResult> getSourceFiles() {
        if (rules == null || rules.isEmpty()) {
            throw new RuntimeException(I18n.getString("message.noRules"));
        }

        List<FileScanResult> fileList = new ArrayList<>(100);

        Set<String> distinct = new HashSet<>();
        for (FileScanRule rule : rules) {
            if (Boolean.FALSE.equals(rule.getEnabled())) {
                continue;
            }
            FileScanRequest request = new FileScanRequest();
            request.setWorkspace(workDir);
            request.setSourceFilePattern(rule.getSourcePattern());
            request.setTranslationFilePattern(rule.getTranslationPattern());
            request.setSrcLang(rule.getSrcLang());
            request.setDestLang(rule.getDestLang());
            request.setIgnores(rule.getIgnores());
            try {
                List<FileScanResult> results = fileScanService.scanAndMapFiles(request);
                for (FileScanResult item : results) {
                    if (!distinct.contains(item.getTranslationFilePath())) {
                        distinct.add(item.getTranslationFilePath());
                        fileList.add(item);
                    } else {
                        log.debug("Duplicated file:{}", item.getTranslationFilePath());
                    }
                }
            } catch (Exception ex) {
                log.error("扫描文件失败, rule:{}", rule, ex);
            }
        }

        // 按照源文件路径进行排序
        fileList.sort(Comparator.comparing(FileScanResult::getSourceFilePath));
        return fileList;
    }

    /**
     * 上传原文
     */
    public void uploadSources() throws IOException, ApiException {
        // 扫描远程服务器上已有的文件
        fetchRemoteFiles();

        log.info("Scanning language files");
        // 扫描语言文件夹下的 en_us 目录，把文本上传到 paratranz
        List<FileScanResult> fileList = getSourceFiles();

        log.info("Found files: {}", fileList.size());
        for (FileScanResult item : fileList) {
            File file = getAbsoluteFile(item.getSourceFilePath());
            String remoteFolder = item.getTranslationFileFolder();

            FilesDto remoteFile = remoteFilesMap.get(item.getTranslationFilePath());
            if (remoteFile == null) {
                uploadFile(remoteFolder, file);
            } else {
                updateFile(remoteFile, remoteFolder, file);
            }
        }
    }

    public void updateFile(FilesDto remoteFile, String remoteFolder, File file) throws IOException, ApiException {
        try (FileInputStream fis = new FileInputStream(file)) {
            String md5 = DigestUtils.md5Hex(fis);
            if (md5.equals(remoteFile.getHash())) {
                log.info("[Not modified] {}/{}", remoteFolder, file.getName());
                return;
            }
        }

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                RequestBody.create(Constants.MULTIPART_FORM_DATA, file));

        Response<FileUploadRespDto> updateResp = filesApi.updateFile(projectId, remoteFile.getId(), filePart).execute();
        if (updateResp.isSuccessful()) {
            log.info("[Updated] {}/{}", remoteFolder, file.getName());
        }
    }

    public void uploadFile(String remoteFolder, File file) throws IOException, ApiException {

        log.info("[NEW] {}/{}", remoteFolder, file.getName());
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                RequestBody.create(Constants.MULTIPART_FORM_DATA, file));

        RequestBody pathPart = RequestBody.create(Constants.MULTIPART_FORM_DATA, remoteFolder);

        Response<FileUploadRespDto> uploadResp = filesApi.uploadFile(projectId, pathPart, filePart).execute();
        if (uploadResp.isSuccessful()) {
            log.info("upload success: {}", uploadResp.body());
        }
    }

    /**
     * 上传原始文件
     * 这个接口是给GUI用的，返回结果用于界面展示。
     * @param scannedFile
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public String uploadSourceFile(FileScanResult scannedFile) throws IOException, ApiException {
        File file = getAbsoluteFile(scannedFile.getSourceFilePath());
        String remoteFolder = scannedFile.getTranslationFileFolder();

        // 生成上传 paratranz 的最终文件名。可用于比较远程文件是否已存在
        FilesDto remoteFile = remoteFilesMap.get(scannedFile.getTranslationFilePath());
        if (remoteFile == null) {
            uploadFile(remoteFolder, file);
            return I18n.getString("label.completed.newFile");
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                if (md5.equals(remoteFile.getHash())) {
                    return I18n.getString("label.skipped.notModified");
                }
            }

            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                    RequestBody.create(Constants.MULTIPART_FORM_DATA, file));

            Response<FileUploadRespDto> updateResp = filesApi.updateFile(projectId, remoteFile.getId(), filePart).execute();
            if (updateResp.isSuccessful()) {
                log.info("update success: {}", updateResp.body());
            }

            return I18n.getString("label.completed.updated");
        }
    }

    /**
     * 下载远程文件
     * @throws IOException
     * @throws ApiException
     */
    public void downloadTranslations() throws IOException, ApiException {
        // 扫描远程文件文件
        fetchRemoteFiles();

        List<FileScanResult> sourceFiles = getSourceFiles();
        if (sourceFiles.isEmpty() || remoteFiles.isEmpty()) {
            log.info("No files to download");
            return;
        }

        Map<String, FileScanResult> sourceFilesMap = sourceFiles.stream().collect(Collectors.toMap(FileScanResult::getTranslationFilePath, file -> file));

        // 执行上传操作
        for (FilesDto remoteFile : remoteFiles) {
            if (sourceFilesMap.containsKey(remoteFile.getName())) {
                List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
                if (translations == null || translations.isEmpty()) {
                    log.info("缺少翻译: {}", remoteFile.getName());
                } else {
                    FileScanResult scannedFile = sourceFilesMap.get(remoteFile.getName());
                    saveTranslations(remoteFile, translations, scannedFile.getSourceFilePath());
                }
            } else {
                // remove everything not in source files
                log.info("忽略远程文件: {}", remoteFile.getName());
            }
        }
    }

    public String downloadTranslation(FilesDto remoteFile) throws IOException, ApiException {
        List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
        if (translations == null || translations.isEmpty()) {
            return I18n.getString("label.skipped.notTranslated");
        }

        DownloadTranslationResult result = saveTranslations(remoteFile, translations);
        if ("skip".equals(result.getStatus())) {
            return I18n.getString("label.skipped.notModified") + " " + formatFileSize(result.getBytes());
        } else {
            return I18n.getString("label.completed.updated") + " " + formatFileSize(result.getBytes());
        }
    }

    public String downloadTranslation(FileDownloadRequest remoteFile) throws IOException, ApiException {
        List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
        if (translations == null || translations.isEmpty()) {
            return I18n.getString("label.skipped.notTranslated");
        }

        DownloadTranslationResult result = saveTranslations(remoteFile, translations, remoteFile.getSourceFilePath());
        if ("skip".equals(result.getStatus())) {
            return I18n.getString("label.skipped.notModified") + " " + formatFileSize(result.getBytes());
        } else {
            return I18n.getString("label.completed.updated") + " " + formatFileSize(result.getBytes());
        }
    }

    /**
     * 保存译文到文件
     *
     * @param remoteFile 源文件
     * @param translations 翻译结果
     * @return 文件大小
     * @throws IOException 保存失败时抛出
     */
    public DownloadTranslationResult saveTranslations(FilesDto remoteFile, List<TranslationDto> translations) throws IOException {
        DownloadTranslationResult result = new DownloadTranslationResult();

        // 创建一个 Map 用于存储翻译结果，使用 LinkedHashMap 保持插入顺序。
        Map<String, String> map = new LinkedHashMap<>();
        for (TranslationDto item : translations) {
            StageEnum stage = StageEnum.of(item.getStage());
            if (stage == StageEnum.HIDDEN || stage == StageEnum.UNTRANSLATED) {
                map.put(item.getKey(), item.getOriginal());
            } else {
                map.put(item.getKey(), item.getTranslation());
            }
        }

        String body = JsonUtils.toJson(map);

        File file = getAbsoluteFile(remoteFile.getName());
        FileUtils.createParentDirectories(file);

        // 文件存在
        if (file.exists()) {
            // 比较文件内容是否更新
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                String downloadMd5 = DigestUtils.md5Hex(body);
                if (md5.equals(downloadMd5)) {
                    log.info("File not modified: {}", remoteFile.getName());
                    result.setStatus("skip");
                } else {
                    JsonUtils.writeFile(file, map);
                    log.info("File updated: {}", remoteFile.getName());
                    result.setStatus("update");
                }
            }
        } else {
            // 文件不存在，直接写入
            JsonUtils.writeFile(file, map);
            log.info("File saved: {}", remoteFile.getName());
            result.setStatus("create");
        }

        result.setBytes(body.getBytes(StandardCharsets.UTF_8).length);
        return result;
    }

    /**
     * 保存译文到文件
     *
     * @param remoteFile 源文件
     * @param translations 翻译结果
     * @param sourceFilePath 源文件路径
     * @return 文件大小
     * @throws IOException 保存失败时抛出
     */
    public DownloadTranslationResult saveTranslations(FilesDto remoteFile, List<TranslationDto> translations, String sourceFilePath) throws IOException {
        // read source json
        File sourceFile = getAbsoluteFile(sourceFilePath);
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> sourceDict = JsonUtils.readFile(sourceFile, mapType);

        DownloadTranslationResult result = new DownloadTranslationResult();

        // 创建一个 Map 用于存储翻译结果，使用 LinkedHashMap 保持插入顺序。
        Map<String, String> translatedDict = new LinkedHashMap<>();
        for (TranslationDto item : translations) {
            StageEnum stage = StageEnum.of(item.getStage());
            if (stage == StageEnum.HIDDEN || stage == StageEnum.UNTRANSLATED) {
                translatedDict.put(item.getKey(), item.getOriginal());
            } else {
                translatedDict.put(item.getKey(), item.getTranslation());
            }
        }

        // 递归更新嵌套结构
        updateNestedStructure(sourceDict, translatedDict, null);
        String body = JsonUtils.toJson(sourceDict);

        File file = getAbsoluteFile(remoteFile.getName());
        FileUtils.createParentDirectories(file);

        // 文件存在
        if (file.exists()) {
            // 比较文件内容是否更新
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                String downloadMd5 = DigestUtils.md5Hex(body);
                if (md5.equals(downloadMd5)) {
                    log.info("File not modified: {}", remoteFile.getName());
                    result.setStatus("skip");
                } else {
                    JsonUtils.writeFile(file, sourceDict);
                    log.info("File updated: {}", remoteFile.getName());
                    result.setStatus("update");
                }
            }
        } else {
            // 文件不存在，直接写入
            JsonUtils.writeFile(file, sourceDict);
            log.info("File saved: {}", remoteFile.getName());
            result.setStatus("create");
        }

        result.setBytes(body.getBytes(StandardCharsets.UTF_8).length);
        return result;
    }

    /**
     * 递归更新嵌套的 JSON 结构
     */
    public static void updateNestedStructure(Map<String, Object> targetMap, Map<String, String> translatedDict, String parentKey) {
        for (Map.Entry<String, Object> entry : targetMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String currentKey = getCurrentKey(parentKey, key);

            if (value instanceof String) {
                // 如果是字符串值，直接查找对应的翻译
                String translatedValue = translatedDict.get(currentKey);
                if (translatedValue != null) {
                    targetMap.put(key, translatedValue);
                }
                // 如果没有翻译，保持原值不变
            } else if (value instanceof Map) {
                // 如果是嵌套的 Map，递归处理
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                updateNestedStructure(nestedMap, translatedDict, currentKey);
            } else if (value instanceof List) {
                // 如果是数组，处理每个元素
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                updateListStructure(list, translatedDict, currentKey);
            }
            // 其他类型（Number, Boolean等）保持不变
        }
    }

    /**
     * 更新数组结构
     */
    private static void updateListStructure(List<Object> list, Map<String, String> translatedDict, String parentKey) {
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            String currentKey = getCurrentKey(parentKey, i);

            if (element instanceof Map) {
                // 数组元素是对象，递归处理
                @SuppressWarnings("unchecked")
                Map<String, Object> elementMap = (Map<String, Object>) element;
                updateNestedStructure(elementMap, translatedDict, currentKey);
            } else if (element instanceof List) {
                // 嵌套数组，递归处理
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) element;
                updateListStructure(nestedList, translatedDict, currentKey);
            } else if (element instanceof String) {
                // 数组元素是字符串，查找对应的翻译
                String translatedValue = translatedDict.get(currentKey);
                if (translatedValue != null) {
                    list.set(i, translatedValue);
                }
            }
        }
    }

    /**
     * 上传译文
     * @param force 是否强制上传未翻译内容
     * @throws IOException
     * @throws ApiException
     */
    public void uploadTranslations(Boolean force) throws IOException, ApiException {
        fetchRemoteFiles();

        if (remoteFiles == null || remoteFiles.isEmpty()) {
            log.info("No remote files found");
            return;
        }

        List<FileScanResult> fileList = getSourceFiles();
        if (fileList.isEmpty()) {
            log.info("No source files found");
            return;
        }

        for (FileScanResult file : fileList) {
            String filePath = file.getTranslationFilePath();
            FilesDto remoteFile = remoteFilesMap.get(filePath);
            if (remoteFile != null) {
                uploadTranslation(remoteFile, force);
            }
        }
    }

    public String uploadTranslation(FilesDto remoteFile, Boolean force) throws IOException, ApiException {
        String relativePath = remoteFile.getName();
        String absolutePath = getAbsolutePath(relativePath);
        File file = new File(absolutePath);
        if (!file.exists() || !file.isFile()) {
            log.info("File not exist: {}", relativePath);
            return I18n.getString("label.failed.notExists");
        }

        // 读取远程译文
        List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
        if (translations == null || translations.isEmpty()) {
            log.info("Not translated: {}", relativePath);
            return I18n.getString("label.skipped.notTranslated");
        }

        // 读取本地汉化文件
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> sourceMap = JsonUtils.readFile(file, mapType);

        Map<String, String> flatSourceMap = new LinkedHashMap<>();

        flattenNestedStructure(sourceMap, flatSourceMap, null);

        int count = 0;// 处理词条数
        for (TranslationDto item : translations) {
            String key = item.getKey();
            StageEnum stage = StageEnum.of(item.getStage());
            if (stage == StageEnum.HIDDEN) {
                // 隐藏词条，不翻译
                continue;
            }

            if (flatSourceMap.containsKey(key)) {
                String value = flatSourceMap.get(key);
                if (value.equals(item.getOriginal())) {
                    // 译文和原文相同，属于未翻译内容。
                    if (stage != StageEnum.UNTRANSLATED && Boolean.TRUE.equals(force)) {
                        // 强制标记为未翻译
                        StringItem stringItem = new StringItem();
                        stringItem.setKey(key);
                        stringItem.setOriginal(item.getOriginal());
                        stringItem.setTranslation(value);
                        stringItem.setStage(StageEnum.UNTRANSLATED.getValue());
                        stringItem.setContext(item.getContext());

                        stringsApi.updateString(projectId, item.getId(), stringItem).execute();
                        log.debug("重置为未翻译, key:{} , stage:{}", key, stage.getDesc());
                        count++;
                    }
                } else {
                    if (!value.equals(item.getTranslation())) {
                        StringItem stringItem = new StringItem();
                        stringItem.setKey(key);
                        stringItem.setOriginal(item.getOriginal());
                        stringItem.setTranslation(value);
                        stringItem.setStage(StageEnum.TRANSLATED.getValue());
                        stringItem.setContext(item.getContext());

                        stringsApi.updateString(projectId, item.getId(), stringItem).execute();
                        log.debug("更新词条, key:{}, value:{} -> {}", key, item.getTranslation(), value);
                        count++;
                    }
                }
            } else {
                log.debug("{} 没有找到译文词条: {}", relativePath, key);
            }
        }
        log.info("上传译文完成: {}, 更新词条数: {}", relativePath, count);
        if (count > 0) {
            return I18n.getString("label.completed.updated") + " " + count + I18n.getString("label.strings");
        } else {
            return I18n.getString("label.completed.notModified");
        }
    }

    /**
     * 打平嵌套的JSON数据结构
     */
    public static void flattenNestedStructure(Map<String, Object> sourceMap, Map<String, String> toSave, String parentKey) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String currentKey = getCurrentKey(parentKey, key);

            if (value instanceof String) {
                toSave.put(currentKey, (String) value);
                // 如果没有翻译，保持原值不变
            } else if (value instanceof Map) {
                // 如果是嵌套的 Map，递归处理
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenNestedStructure(nestedMap, toSave, currentKey);
            } else if (value instanceof List) {
                // 如果是数组，处理每个元素
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                flattenListStructure(list, toSave, currentKey);
            }
            // 其他类型（Number, Boolean等）保持不变
        }
    }

    /**
     * 打平数组结构
     */
    private static void flattenListStructure(List<Object> list, Map<String, String> toSaveMap, String parentKey) {
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            String currentKey = getCurrentKey(parentKey, i);

            if (element instanceof Map) {
                // 数组元素是对象，递归处理
                @SuppressWarnings("unchecked")
                Map<String, Object> elementMap = (Map<String, Object>) element;
                flattenNestedStructure(elementMap, toSaveMap, currentKey);
            } else if (element instanceof List) {
                // 嵌套数组，递归处理
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) element;
                flattenListStructure(nestedList, toSaveMap, currentKey);
            } else if (element instanceof String) {
                // 数组元素是字符串，查找对应的翻译
                String translatedValue = toSaveMap.get(currentKey);
                if (translatedValue != null) {
                    list.set(i, translatedValue);
                }
            }
        }
    }

    private static String getCurrentKey(String parentKey, String key) {
        return parentKey == null || parentKey.isEmpty() ? key : parentKey + "->" + key;
    }

    private static String getCurrentKey(String parentKey, int index) {
        return parentKey == null || parentKey.isEmpty() ? index + "" : parentKey + "->" + index;
    }

    public File getAbsoluteFile(String relativePath) {
        return new File(workDir + SEPARATOR + relativePath);
    }

    public String getAbsolutePath(String relativePath) {
        return workDir + SEPARATOR + relativePath;
    }
}
