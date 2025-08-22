package io.github.transsync.config;

public class ParatranzConfig {
    private String token;
    private int projectId;
    public ParatranzConfig() {}
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    @Override
    public String toString() {
        return "ParatranzConfig{" +
                "token='" + token + '\'' +
                ", projectId=" + projectId +
                '}';
    }
}
