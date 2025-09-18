package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStimulusType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Payload used to create steps inside a template.
 */
public record JourneyStepRequest(
        String name,
        String description,
        @NotNull JourneyPhase phase,
        @NotNull JourneyStimulusType stimulusType,
        Integer position,
        Long creativeId,
        Long angleId,
        Long visualProofId,
        Long emotionalTriggerId,
        String entryCondition,
        String exitCondition,
        Integer delayMinutes,
        Map<String, String> metadata
) {
}
