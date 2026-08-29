package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Responsabilidade: receber a auditoria imutável da chamada de modelo de uma tarefa BPM. */
public record AgentTaskExecutionAuditRequest(
    @NotBlank @Size(max = 24) String executionMode,
    @Size(max = 128) String modelCode,
    @Size(max = 32) String reasoningEffort,
    @Size(max = 16_777_215) String promptSent,
    @Size(max = 50) List<@Valid AgentTaskAccessedUrlRequest> accessedUrls) {

  /**
   * Mantém compatibilidade tipada com produtores que já enviavam uma chamada de modelo completa.
   */
  public AgentTaskExecutionAuditRequest(
      String modelCode, String reasoningEffort, String promptSent) {
    this("MODEL", modelCode, reasoningEffort, promptSent, List.of());
  }
}
