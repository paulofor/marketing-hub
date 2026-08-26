package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: receber a auditoria imutável da chamada de modelo de uma tarefa BPM. */
public record AgentTaskExecutionAuditRequest(
    @NotBlank @Size(max = 128) String modelCode,
    @NotBlank @Size(max = 32) String reasoningEffort,
    @NotBlank @Size(max = 16_777_215) String promptSent) {}
