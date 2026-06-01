package com.marketinghub.oprm.cnae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * DTO de criação ou atualização de ciclo operacional CNAE executado pelo módulo OPRM.
 */
public record OprmCnaeCycleUpsertRequestDto(
        @NotBlank String cycleId,
        @NotBlank String cycleType,
        @NotNull Long cycleNumber,
        @NotBlank String status,
        String selectionCriteria,
        Integer processedCount,
        Integer failedCount,
        @NotNull Instant startedAt,
        Instant finishedAt,
        String summary,
        String errorMessage) {}
