package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: validar a causa e preservar o parecer que bloqueou uma atividade BPM. */
public record FailAgentTaskRequest(@NotBlank String error, String resultJson, String evidenceJson) {

  /** Mantém compatibilidade com callbacks técnicos que ainda reportam somente a causa. */
  public FailAgentTaskRequest(String error) {
    this(error, null, null);
  }
}
