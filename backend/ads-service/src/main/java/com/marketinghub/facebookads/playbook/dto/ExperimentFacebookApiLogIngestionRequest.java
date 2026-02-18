package com.marketinghub.facebookads.playbook.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookads.playbook.ExperimentFacebookApiLogContext;

import java.time.Instant;
import java.util.List;

/**
 * Payload posted by workers to register Facebook Graph API interactions.
 */
public record ExperimentFacebookApiLogIngestionRequest(
        ExperimentFacebookApiLogContext context,
        List<ApiCallPayload> logs
) {
    public record ApiCallPayload(
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
}
