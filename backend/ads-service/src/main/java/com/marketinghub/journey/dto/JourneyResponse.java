package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Representation of a journey instance returned by the API.
 */
public record JourneyResponse(
        Long id,
        Long templateId,
        String templateName,
        String name,
        String description,
        JourneyStatus status,
        Long marketNicheId,
        Long experimentId,
        String segmentReference,
        String segmentFilter,
        Map<String, String> metadata,
        Instant startAt,
        Instant endAt,
        Instant createdAt,
        Instant updatedAt
) {
}
