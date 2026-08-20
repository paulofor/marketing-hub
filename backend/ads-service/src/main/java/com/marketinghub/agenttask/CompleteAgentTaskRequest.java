package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Responsabilidade: validar o resultado funcional reportado por um executor de atividade BPM. */
public record CompleteAgentTaskRequest(
    @NotBlank String resultJson,
    @NotBlank String evidenceJson,
    List<@Valid AgentTaskModelUsageRequest> modelUsages) {

  /** Mantém compatibilidade com executores que ainda não reportam consumo de modelo. */
  public CompleteAgentTaskRequest(String resultJson, String evidenceJson) {
    this(resultJson, evidenceJson, null);
  }
}
