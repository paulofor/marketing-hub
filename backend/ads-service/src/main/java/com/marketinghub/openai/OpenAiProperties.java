package com.marketinghub.openai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration requestTimeout = Duration.ofSeconds(90);
    private Duration batchPollInterval = Duration.ofMillis(500);
    private Duration batchTimeout = Duration.ofMinutes(2);
    private String batchCompletionWindow = "24h";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getBatchPollInterval() {
        return batchPollInterval;
    }

    public void setBatchPollInterval(Duration batchPollInterval) {
        this.batchPollInterval = batchPollInterval;
    }

    public Duration getBatchTimeout() {
        return batchTimeout;
    }

    public void setBatchTimeout(Duration batchTimeout) {
        this.batchTimeout = batchTimeout;
    }

    public String getBatchCompletionWindow() {
        return batchCompletionWindow;
    }

    public void setBatchCompletionWindow(String batchCompletionWindow) {
        this.batchCompletionWindow = batchCompletionWindow;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(apiKey);
    }
}
