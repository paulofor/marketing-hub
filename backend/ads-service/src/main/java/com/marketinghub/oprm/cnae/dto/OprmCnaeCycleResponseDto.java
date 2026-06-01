package com.marketinghub.oprm.cnae.dto;

import java.time.Instant;

/**
 * DTO de resposta para ciclos operacionais CNAE persistidos pelo backend.
 */
public record OprmCnaeCycleResponseDto(
        String cycleId,
        String cycleType,
        Long cycleNumber,
        String status,
        String selectionCriteria,
        Integer processedCount,
        Integer failedCount,
        Instant startedAt,
        Instant finishedAt,
        String summary,
        String errorMessage) {}
