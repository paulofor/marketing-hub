package com.marketinghub.product.service.valuechainposition;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Responsabilidade: expor tempo e custo auditável de uma permanência de processo ou subprocesso.
 */
public record ProductStageMeasurementResponse(
    String stageType,
    String trackingStatus,
    Long processDefinitionId,
    String processCode,
    String processName,
    Instant enteredAt,
    String entryEvidence,
    Instant exitedAt,
    String exitEvidence,
    boolean objectiveAchieved,
    Long elapsedDays,
    BigDecimal knownEstimatedCostUsd,
    String costCoverage,
    Integer costedExecutionCount,
    Integer uncostedExecutionCount) {}
