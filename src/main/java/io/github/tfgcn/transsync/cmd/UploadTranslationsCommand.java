package io.github.tfgcn.transsync.cmd;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.api.StringsApi;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import io.github.tfgcn.transsync.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * desc: 上传译文命令
 *
 * @author yanmaoyuan
 */
@Slf4j
@CommandLine.Command(name = "up-trans", mixinStandardHelpOptions = true, version = Constants.VERSION,
        description ="Upload translated file to paratranz.")
public class UploadTranslationsCommand extends BaseCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"-f", "--force"}, description = "是否强制覆盖未翻译内容", defaultValue = "false", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    protected Boolean force;// 强制覆盖未翻译内容

    @Override
    public Integer call() throws Exception {
        Config config = initConfig();
        if (config == null) {
            return 1;
        }

        ParatranzApiFactory factory = new ParatranzApiFactory(config);

        ProjectsApi projectsApi = factory.create(ProjectsApi.class);
        FilesApi filesApi = factory.create(FilesApi.class);
        StringsApi  stringsApi = factory.create(StringsApi.class);

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
        app.setStringsApi(stringsApi);
        app.setProjectId(config.getProjectId());
        app.setWorkspace(config.getWorkspace());

        app.uploadTranslations(force);
        return 0;
    }
}
