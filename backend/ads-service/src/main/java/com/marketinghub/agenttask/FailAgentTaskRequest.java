package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: validar a causa auditável de uma falha do executor BPM. */
public record FailAgentTaskRequest(@NotBlank String error) {}
