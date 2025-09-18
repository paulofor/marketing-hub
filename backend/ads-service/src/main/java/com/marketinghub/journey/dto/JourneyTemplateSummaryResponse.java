package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyPhase;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight representation of a {@link com.marketinghub.journey.model.JourneyTemplate}.
 */
public record JourneyTemplateSummaryResponse(
        Long id,
        String name,
        String objective,
        List<JourneyPhase> phases,
        String preferredChannel,
        Set<String> tags,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt
) {
}
