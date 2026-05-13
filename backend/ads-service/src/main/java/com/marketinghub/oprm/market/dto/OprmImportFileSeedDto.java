package com.marketinghub.oprm.market.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record OprmImportFileSeedDto(
        @NotBlank String fileName,
        @NotBlank String fileUrl,
        @NotBlank String datasetType,
        String status,
        Long rowsRead,
        Long rowsValid,
        Long rowsRejected,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {}
