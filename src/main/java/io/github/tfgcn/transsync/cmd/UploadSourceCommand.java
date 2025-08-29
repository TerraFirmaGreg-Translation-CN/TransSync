package io.github.tfgcn.transsync.cmd;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectStatsDto;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import io.github.tfgcn.transsync.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * desc: 上传原文
 *
 * @author yanmaoyuan
 */
@Slf4j
@CommandLine.Command(name = "upload-source", mixinStandardHelpOptions = true, version = Constants.VERSION,
        description ="Upload source file to paratranz.")
public class UploadSourceCommand extends BaseCommand implements Callable<Integer> {

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
        app.uploadSources();

        log.info("Done.");

        // 查询统计结果
        projectsDto = projectsApi.getProject(config.getProjectId()).execute().body();
        assert projectsDto != null;
        ProjectStatsDto stats = projectsDto.getStats();

        if (stats != null) {
            // 提取统计数据
            int total = stats.getTotal();
            int translated = stats.getTranslated();
            int checked = stats.getChecked();
            int reviewed = stats.getReviewed();
            int words = stats.getWords();
            int hidden = stats.getHidden();

            double tp = stats.getTp();
            double cp = stats.getCp();
            double rp = stats.getRp();

            // 添加统计行（进度条支持拉伸）
            log.info("总字数: {}", formatNumber(words));
            log.info("总条数: {} (+{})", formatNumber(total), hidden);
            log.info("已翻译条数: {} ({})", formatNumber(translated), formatPercent(tp));
            log.info("已检查条数: {} ({})", formatNumber(checked), formatPercent(cp));
            log.info("已审核条数: {} ({})", formatNumber(reviewed), formatPercent(rp));
        } else {
            log.info(EMPTY_STAT_TIP);
        }
        return 0;
    }

}
