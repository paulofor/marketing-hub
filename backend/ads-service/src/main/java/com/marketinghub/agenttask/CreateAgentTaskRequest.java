package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Responsabilidade: validar uma tarefa aberta por uma pessoa para um agente. */
public record CreateAgentTaskRequest(
    @NotBlank @Size(max = 100) String assignedAgentKey,
    @NotBlank @Size(max = 100) String requestedByName,
    @NotBlank @Size(max = 160) String title,
    @NotBlank String description,
    @NotBlank @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority,
    @Size(max = 200) String sourceReference) {}
