package com.marketinghub.journey.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Representation of an ingested event.
 */
public record EventLogResponse(
        Long id,
        UUID actorId,
        String eventType,
        Long journeyId,
        Long journeyStepId,
        String source,
        String campaignId,
        Map<String, Object> metadata,
        BigDecimal value,
        Instant occurredAt,
        Instant receivedAt
) {
}
