package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Responsabilidade: validar a evolução de estado de uma tarefa do agente. */
public record UpdateAgentTaskStatusRequest(
    @NotBlank @Pattern(regexp = "PENDING|IN_PROGRESS|COMPLETED|BLOCKED|CANCELLED") String status) {}
