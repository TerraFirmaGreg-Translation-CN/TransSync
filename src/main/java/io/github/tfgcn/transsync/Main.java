package io.github.tfgcn.transsync;

import com.google.common.base.Preconditions;
import io.github.tfgcn.transsync.paratranz.ApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.UploadFileResp;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.collections4.CollectionUtils;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException {

        ApiFactory apiFactory = new ApiFactory();

        Config config = Config.load();
        Integer projectId = config.getProjectId();
        Preconditions.checkArgument(projectId != null && projectId > 0,
                "项目ID不能为空，请前往 https://paratranz.cn 获取项目ID，并写入配置文件");

        FilesApi filesApi = apiFactory.create(FilesApi.class);

        // 查询已有的文件列表
        Map<String, Integer> existsFiles = new HashMap<>();

        Response<List<FilesDto>> getFilesResp = filesApi.getFiles(projectId).execute();
        if (getFilesResp.isSuccessful()) {
            List<FilesDto> fileList = getFilesResp.body();
            if (CollectionUtils.isNotEmpty(fileList)) {
                for (FilesDto file : fileList) {
                    existsFiles.put(file.getName(), file.getId());
                }
            }
        } else {
            log.error("Failed to get files list: {}", getFilesResp.message());
            return;
        }

        // 扫描所有需要汉化的文件
        // 默认本项目与 Tools-Modern 处于同级目录，因此直接到上级目录查找即可
        String workspace = "..";
        String workDir = new File(workspace).getCanonicalPath();
        log.info("workspace:{}", workDir);
        int workDirLength = workDir.length();

        File toolsModernFolder = new File(workDir + "/Tools-Modern");
        if (!toolsModernFolder.exists()) {
            log.error("Tools-Modern folder not found");
            return;
        }

        File languageFolder = new File(workDir + "/Tools-Modern/LanguageMerger/LanguageFiles");

        // 扫描 mod 文件夹下的 en_us 目录，把文本上传到 paratranz
        File[] mods = languageFolder.listFiles();
        if (mods == null) {
            log.error("LanguageFiles folder not found");
            return;
        }

        for (File mod : mods) {
            if (mod.isDirectory()) {
                String path = mod.getAbsolutePath();
                String projectPath = path.substring(workDirLength + 1);

                File enFolder = new File(path + "/en_us");
                String zhFolder = projectPath + "/zh_cn";
                log.info("目录：{}", zhFolder);

                File[] enFiles = enFolder.listFiles();
                if (enFiles == null) {
                    log.error("目录为空：{}", enFolder);
                    continue;
                }

                for (File file : enFiles) {
                    if (file.isFile()) {
                        String fileName = file.getName();

                        String uploadFilePath = zhFolder + "/" + fileName;
                        Integer fileId = existsFiles.get(uploadFilePath);
                        if (fileId != null) {
                            log.info("更新文件：{}/{}", zhFolder, fileName);
                            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                                    RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

                            Response<UploadFileResp> updateResp = filesApi.updateFile(projectId, fileId, filePart).execute();
                            if (updateResp.isSuccessful()) {
                                log.info("update success: {}", updateResp.body());
                            } else {
                                log.error("update failed: {}", updateResp.errorBody());
                            }
                        } else {
                            log.info("上传新文件：{}/{}", zhFolder, fileName);
                            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                                    RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

                            RequestBody pathPart = RequestBody.create(zhFolder, Constants.MULTIPART_FORM_DATA);

                            Response<UploadFileResp> uploadResp = filesApi.uploadFile(projectId, pathPart, filePart).execute();
                            if (uploadResp.isSuccessful()) {
                                log.info("upload success: {}", uploadResp.body());
                            } else {
                                log.error("upload failed: {}", uploadResp.errorBody());
                            }
                        }
                    }
                }
            }
        }
    }
}
