package com.marketinghub.oprm.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OprmCreateImportRunRequestDto(
        @NotNull LocalDate snapshotDate,
        @NotBlank String sourceUrl,
        @NotBlank String status,
        @NotNull Instant startedAt,
        Instant finishedAt,
        Integer filesTotal,
        Integer filesProcessed,
        Long rowsRead,
        Long rowsValid,
        Long rowsRejected,
        String errorMessage,
        @NotEmpty List<OprmImportFileSeedDto> files
) {}
