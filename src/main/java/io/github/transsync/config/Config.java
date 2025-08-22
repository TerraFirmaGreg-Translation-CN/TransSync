package io.github.transsync.config;

public class Config {
    private ParatranzConfig paratranz;
    private GithubConfig github;
    private LocalConfig local;
    public Config() {}

    public ParatranzConfig getParatranz() { return paratranz; }
    public void setParatranz(ParatranzConfig paratranz) { this.paratranz = paratranz; }
    public GithubConfig getGithub() { return github; }
    public void setGithub(GithubConfig github) { this.github = github; }
    public LocalConfig getLocal() { return local; }
    public void setLocal(LocalConfig local) { this.local = local; }
    @Override
    public String toString() {
        return "Config{" +
                "paratranz=" + paratranz +
                ", github=" + github +
                ", local=" + local +
                '}';
    }
}