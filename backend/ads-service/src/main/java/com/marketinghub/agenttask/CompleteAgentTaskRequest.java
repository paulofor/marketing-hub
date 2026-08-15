package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: validar o resultado funcional reportado por um executor de atividade BPM. */
public record CompleteAgentTaskRequest(
    @NotBlank String resultJson, @NotBlank String evidenceJson) {}
