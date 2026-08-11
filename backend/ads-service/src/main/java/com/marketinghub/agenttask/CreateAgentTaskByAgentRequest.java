package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Responsabilidade: validar uma tarefa delegada por um agente a outro agente. */
public record CreateAgentTaskByAgentRequest(
    @NotBlank @Size(max = 100) String requestedByAgentKey,
    @NotBlank @Size(max = 100) String assignedAgentKey,
    @NotBlank @Size(max = 160) String title,
    @NotBlank String description,
    @NotBlank @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority,
    @Size(max = 200) String sourceReference) {}
