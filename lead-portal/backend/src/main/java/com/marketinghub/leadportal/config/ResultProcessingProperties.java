package com.marketinghub.leadportal.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lead-portal.results")
public class ResultProcessingProperties {

    /**
     * Artificial delay before a lead is marked as completed.
     */
    private Duration delay = Duration.ofSeconds(5);

    public Duration getDelay() {
        return delay;
    }

    public void setDelay(Duration delay) {
        this.delay = delay;
    }
}
