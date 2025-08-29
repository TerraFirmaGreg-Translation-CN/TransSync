package io.github.tfgcn.transsync.cmd;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.I18n;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectStatsDto;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * desc: Show project info
 *
 * @author yanmaoyuan
 */
@CommandLine.Command(name = "info", mixinStandardHelpOptions = true, version = Constants.VERSION, description ="Show project info.")
public class ProjectInfoCommand extends BaseCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        Config config = initConfig();
        if (config == null) {
            return 1;
        }

        ParatranzApiFactory factory = new ParatranzApiFactory(config);

        ProjectsApi projectsApi = factory.create(ProjectsApi.class);

        // 检查ProjectId是否正确
        ProjectsDto projectsDto = projectsApi.getProject(config.getProjectId()).execute().body();
        if (projectsDto == null) {
            System.err.println("Project Not found");
            return 1;
        }
        System.out.println("#" + projectsDto.getId() + " " + projectsDto.getName());
        System.out.println(projectsDto.getDesc());
        System.out.println("===================");

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

            System.out.println(I18n.getString("label.totalWords") + ": " + formatNumber(words));
            System.out.println(I18n.getString("label.totalStrings") + ": " + formatNumber(total) + " (+" + formatNumber(hidden) + ")");
            System.out.println(I18n.getString("label.translatedStrings") + ": " + formatNumber(translated) + " (" + formatPercent(tp) + ")");
            System.out.println(I18n.getString("label.checkedStrings") + ": " + formatNumber(checked) + " (" + formatPercent(cp) + ")");
            System.out.println(I18n.getString("label.reviewedStrings") + ": " + formatNumber(reviewed) + " (" + formatPercent(rp) + ")");
        } else {
            System.out.println(I18n.getString("label.emptyStats"));
        }
        return 0;
    }
}
