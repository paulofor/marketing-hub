package com.marketinghub.journey.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified ingest payload for external events.
 */
public record EventLogRequest(
        UUID actorId,
        @NotBlank String eventType,
        Long journeyId,
        Long journeyStepId,
        String source,
        String campaignId,
        Map<String, Object> metadata,
        BigDecimal value,
        Instant occurredAt
) {
}
