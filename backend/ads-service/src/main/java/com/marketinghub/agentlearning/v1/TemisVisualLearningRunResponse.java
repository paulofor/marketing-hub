package com.marketinghub.agentlearning.v1;

import java.time.Instant;

/** Responsabilidade: expor a decisão auditável de uma consolidação visual de Têmis. */
public record TemisVisualLearningRunResponse(
    Long id,
    String contextKey,
    TemisVisualLearningRunStatus status,
    String baselineVersion,
    String candidateVersion,
    Long memoryId,
    Long learningExperimentId,
    String error,
    Instant startedAt,
    Instant finishedAt,
    Instant createdAt) {}
