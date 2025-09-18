package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyAssignmentStatus;
import com.marketinghub.journey.model.JourneyAssignmentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation of a journey assignment.
 */
public record JourneyAssignmentResponse(
        Long id,
        Long journeyId,
        JourneyAssignmentType type,
        JourneyAssignmentStatus status,
        UUID leadId,
        String segmentIdentifier,
        Long currentStepId,
        Long nextStepId,
        Instant lastEventAt,
        String contextPayload,
        Instant createdAt,
        Instant updatedAt
) {
}
