package com.marketinghub.oprmcoletormei.marketimport.dto;

import java.time.Instant;

public record OprmImportFileEventRequestDto(
        String status,
        Long rowsRead,
        Long rowsValid,
        Long rowsRejected,
        String errorMessage,
        Instant finishedAt
) {
}
