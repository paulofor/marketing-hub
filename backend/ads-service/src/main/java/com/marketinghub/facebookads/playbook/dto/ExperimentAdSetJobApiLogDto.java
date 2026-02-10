package com.marketinghub.facebookads.playbook.dto;

import java.time.Instant;

/**
 * API interaction snapshot exposed to the frontend.
 */
public record ExperimentAdSetJobApiLogDto(
        Long id,
        String provider,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        String requestPayload,
        String responsePayload,
        String errorMessage,
        Instant requestedAt,
        Instant respondedAt,
        Instant createdAt
) {
}
