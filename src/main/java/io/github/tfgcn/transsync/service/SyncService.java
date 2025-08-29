package io.github.tfgcn.transsync.service;

import com.google.gson.reflect.TypeToken;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.api.StringsApi;
import io.github.tfgcn.transsync.paratranz.error.ApiException;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.StageEnum;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.FileUploadRespDto;
import io.github.tfgcn.transsync.paratranz.model.files.TranslationDto;
import io.github.tfgcn.transsync.paratranz.model.strings.StringItem;
import io.github.tfgcn.transsync.service.model.DownloadTranslationResult;
import io.github.tfgcn.transsync.service.model.FileScanRequest;
import io.github.tfgcn.transsync.service.model.FileScanResult;
import io.github.tfgcn.transsync.service.model.FileScanRule;
import io.github.tfgcn.transsync.utils.JsonUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import retrofit2.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
            throw new RuntimeException("未配置文件扫描规则，请先配置扫描规则");
        }

        List<FileScanResult> fileList = new ArrayList<>(100);

        Set<String> distinct = new HashSet<>();
        for (FileScanRule rule : rules) {
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
            File file = new File(workDir + SEPARATOR + item.getSourceFilePath());
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
        File file = new File(workDir + SEPARATOR + scannedFile.getSourceFilePath());
        String remoteFolder = scannedFile.getTranslationFileFolder();

        // 生成上传 paratranz 的最终文件名。可用于比较远程文件是否已存在
        FilesDto remoteFile = remoteFilesMap.get(scannedFile.getTranslationFilePath());
        if (remoteFile == null) {
            uploadFile(remoteFolder, file);
            return "完成 - 新文件";
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                if (md5.equals(remoteFile.getHash())) {
                    return "跳过 - 未更新";
                }
            }

            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                    RequestBody.create(Constants.MULTIPART_FORM_DATA, file));

            Response<FileUploadRespDto> updateResp = filesApi.updateFile(projectId, remoteFile.getId(), filePart).execute();
            if (updateResp.isSuccessful()) {
                log.info("update success: {}", updateResp.body());
            }

            return "完成 - 已更新";
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

        // 执行上传操作
        for (FilesDto remoteFile : remoteFiles) {
            List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
            if (translations == null || translations.isEmpty()) {
                log.info("缺少翻译: {}", remoteFile.getName());
            } else {
                saveTranslations(remoteFile, translations);
            }
        }
    }

    public String downloadTranslation(FilesDto remoteFile) throws IOException, ApiException {
        List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
        if (translations == null || translations.isEmpty()) {
            return "跳过 - 无翻译";
        }

        DownloadTranslationResult result = saveTranslations(remoteFile, translations);
        if ("skip".equals(result.getStatus())) {
            return "跳过 - 未更新 " + formatFileSize(result.getBytes());
        } else {
            return "完成 - " + formatFileSize(result.getBytes());
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

        File file = new File(workDir + SEPARATOR + remoteFile.getName());
        FileUtils.createParentDirectories(file);

        // 文件存在
        if (file.exists()) {
            // 比较文件内容是否更新
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                String downloadMd5 = DigestUtils.md5Hex(body);
                if (md5.equals(downloadMd5)) {
                    log.info("文件未更新: {}", remoteFile.getName());
                    result.setStatus("skip");
                } else {
                    JsonUtils.writeFile(file, map);
                    log.info("文件已更新: {}", remoteFile.getName());
                    result.setStatus("update");
                }
            }
        } else {
            // 文件不存在，直接写入
            JsonUtils.writeFile(file, map);
            log.info("文件已保存: {}", remoteFile.getName());
            result.setStatus("create");
        }

        result.setBytes(body.getBytes(StandardCharsets.UTF_8).length);
        return result;
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
            log.info("没有远程文件");
            return;
        }

        for (FilesDto remoteFile : remoteFiles) {
            uploadTranslation(remoteFile, force);
        }
    }

    public String uploadTranslation(FilesDto remoteFile, Boolean force) throws IOException, ApiException {
        String relativePath = remoteFile.getName();
        String absolutePath = workDir + SEPARATOR + relativePath;
        File file = new File(absolutePath);
        if (!file.exists() || !file.isFile()) {
            log.info("文件不存在: {}", relativePath);
            return "失败 - 文件不存在";
        }

        // 读取远程译文
        List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
        if (translations == null || translations.isEmpty()) {
            log.info("没有远程译文: {}", relativePath);
            return "跳过 - 无需翻译";
        }

        // 读取本地汉化文件
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map = JsonUtils.readFile(file, mapType);

        int count = 0;// 处理词条数
        for (TranslationDto item : translations) {
            String key = item.getKey();
            StageEnum stage = StageEnum.of(item.getStage());
            if (stage == StageEnum.HIDDEN) {
                // 隐藏词条，不翻译
                continue;
            }

            if (map.containsKey(key)) {
                String value = map.get(key);
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
            return "完成 - 更新 " + count + " 词条";
        } else {
            return "完成 - 未更新";
        }
    }
}
