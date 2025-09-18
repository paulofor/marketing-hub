package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStimulusType;

import java.util.Map;

/**
 * Representation of a journey step exposed via the API.
 */
public record JourneyStepResponse(
        Long id,
        Long templateId,
        Integer position,
        String name,
        String description,
        JourneyPhase phase,
        JourneyStimulusType stimulusType,
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
