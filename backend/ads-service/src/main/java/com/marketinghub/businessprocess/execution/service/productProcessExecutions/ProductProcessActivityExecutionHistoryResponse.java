package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: apresentar as atividades e tarefas auditáveis de um produto em um processo. */
public record ProductProcessActivityExecutionHistoryResponse(
    Long productId,
    String productName,
    String productInternalName,
    Long selectedProcessDefinitionId,
    String processCode,
    String processName,
    Integer selectedProcessVersionNumber,
    String selectedProcessStatus,
    int activityCount,
    int activitiesWithTasksCount,
    int uniqueTaskCount,
    BigDecimal knownEstimatedCostUsd,
    String costCoverage,
    List<ProductProcessActivityExecutionGroupResponse> activities) {}
