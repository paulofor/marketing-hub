package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import java.math.BigDecimal;
import java.util.List;

/**
 * Responsabilidade: apresentar a situação consolidada, as atividades e as tarefas de um produto em
 * um processo.
 */
public record ProductProcessActivityExecutionHistoryResponse(
    Long productId,
    String productName,
    String productInternalName,
    Long commercialPlanId,
    String commercialPlanName,
    Long selectedProcessDefinitionId,
    String processCode,
    String processName,
    Integer selectedProcessVersionNumber,
    String selectedProcessStatus,
    String currentExecutionReference,
    String operationalState,
    boolean objectiveAchieved,
    int selectedActivityCount,
    int completedActivityCount,
    int remainingActivityCount,
    int blockedActivityCount,
    String currentActivityId,
    String currentActivityName,
    String currentActivityState,
    String currentActivityStateReason,
    int activityCount,
    int activitiesWithTasksCount,
    int uniqueTaskCount,
    BigDecimal knownEstimatedCostUsd,
    String costCoverage,
    List<ProductProcessActivityExecutionGroupResponse> activities) {}
