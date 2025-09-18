package com.marketinghub.journey.execution.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Google Analytics 4 Measurement Protocol configuration.
 */
@Component
@ConfigurationProperties(prefix = "integrations.ga4")
@Getter
@Setter
@ToString
public class Ga4Properties {
    private boolean enabled = false;
    private String endpoint = "https://www.google-analytics.com/mp/collect";
    private String measurementId;
    private String apiSecret;
}
