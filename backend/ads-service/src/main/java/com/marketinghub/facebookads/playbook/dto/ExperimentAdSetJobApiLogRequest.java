package com.marketinghub.facebookads.playbook.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Payload describing a single API interaction executed while processing a job.
 */
public record ExperimentAdSetJobApiLogRequest(
        String provider,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        JsonNode requestPayload,
        JsonNode responsePayload,
        String errorMessage,
        Instant requestedAt,
        Instant respondedAt
) {
}
