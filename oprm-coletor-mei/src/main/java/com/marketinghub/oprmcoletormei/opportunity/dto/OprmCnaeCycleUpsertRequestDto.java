package com.marketinghub.oprmcoletormei.opportunity.dto;

import java.time.Instant;

/** DTO usado pelo OPRM para registrar ciclo operacional CNAE no backend. */
public record OprmCnaeCycleUpsertRequestDto(
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
