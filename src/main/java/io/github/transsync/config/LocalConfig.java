package io.github.transsync.config;

public class LocalConfig {
    private String languagePath;
    public LocalConfig() {}
    public String getLanguagePath() { return languagePath; }
    public void setLanguagePath(String languagePath) { this.languagePath = languagePath; }
    @Override
    public String toString() {
        return "LocalConfig{" +
                "languagePath='" + languagePath + '\'' +
                '}';
    }
}
