package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Partial update payload for journey instances.
 */
public record JourneyUpdateRequest(
        Long templateId,
        String name,
        String description,
        JourneyStatus status,
        Long marketNicheId,
        Long experimentId,
        String segmentReference,
        String segmentFilter,
        Instant startAt,
        Instant endAt,
        Map<String, String> metadata
) {
}
