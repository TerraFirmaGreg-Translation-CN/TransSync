package io.github.transsync.config;

public class GithubConfig {
    private String repoOwner;
    private String repoName;
    private String branch;
    public GithubConfig() {}
    public String getRepoOwner() { return repoOwner; }
    public void setRepoOwner(String repoOwner) { this.repoOwner = repoOwner; }
    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    @Override
    public String toString() {
        return "GithubConfig{" +
                "repoOwner='" + repoOwner + '\'' +
                ", repoName='" + repoName + '\'' +
                ", branch='" + branch + '\'' +
                '}';
    }
}
