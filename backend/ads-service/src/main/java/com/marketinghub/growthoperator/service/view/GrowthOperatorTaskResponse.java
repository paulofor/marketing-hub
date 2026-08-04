package com.marketinghub.growthoperator.service.view;

import com.marketinghub.growthoperator.GrowthOperatorTaskStatus;
import java.time.Instant;

/** Responsabilidade: expor uma pendencia do Operador e sua evidencia de resolucao. */
public record GrowthOperatorTaskResponse(
    Long id,
    Long planId,
    Long sourceExecutionId,
    String actionText,
    GrowthOperatorTaskStatus status,
    String resolutionEvidence,
    Instant resolvedAt,
    Instant createdAt) {}
