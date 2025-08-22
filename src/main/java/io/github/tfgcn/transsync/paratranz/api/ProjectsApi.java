package io.github.tfgcn.transsync.paratranz.api;

import io.github.tfgcn.transsync.paratranz.model.PageResult;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public interface ProjectsApi {

    @GET("projects")
    Call<PageResult<ProjectsDto>> getProjects();

    @GET("projects/{projectId}")
    Call<ProjectsDto> getProject(@Path("projectId") int projectId);
}