package com.marketinghub.experiment.service.cockpit;

import java.time.Instant;

/** Etapa do funil formatada para o cockpit comercial. */
public record ExperimentCockpitFunnelStageDto(
    String stage,
    String label,
    int order,
    long totalCount,
    Long uniqueCount,
    Instant lastEventAt,
    String source) {}
