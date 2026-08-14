package com.marketinghub.agentlearning.v1;

import java.math.BigDecimal;
import java.time.Instant;

/** Resumo auditável da decisão de aprendizado. */
public record LearningExperimentResponse(
    Long id,
    String agentKey,
    String scopeType,
    String scopeId,
    String candidateVersion,
    String baselineVersion,
    String status,
    Long memoryId,
    String baselineResultJson,
    String candidateResultJson,
    String decisionEvidence,
    BigDecimal minimumGain,
    BigDecimal maximumCostIncreaseRatio,
    boolean regressionPassed,
    boolean localValidationPassed,
    Instant createdAt,
    Instant evaluatedAt,
    Instant promotedAt) {}
