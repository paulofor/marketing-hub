package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import java.time.Instant;

/** Responsabilidade: identificar a fonte histórica de publicação e suas limitações auditáveis. */
public record LearningCycleHistoricalPublication(
    String source,
    Long experimentId,
    String reference,
    Instant recordedAt,
    boolean preflightRecorded,
    String summary) {}
