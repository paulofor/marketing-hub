package com.marketinghub.oprmcoletormei.marketimport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Define as propriedades operacionais da rotina legada de importação OPRM CNPJ/CNAE. */
@ConfigurationProperties(prefix = "oprm.market-import.schedule")
public record OprmMarketImportScheduleProperties(
        boolean enabled,
        String cron,
        String timezone,
        String snapshotDate,
        String sourceBaseUrl
) {}
