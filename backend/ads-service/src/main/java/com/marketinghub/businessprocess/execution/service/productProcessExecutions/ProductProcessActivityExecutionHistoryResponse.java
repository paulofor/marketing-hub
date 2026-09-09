package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.getSalesFlow.SalesFlowResponse;
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
    List<ProductProcessActivityExecutionGroupResponse> activities,
    SalesFlowResponse salesFlow) {
  /** Mantém compatibilidade com processos sem contrato de fluxo comercial. */
  public ProductProcessActivityExecutionHistoryResponse(
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
      List<ProductProcessActivityExecutionGroupResponse> activities) {
    this(
        productId,
        productName,
        productInternalName,
        commercialPlanId,
        commercialPlanName,
        selectedProcessDefinitionId,
        processCode,
        processName,
        selectedProcessVersionNumber,
        selectedProcessStatus,
        currentExecutionReference,
        operationalState,
        objectiveAchieved,
        selectedActivityCount,
        completedActivityCount,
        remainingActivityCount,
        blockedActivityCount,
        currentActivityId,
        currentActivityName,
        currentActivityState,
        currentActivityStateReason,
        activityCount,
        activitiesWithTasksCount,
        uniqueTaskCount,
        knownEstimatedCostUsd,
        costCoverage,
        activities,
        null);
  }
}
