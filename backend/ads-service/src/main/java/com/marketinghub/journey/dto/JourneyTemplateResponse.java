package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyPhase;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full representation of a journey template.
 */
public record JourneyTemplateResponse(
        Long id,
        String name,
        String description,
        String objective,
        List<JourneyPhase> phases,
        String preferredChannel,
        Set<String> tags,
        Map<String, String> metadata,
        List<JourneyStepResponse> steps,
        Instant createdAt,
        Instant updatedAt
) {
}
