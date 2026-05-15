package com.marketinghub.oprmcoletormei.marketimport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oprm.market-import.schedule")
public record OprmMarketImportScheduleProperties(
        boolean enabled,
        String cron,
        String timezone,
        String snapshotDate,
        String sourceBaseUrl
) {}
