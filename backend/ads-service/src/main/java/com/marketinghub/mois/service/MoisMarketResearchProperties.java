package com.marketinghub.mois.service;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "integrations.mois.market-research")
public class MoisMarketResearchProperties {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:8091";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(8);
    private int maxSources = 10;
}
