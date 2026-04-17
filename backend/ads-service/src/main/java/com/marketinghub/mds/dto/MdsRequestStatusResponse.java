package com.marketinghub.mds.dto;

import com.marketinghub.mds.MdsRequestStatus;

import java.time.Instant;

public record MdsRequestStatusResponse(
        Long id,
        MdsRequestStatus status,
        String market,
        String problem,
        String desiredOutcome,
        String correlationId,
        String failureReason,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Instant updatedAt
) {
}
