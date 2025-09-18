package com.marketinghub.journey.execution.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for Meta marketing integrations (Marketing API and Pixel).
 */
@Component
@ConfigurationProperties(prefix = "integrations.meta")
@Getter
@Setter
@ToString
public class MetaMarketingProperties {
    private boolean enabled = false;
    private String baseUrl = "https://graph.facebook.com/v18.0";
    private String accessToken;
    private String adAccountId;
    private String defaultAdStatus = "PAUSED";
    private boolean pixelEnabled = false;
    private String pixelId;
}
