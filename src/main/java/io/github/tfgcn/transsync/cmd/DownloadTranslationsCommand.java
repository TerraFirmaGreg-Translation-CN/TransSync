package io.github.tfgcn.transsync.cmd;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import io.github.tfgcn.transsync.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * desc: 下载译文命令
 *
 * @author yanmaoyuan
 */
@Slf4j
@CommandLine.Command(name = "download-translation", mixinStandardHelpOptions = true, version = Constants.VERSION,
        description ="Download translations from paratranz.")
public class DownloadTranslationsCommand extends BaseCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        Config config = initConfig();
        if (config == null) {
            return 1;
        }

        ParatranzApiFactory factory = new ParatranzApiFactory(config);
        ProjectsApi projectsApi = factory.create(ProjectsApi.class);
        FilesApi filesApi = factory.create(FilesApi.class);

        // 检查ProjectId是否正确
        ProjectsDto projectsDto = projectsApi.getProject(config.getProjectId()).execute().body();
        if (projectsDto == null) {
            log.error("项目不存在");
            return 1;
        }
        log.info("Project ID: {}", projectsDto.getId());
        log.info("Project Name: {}", projectsDto.getName());

        SyncService app = new SyncService();
        app.setFilesApi(filesApi);
        app.setProjectId(config.getProjectId());
        app.setWorkspace(config.getWorkspace());
        app.setRules(config.getRules());

        // 执行上传
        app.downloadTranslations();

        log.info("Done");
        return 0;
    }
}
