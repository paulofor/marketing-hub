package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Responsabilidade: validar o vínculo tardio de uma tarefa excepcional a uma atividade BPM. */
public record BindAgentTaskProcessRequest(
    @NotNull Long processDefinitionId,
    @NotBlank @Size(max = 100) String processActivityId) {}
