package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** Responsabilidade: receber o consumo real de uma chamada de modelo vinculada à tarefa. */
public record AgentTaskModelUsageRequest(
    @NotBlank String modelCode,
    @NotBlank @Pattern(regexp = "(?i)STANDARD|FLEX|BATCH") String serviceTier,
    @NotNull @PositiveOrZero Long inputTokens,
    @NotNull @PositiveOrZero Long cachedInputTokens,
    @NotNull @PositiveOrZero Long outputTokens) {}
