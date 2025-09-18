package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * Payload for creating a journey instance.
 */
public record JourneyRequest(
        @NotNull Long templateId,
        @NotBlank String name,
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
