package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStimulusType;

import java.util.Map;

/**
 * Partial update payload for journey steps.
 */
public record JourneyStepUpdateRequest(
        String name,
        String description,
        JourneyPhase phase,
        JourneyStimulusType stimulusType,
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
