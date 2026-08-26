package com.marketinghub.businessprocess.execution.service.recentExecutions;

import java.util.List;

/** Responsabilidade: identificar a atividade consultada e suas dez execuções mais recentes. */
public record BusinessProcessActivityExecutionHistoryResponse(
    Long selectedProcessDefinitionId,
    String processCode,
    String processName,
    Integer selectedProcessVersionNumber,
    String selectedProcessStatus,
    String activityId,
    String activityName,
    String activityOwnerName,
    List<BusinessProcessActivityExecutionResponse> executions) {}
