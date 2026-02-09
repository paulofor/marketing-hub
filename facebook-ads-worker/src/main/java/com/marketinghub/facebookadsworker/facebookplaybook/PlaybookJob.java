package com.marketinghub.facebookadsworker.facebookplaybook;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Job claimed from the backend queue.
 */
public record PlaybookJob(
        long id,
        PlaybookJobType type,
        Long workflowId,
        Long resourceId,
        JsonNode payload,
        Instant createdAt
) {
}
