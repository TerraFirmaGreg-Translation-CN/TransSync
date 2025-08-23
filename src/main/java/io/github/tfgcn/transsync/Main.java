package io.github.tfgcn.transsync;

import com.google.common.base.Preconditions;
import io.github.tfgcn.transsync.app.SyncToParatranz;
import io.github.tfgcn.transsync.paratranz.error.ApiException;
import io.github.tfgcn.transsync.paratranz.ApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException, ApiException {

        ApiFactory apiFactory = new ApiFactory();

        Config config = Config.load();
        Integer projectId = config.getProjectId();
        Preconditions.checkArgument(projectId != null && projectId > 0,
                "项目ID不能为空，请前往 https://paratranz.cn 获取项目ID，并写入配置文件");

        FilesApi filesApi = apiFactory.create(FilesApi.class);

        SyncToParatranz app = new SyncToParatranz(filesApi, projectId);

        // 默认本项目与 Tools-Modern 处于同级目录，因此直接到上级目录查找即可
        app.setWorkspace("..");

        // 扫描所有需要汉化的文件
        app.pushToParatranz();

    }
}
