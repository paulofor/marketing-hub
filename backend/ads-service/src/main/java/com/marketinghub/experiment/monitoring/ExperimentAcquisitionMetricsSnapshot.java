package com.marketinghub.experiment.monitoring;

import com.marketinghub.experiment.ExperimentPlatform;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor ao restante do backend a fotografia canônica do canal do experimento. */
public record ExperimentAcquisitionMetricsSnapshot(
    ExperimentPlatform platform,
    BigDecimal spendBrl,
    Instant lastSyncedAt,
    Instant finalSyncedAt,
    String lastError,
    Long impressions,
    Long clicks,
    boolean campaignLinked) {}
