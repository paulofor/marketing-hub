package com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution;

import com.marketinghub.agenttask.AgentTaskResponse;
import java.util.List;

/** Responsabilidade: informar as tarefas abertas atomicamente para uma atividade do produto. */
public record ProductProcessActivityExecutionRequestResponse(
    Long processDefinitionId,
    Long productId,
    String activityId,
    String sourceReference,
    List<AgentTaskResponse> tasks) {}
