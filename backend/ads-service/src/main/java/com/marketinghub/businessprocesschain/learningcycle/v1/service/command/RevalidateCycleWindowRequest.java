package com.marketinghub.businessprocesschain.learningcycle.v1.service.command;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Responsabilidade: solicitar nova janela de um ciclo planejado sem ampliar seu teto financeiro.
 */
public record RevalidateCycleWindowRequest(
    @NotNull UUID requestKey,
    @Min(0) long expectedRevision,
    @NotNull @FutureOrPresent LocalDate startDate,
    @NotNull @FutureOrPresent LocalDate endDate,
    @NotBlank @Size(max = 2000) String reason) {}
