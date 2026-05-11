package com.marketinghub.oprmcoletormei.catalog.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oprm.collector")
public record CnaeCatalogCollectorProperties(
        @NotBlank String backendBaseUrl,
        @Min(1) int batchSize
) {
}
