package com.marketinghub.oprmcoletormei.marketimport.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OprmCreateImportRunRequestDto(
        LocalDate snapshotDate,
        String sourceUrl,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Integer filesTotal,
        Integer filesProcessed,
        Long rowsRead,
        Long rowsValid,
        Long rowsRejected,
        String errorMessage,
        List<OprmImportFileSeedDto> files
) {}
