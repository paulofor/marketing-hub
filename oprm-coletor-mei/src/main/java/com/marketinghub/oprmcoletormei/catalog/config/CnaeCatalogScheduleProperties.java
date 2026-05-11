package com.marketinghub.oprmcoletormei.catalog.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oprm.collector.schedule")
public record CnaeCatalogScheduleProperties(
        boolean enabled,
        @NotBlank String cron,
        @NotBlank String timezone,
        @NotBlank String source,
        String payloadFile
) {
}
