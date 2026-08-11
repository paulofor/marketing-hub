package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Responsabilidade: validar a abertura de um gate por um agente na mesa de outro agente. */
public record CreateAgentGateByAgentRequest(
    @NotBlank @Size(max = 100) String requestedByAgentKey,
    @NotBlank @Size(max = 100) String assignedAgentKey,
    @NotBlank @Size(max = 100) String gateCode,
    @NotBlank @Size(max = 160) String title,
    @NotBlank String description,
    @NotBlank @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority,
    @Size(max = 200) String sourceReference) {

  /** Converte os campos compartilhados para o contrato canônico de delegação. */
  public CreateAgentTaskByAgentRequest asTaskRequest() {
    return new CreateAgentTaskByAgentRequest(
        requestedByAgentKey, assignedAgentKey, title, description, priority, sourceReference);
  }
}
