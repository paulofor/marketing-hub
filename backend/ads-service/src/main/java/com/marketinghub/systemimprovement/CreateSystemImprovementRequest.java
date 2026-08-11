package com.marketinghub.systemimprovement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contrato usado por qualquer agente para sugerir uma melhoria durante sua tarefa. */
public record CreateSystemImprovementRequest(
    @NotBlank @Size(max = 100) String agentKey,
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 10000) String description,
    @Size(max = 200) String taskReference) {}
