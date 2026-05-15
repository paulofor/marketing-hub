package com.marketinghub.oprmcoletormei.marketimport.dto;

import java.time.Instant;

public record OprmCompleteImportRunRequestDto(
        String status,
        Instant finishedAt,
        Integer filesProcessed,
        Long rowsRead,
        Long rowsValid,
        Long rowsRejected,
        String errorMessage
) {
}
