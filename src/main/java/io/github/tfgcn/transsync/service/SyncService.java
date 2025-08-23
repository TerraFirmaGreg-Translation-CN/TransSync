package io.github.tfgcn.transsync.service;

import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class SyncService {

    public SyncService() {
    }

    public boolean checkGithubConnected() {
        return false;
    }
    public boolean checkParatranzConnected() {
        ParatranzApiFactory factory;
        try {
            factory = new ParatranzApiFactory();
        } catch (IOException e) {
            return false;
        }

        ProjectsApi projectsApi = factory.create(ProjectsApi.class);

        return false;
    }
}