package io.github.tfgcn.transsync.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.error.ApiException;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.FileUploadRespDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import retrofit2.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * desc: 封装 paratranz 服务
 *
 * @author yanmaoyuan
 */
@Slf4j
public class UploadOriginalService {

    @Setter
    private FilesApi filesApi;
    @Setter
    private Integer projectId;
    @Getter
    private String workDir;

    private List<FilesDto> remoteFiles;
    private Map<String, FilesDto> remoteFilesMap;

    public UploadOriginalService() {
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
        File languageFolder = new File(workDir + FOLDER_TOOLS_MODERN_LANGUAGE_FILES);
        if (!languageFolder.exists()) {
            log.warn("folder not found: {}", languageFolder);
            throw new IOException(Constants.MSG_FOLDER_NOT_FOUND + ":" + languageFolder);
        }

    }

    /**
     * 获取远程文件列表
     *
     */
    public void fetchRemoteFiles() throws IOException, ApiException {
        // 查询已有的文件列表

        Response<List<FilesDto>> getFilesResp = filesApi.getFiles(projectId).execute();
        if (getFilesResp.isSuccessful()) {
            List<FilesDto> fileList = getFilesResp.body();
            if (CollectionUtils.isNotEmpty(fileList)) {
                remoteFiles = Lists.newArrayListWithCapacity(fileList.size());
                remoteFilesMap = Maps.newHashMap();
                for (FilesDto file : fileList) {
                    remoteFiles.add(file);
                    remoteFilesMap.put(file.getName(), file);
                    log.info("Remote file: {}", file.getName());
                }
                log.info("Found {} remote files", fileList.size());
            } else {
                log.info("No files found");
                remoteFilesMap = Collections.emptyMap();
                remoteFiles = Collections.emptyList();
            }
        } else {
            log.error("Failed to get files list: {}", getFilesResp.message());
        }
    }

    /**
     * 获取待上传的文件列表
     */
    public List<File> getOriginalFiles() {
        // 扫描语言文件夹下的 en_us 目录，把文本上传到 paratranz
        List<File> fileList = Lists.newArrayListWithCapacity(100);
        scanEnglishFiles(fileList, new File(workDir + FOLDER_TOOLS_MODERN_LANGUAGE_FILES));
        return fileList;
    }

    public List<String> getRemoteFilePaths(List<File> fileList) {
        if (CollectionUtils.isEmpty(fileList)) {
            return Collections.emptyList();
        }

        List<String> fileNames = Lists.newArrayListWithCapacity(fileList.size());
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
     * 扫描本地文件
     *
     */
    public void pushToParatranz() throws IOException, ApiException {
        // 扫描远程服务器上已有的文件
        fetchRemoteFiles();

        // 扫描语言文件夹下的 en_us 目录，把文本上传到 paratranz
        List<File> fileList = Lists.newArrayListWithCapacity(100);
        scanEnglishFiles(fileList, new File(workDir + FOLDER_TOOLS_MODERN_LANGUAGE_FILES));

        log.info("Found {} files from {}", fileList.size(), FOLDER_TOOLS_MODERN_LANGUAGE_FILES);
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
                log.info("File not modified: {}/{}", remoteFolder, file.getName());
                return;
            }
        }

        log.info("更新文件：{}/{}", remoteFolder, file.getName());
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

        Response<FileUploadRespDto> updateResp = filesApi.updateFile(projectId, remoteFile.getId(), filePart).execute();
        if (updateResp.isSuccessful()) {
            log.info("update success: {}", updateResp.body());
        }
    }

    public void uploadFile(String remoteFolder, File file) throws IOException, ApiException {

        log.info("上传新文件：{}/{}", remoteFolder, file.getName());
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

        RequestBody pathPart = RequestBody.create(remoteFolder, Constants.MULTIPART_FORM_DATA);

        Response<FileUploadRespDto> uploadResp = filesApi.uploadFile(projectId, pathPart, filePart).execute();
        if (uploadResp.isSuccessful()) {
            log.info("upload success: {}", uploadResp.body());
        }
    }

    public String uploadFile(File file) throws IOException, ApiException {
        // 计算远程目录的路径
        String remoteFolder = getRemoteFolder(file);

        // 生成上传 paratranz 的最终文件名。可用于比较远程文件是否已存在
        String zhFilePath = remoteFolder + SEPARATOR + file.getName();

        FilesDto remoteFile = remoteFilesMap.get(zhFilePath);
        if (remoteFile == null) {
            uploadFile(remoteFolder, file);
            return "完成 - 上传新文件";
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                if (md5.equals(remoteFile.getHash())) {
                    return "跳过 - 文件无更改";
                }
            }

            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                    RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

            Response<FileUploadRespDto> updateResp = filesApi.updateFile(projectId, remoteFile.getId(), filePart).execute();
            if (updateResp.isSuccessful()) {
                log.info("update success: {}", updateResp.body());
            }

            return "完成 - 更新文件";
        }
    }
}
