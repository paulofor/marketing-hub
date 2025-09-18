package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyPhase;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Partial update payload for {@link com.marketinghub.journey.model.JourneyTemplate}.
 */
public record JourneyTemplateUpdateRequest(
        String name,
        String description,
        String objective,
        List<JourneyPhase> phases,
        String preferredChannel,
        Set<String> tags,
        Map<String, String> metadata
) {
}
