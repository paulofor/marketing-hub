package com.marketinghub.marketresearch.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "market-research")
public class MarketResearchProperties {

    @NotNull
    private Duration httpTimeout = Duration.ofSeconds(12);

    @Min(1000)
    @Max(20000)
    private int maxContextLength = 8000;

    @Min(500)
    @Max(10000)
    private int perSourceMaxLength = 2000;

    public Duration getHttpTimeout() {
        return httpTimeout;
    }

    public void setHttpTimeout(Duration httpTimeout) {
        this.httpTimeout = httpTimeout;
    }

    public int getMaxContextLength() {
        return maxContextLength;
    }

    public void setMaxContextLength(int maxContextLength) {
        this.maxContextLength = maxContextLength;
    }

    public int getPerSourceMaxLength() {
        return perSourceMaxLength;
    }

    public void setPerSourceMaxLength(int perSourceMaxLength) {
        this.perSourceMaxLength = perSourceMaxLength;
    }
}
