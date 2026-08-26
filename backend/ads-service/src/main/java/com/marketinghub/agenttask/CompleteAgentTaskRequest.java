package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Responsabilidade: validar o resultado funcional reportado por um executor de atividade BPM. */
public record CompleteAgentTaskRequest(
    @NotBlank String resultJson,
    @NotBlank String evidenceJson,
    List<@Valid AgentTaskModelUsageRequest> modelUsages,
    @Valid AgentTaskExecutionAuditRequest executionAudit) {

  /** Mantém compatibilidade com executores que ainda não reportam consumo de modelo. */
  public CompleteAgentTaskRequest(String resultJson, String evidenceJson) {
    this(resultJson, evidenceJson, null, null);
  }

  /** Mantém compatibilidade com executores que já reportavam o consumo do modelo. */
  public CompleteAgentTaskRequest(
      String resultJson, String evidenceJson, List<AgentTaskModelUsageRequest> modelUsages) {
    this(resultJson, evidenceJson, modelUsages, null);
  }
}
