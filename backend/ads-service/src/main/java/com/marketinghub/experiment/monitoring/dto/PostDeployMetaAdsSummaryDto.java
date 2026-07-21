package com.marketinghub.experiment.monitoring.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Resume métricas de mídia paga vinculadas ao experimento monitorado. */
public record PostDeployMetaAdsSummaryDto(
        LocalDate dateStart,
        LocalDate dateStop,
        Long reach,
        Long impressions,
        Long clicks,
        Long leads,
        BigDecimal spend,
        BigDecimal cpc,
        BigDecimal cpl,
        BigDecimal ctrPercent,
        Instant lastSyncedAt,
        String lastSyncError
) {}
