package io.github.tfgcn.transsync.cmd;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.interceptor.LoggingInterceptor;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import io.github.tfgcn.transsync.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.github.tfgcn.transsync.Constants.*;
/**
 * desc: 上传原文
 *
 * @author yanmaoyuan
 */
@Slf4j
@CommandLine.Command(name = "upload-origin", mixinStandardHelpOptions = true, version = Constants.VERSION,
        description ="Upload original file to paratranz.")
public class UploadOriginCommand extends BaseCommand implements Callable<Integer> {

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

        // 执行上传
        app.uploadOriginals();

        log.info("Done.");

        // 查询统计结果
        projectsDto = projectsApi.getProject(config.getProjectId()).execute().body();
        assert projectsDto != null;
        Map<String, Object> stats = projectsDto.getStats();


        if (stats != null && !stats.isEmpty()) {
            // 提取统计数据（默认值为0，避免NPE）
            int total = (int) stats.getOrDefault("total", 0);
            int translated = (int) stats.getOrDefault("translated", 0);
            int checked = (int) stats.getOrDefault("checked", 0);
            int reviewed = (int) stats.getOrDefault("reviewed", 0);
            int words = (int) stats.getOrDefault("words", 0);
            int hidden = (int) stats.getOrDefault("hidden", 0);

            double tp = (double) stats.getOrDefault("tp", 0.0);
            double cp = (double) stats.getOrDefault("cp", 0.0);
            double rp = (double) stats.getOrDefault("rp", 0.0);

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
