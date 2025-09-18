package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyPhase;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Payload used to create journey templates.
 */
public record JourneyTemplateRequest(
        @NotBlank String name,
        String description,
        String objective,
        List<JourneyPhase> phases,
        String preferredChannel,
        Set<String> tags,
        Map<String, String> metadata
) {
}
