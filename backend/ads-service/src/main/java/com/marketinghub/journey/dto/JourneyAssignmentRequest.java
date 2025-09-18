package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyAssignmentStatus;

import java.util.List;
import java.util.UUID;

/**
 * Batch assignment payload for linking leads or segments to a journey.
 */
public record JourneyAssignmentRequest(
        List<UUID> leadIds,
        List<String> segmentIdentifiers,
        JourneyAssignmentStatus status,
        Long currentStepId,
        Long nextStepId,
        String contextPayload
) {
}
