package com.marketinghub.mois.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mois.robot.hotmart")
public class MoisHotmartRobotProperties {

    private boolean enabled = false;
    private String cron = "0 0 * * * *";
    private String workspaceId = "workspace-001";
    private String niche = "marketing-digital";
    private String marketTheme = "ofertas-com-temperatura-alta";
    private List<String> sources = List.of("HOTMART");
    private String timeWindow = "LAST_7_DAYS";
    private Integer limitPerSource = 25;
    private String locale = "pt-BR";
    private String country = "BR";
    private Integer minSuccessScore = 80;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getMarketTheme() {
        return marketTheme;
    }

    public void setMarketTheme(String marketTheme) {
        this.marketTheme = marketTheme;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public Integer getLimitPerSource() {
        return limitPerSource;
    }

    public void setLimitPerSource(Integer limitPerSource) {
        this.limitPerSource = limitPerSource;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getMinSuccessScore() {
        return minSuccessScore;
    }

    public void setMinSuccessScore(Integer minSuccessScore) {
        this.minSuccessScore = minSuccessScore;
    }
}
