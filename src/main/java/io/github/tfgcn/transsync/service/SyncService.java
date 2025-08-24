package io.github.tfgcn.transsync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.api.StringsApi;
import io.github.tfgcn.transsync.paratranz.error.ApiException;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.StageEnum;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.FileUploadRespDto;
import io.github.tfgcn.transsync.paratranz.model.files.TranslationDto;
import io.github.tfgcn.transsync.paratranz.model.strings.StringItem;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * desc: 上传原文服务
 *
 * @author yanmaoyuan
 */
@Slf4j
public class SyncService {

    private final ObjectMapper objectMapper;

    @Setter
    private StringsApi stringsApi;
    @Setter
    private FilesApi filesApi;
    @Setter
    private Integer projectId;
    @Getter
    private String workDir;

    private List<FilesDto> remoteFiles;
    private Map<String, FilesDto> remoteFilesMap;

    public SyncService() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setDefaultPrettyPrinter(
                new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("    ", "\r\n"))
        );

        this.remoteFiles = Collections.emptyList();
        this.remoteFilesMap = Collections.emptyMap();
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

        // check language folder exists
        File languageFolder = new File(workDir + SEPARATOR + FOLDER_TOOLS_MODERN_LANGUAGE_FILES);
        if (!languageFolder.exists()) {
            log.warn("folder not found: {}", languageFolder);
            throw new IOException(Constants.MSG_FOLDER_NOT_FOUND + ":" + FOLDER_TOOLS_MODERN_LANGUAGE_FILES);
        }
    }

    /**
     * 获取远程文件列表
     *
     */
    public List<FilesDto> fetchRemoteFiles() throws IOException, ApiException {
        log.info("Fetching remote files...");
        // 查询已有的文件列表
        List<FilesDto> fileList = filesApi.getFiles(projectId).execute().body();
        if (CollectionUtils.isNotEmpty(fileList)) {
            remoteFiles = new ArrayList<>(fileList.size());
            remoteFilesMap = new HashMap<>();
            for (FilesDto file : fileList) {
                remoteFiles.add(file);
                remoteFilesMap.put(file.getName(), file);
                log.debug("{}", file.getName());
            }
            log.info("Found remote files: {}", fileList.size());
        } else {
            log.info("No remote files found");
            remoteFilesMap = Collections.emptyMap();
            remoteFiles = Collections.emptyList();
        }
        return remoteFiles;
    }

    /**
     * 获取待上传的文件列表
     */
    public List<File> getOriginalFiles() {
        // 扫描语言文件夹下的 en_us 目录，把文本上传到 paratranz
        List<File> fileList = new ArrayList<>(100);
        scanEnglishFiles(fileList, new File(workDir + SEPARATOR + FOLDER_TOOLS_MODERN_LANGUAGE_FILES));
        return fileList;
    }

    public List<String> getRemoteFilePaths(List<File> fileList) {
        if (CollectionUtils.isEmpty(fileList)) {
            return Collections.emptyList();
        }

        List<String> fileNames = new ArrayList<>(fileList.size());
        for (File file : fileList) {
            // 计算远程目录的路径
            String remoteFolder = getRemoteFolder(file);
            // 生成上传 paratranz 的最终文件名。可用于比较远程文件是否已存在
            String zhFilePath = remoteFolder + SEPARATOR + file.getName();
            fileNames.add(zhFilePath);
        }
        return fileNames;
    }

    /**
     * 上传原文
     */
    public void uploadOriginals() throws IOException, ApiException {
        // 扫描远程服务器上已有的文件
        fetchRemoteFiles();

        log.info("Scanning language files in: {}", FOLDER_TOOLS_MODERN_LANGUAGE_FILES);
        // 扫描语言文件夹下的 en_us 目录，把文本上传到 paratranz
        List<File> fileList = new ArrayList<>(100);
        scanEnglishFiles(fileList, new File(workDir + SEPARATOR + FOLDER_TOOLS_MODERN_LANGUAGE_FILES));

        log.info("Found files: {}", fileList.size());
        for (File file : fileList) {
            // 计算远程目录的路径
            String remoteFolder = getRemoteFolder(file);

            // 生成上传 paratranz 的最终文件名。可用于比较远程文件是否已存在
            String zhFilePath = remoteFolder + SEPARATOR + file.getName();

            FilesDto remoteFile = remoteFilesMap.get(zhFilePath);
            if (remoteFile == null) {
                uploadFile(remoteFolder, file);
            } else {
                updateFile(remoteFile, remoteFolder, file);
            }
        }
    }

    private String getRemoteFolder(File file) {
        // 获取英文文件的文件夹绝对路径
        // C:\Users\yanmaoyuan\TerraFirmaGreg\Tools-Modern\LanguageMerger\LanguageFiles\mods\tfg\en_us\Quests\ae2.json
        // C:\Users\yanmaoyuan\TerraFirmaGreg\Tools-Modern\LanguageMerger\LanguageFiles\mods\tfg\en_us\Quests
        String absoluteFolderPath = file.getParentFile().getAbsolutePath();

        // 将其替换为相对路径，以便和 paratranz 的文件路径一致
        // Tools-Modern/LanguageMerger/LanguageFiles/mods/tfg/en_us/Quests
        String relativeFolderPath = absoluteFolderPath.replace("\\", SEPARATOR).substring(workDir.length() + 1);

        // 替换为中文文件路径，与 paratranz 的文件路径一致
        // Tools-Modern/LanguageMerger/LanguageFiles/mods/tfg/zh_cn/Quests
        return relativeFolderPath.replaceFirst(EN_US, ZH_CN);
    }

    /**
     * 递归扫描指定目录下的英语文件
     *
     * @param files 用于存储扫描到的文件
     * @param folder 当前扫描目录
     */
    public void scanEnglishFiles(List<File> files, File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("目录不存在或者不是目录：{}", folder);
            return;
        }

        File[] subFiles = folder.listFiles();
        if (subFiles == null) {
            log.warn("目录不存在或者不是目录：{}", folder);
            return;
        }

        for (File subFile : subFiles) {
            if (subFile.isFile() && isEnglishLanguageFile(subFile)) {
                files.add(subFile);
            } else if (subFile.isDirectory()) {
                scanEnglishFiles(files, subFile);
            }
        }
    }

    /**
     * 判断是否是英文语言文件
     * <pre>Tools-Modern/LanguageMerger/LanguageFiles/../en_us/../*.json</pre>
     * @param file
     * @return
     */
    private boolean isEnglishLanguageFile(File file) {
        String path = file.getAbsolutePath().replace("\\", SEPARATOR);
        int workDirLength = workDir.length();
        if (path.length() < workDirLength + 1) {
            return false;
        }
        String relativePath = path.substring(workDirLength + 1);
        return relativePath.indexOf(EN_US) > 0 && relativePath.endsWith(".json");
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
     * @param file
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public String uploadOriginalFile(File file) throws IOException, ApiException {
        // 计算远程目录的路径
        String remoteFolder = getRemoteFolder(file);

        // 生成上传 paratranz 的最终文件名。可用于比较远程文件是否已存在
        String zhFilePath = remoteFolder + SEPARATOR + file.getName();

        FilesDto remoteFile = remoteFilesMap.get(zhFilePath);
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
        List<FilesDto> remoteFiles = fetchRemoteFiles();

        // 执行上传操作
        for (FilesDto remoteFile : remoteFiles) {
            List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
            if (CollectionUtils.isEmpty(translations)) {
                log.info("缺少翻译: {}", remoteFile.getName());
            } else {
                saveTranslations(remoteFile, translations);
            }
        }
    }

    public String downloadTranslation(FilesDto remoteFile) throws IOException, ApiException {
        List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
        if (CollectionUtils.isEmpty( translations)) {
            return "跳过 - 无翻译";
        }

        saveTranslations(remoteFile, translations);
        return "完成";
    }

    /**
     * 保存译文到文件
     *
     * @param remoteFile 源文件
     * @param translations 翻译结果
     * @throws IOException 保存失败时抛出
     */
    public void saveTranslations(FilesDto remoteFile, List<TranslationDto> translations) throws IOException {
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

        String relativePath = remoteFile.getName();
        String absolutePath = workDir + SEPARATOR + relativePath;
        File file = new File(absolutePath);

        // 检测父目录是否存在，不存在则创建
        File parentDir = file.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            log.warn("创建目录失败: {}", parentDir.getAbsolutePath());
            throw new IOException("创建目录失败: " + parentDir.getAbsolutePath());
        }

        if (file.exists()) {
            // 文件存在
            String body = objectMapper.writeValueAsString(map);
            // 比较文件内容是否更新
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                String downloadMd5 = DigestUtils.md5Hex(body);
                if (md5.equals(downloadMd5)) {
                    log.info("文件未更新: {}", relativePath);
                } else {
                    objectMapper.writeValue(file, map);
                    log.info("文件已更新: {}", relativePath);
                }
            }
        } else {
            // 文件不存在，直接写入
            objectMapper.writeValue(file, map);
            log.info("文件已保存: {}", relativePath);
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

        if (CollectionUtils.isEmpty(remoteFiles)) {
            log.info("没有远程文件");
            return;
        }

        for (FilesDto remoteFile : remoteFiles) {
            String relativePath = remoteFile.getName();
            String absolutePath = workDir + SEPARATOR + relativePath;
            File file = new File(absolutePath);
            if (!file.exists() || !file.isFile()) {
                log.info("文件不存在: {}", relativePath);
                continue;
            }

            // 读取远程译文
            List<TranslationDto> translations = filesApi.getTranslate(projectId, remoteFile.getId()).execute().body();
            if (CollectionUtils.isEmpty(translations)) {
                log.info("没有远程译文: {}", relativePath);
                continue;
            }

            // 读取本地汉化文件
            Map<String, String> map = objectMapper.readValue(file, new TypeReference<>() {
            });

            for (TranslationDto item : translations) {
                String key = item.getKey();
                StageEnum stage = StageEnum.of(item.getStage());
                if (stage == StageEnum.HIDDEN) {
                    // 隐藏词条，不翻译
                    //log.debug("跳过隐藏词条: {}", key);
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
                        }
                    }
                } else {
                    log.debug("{} 没有找到译文词条: {}", relativePath, key);
                }
            }
            log.info("上传译文完成: {}", relativePath);
        }
    }
}
