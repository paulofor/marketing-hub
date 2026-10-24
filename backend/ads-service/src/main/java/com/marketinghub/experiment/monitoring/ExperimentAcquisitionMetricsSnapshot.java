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
    boolean campaignLinked,
    String stopReason,
    Instant stopRequestedAt,
    Instant stopCompletedAt,
    String stopExplanation) {

  /** Preserva consumidores históricos que não receberam diagnóstico de parada. */
  public ExperimentAcquisitionMetricsSnapshot(
      ExperimentPlatform platform,
      BigDecimal spendBrl,
      Instant lastSyncedAt,
      Instant finalSyncedAt,
      String lastError,
      Long impressions,
      Long clicks,
      boolean campaignLinked) {
    this(
        platform,
        spendBrl,
        lastSyncedAt,
        finalSyncedAt,
        lastError,
        impressions,
        clicks,
        campaignLinked,
        null,
        null,
        null,
        null);
  }
}
