package com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution;

import com.marketinghub.agenttask.AgentTaskResponse;
import java.util.List;

/** Responsabilidade: informar o resultado operacional ou as tarefas abertas para uma atividade. */
public record ProductProcessActivityExecutionRequestResponse(
    Long processDefinitionId,
    Long productId,
    String activityId,
    String sourceReference,
    List<AgentTaskResponse> tasks,
    String operationalState,
    boolean objectiveAchieved,
    String message) {

  /** Mantém o contrato das atividades executadas por agentes com estado inicial pendente. */
  public ProductProcessActivityExecutionRequestResponse(
      Long processDefinitionId,
      Long productId,
      String activityId,
      String sourceReference,
      List<AgentTaskResponse> tasks) {
    this(
        processDefinitionId,
        productId,
        activityId,
        sourceReference,
        tasks,
        "PENDING",
        false,
        "As tarefas responsáveis foram abertas e aguardam execução.");
  }
}
