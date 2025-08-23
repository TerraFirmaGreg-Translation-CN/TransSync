package io.github.tfgcn.transync.paratranz;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.paratranz.ApiFactory;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.model.PageResult;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * desc: 项目接口测试
 *
 * @author yanmaoyuan
 */
@Slf4j
public class ProjectsApiTest {

    @Test
    void testGetProjects() throws Exception {
        ApiFactory apiFactory = new ApiFactory();
        ProjectsApi projectsApi = apiFactory.create(ProjectsApi.class);
        PageResult<ProjectsDto> projects = projectsApi.getProjects().execute().body();
        Assertions.assertNotNull(projects);
    }

    @Test
    void testGetProject() throws Exception {
        Config config = Config.load();
        Integer projectId = config.getProjectId();
        Assertions.assertNotNull(projectId, "请将 paratranz 项目ID写入 config.json 配置文件中");
        Assertions.assertTrue(projectId > 0, "请将paratranz 项目ID写入 config.json 配置文件中");

        ApiFactory apiFactory = new ApiFactory();
        ProjectsApi projectsApi = apiFactory.create(ProjectsApi.class);
        ProjectsDto project = projectsApi.getProject(projectId).execute().body();
        Assertions.assertNotNull(project);
    }
}
