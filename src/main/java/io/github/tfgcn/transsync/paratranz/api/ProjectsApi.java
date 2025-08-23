package io.github.tfgcn.transsync.paratranz.api;

import io.github.tfgcn.transsync.paratranz.model.PageResult;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * desc: 项目相关接口
 *
 * @author yanmaoyuan
 */
public interface ProjectsApi {

    /**
     * 项目列表
     * @return 项目列表
     */
    @GET("projects")
    Call<PageResult<ProjectsDto>> getProjects();

    /**
     * 项目信息
     * @param projectId 项目ID
     * @return 项目信息
     */
    @GET("projects/{projectId}")
    Call<ProjectsDto> getProject(@Path("projectId") int projectId);
}