package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Responsabilidade: validar a causa e preservar o parecer que bloqueou uma atividade BPM. */
public record FailAgentTaskRequest(
    @NotBlank String error,
    String resultJson,
    String evidenceJson,
    List<@Valid AgentTaskModelUsageRequest> modelUsages) {

  /** Mantém compatibilidade com callbacks técnicos que ainda reportam somente a causa. */
  public FailAgentTaskRequest(String error) {
    this(error, null, null, null);
  }

  /** Mantém compatibilidade com pareceres anteriores à contabilização por tarefa. */
  public FailAgentTaskRequest(String error, String resultJson, String evidenceJson) {
    this(error, resultJson, evidenceJson, null);
  }
}
