package com.marketinghub.agenttask;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor uma ocorrência de atividade e suas tentativas sem misturar os níveis. */
public record ProcessInstanceActivityResponse(
    Long activityInstanceId,
    Long activityDefinitionId,
    String activityId,
    String activityName,
    String objective,
    Integer occurrenceNumber,
    String status,
    String operationalState,
    String stateReason,
    Instant enteredAt,
    Instant exitedAt,
    boolean objectiveAchieved,
    BigDecimal knownCostUsd,
    String costCoverage,
    String evidenceQuality,
    List<ProcessInstanceTaskResponse> tasks) {}
