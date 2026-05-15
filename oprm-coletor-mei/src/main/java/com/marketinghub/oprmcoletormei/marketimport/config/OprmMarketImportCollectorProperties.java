package com.marketinghub.oprmcoletormei.marketimport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oprm.market-import.collector")
public record OprmMarketImportCollectorProperties(
        String backendBaseUrl,
        String tempDir
) {}
