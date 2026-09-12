package com.marketinghub.businessprocess.automation.v1.service.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Responsabilidade: transportar o contexto explícito sem aceitar decisão de próxima atividade. */
public record ProcessRunCommand(
    @NotNull @Positive Long chainId,
    @Positive Long learningCycleId,
    @NotBlank @Size(max = 255) String sourceReference) {}
