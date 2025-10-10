package com.marketinghub.experiment.dto;

import com.marketinghub.journey.dto.JourneyAssignmentResponse;

import java.util.List;

/**
 * Response payload summarising the journey assignments generated for an experiment.
 */
public record ExperimentJourneyAssignmentsResponse(
        Long journeyId,
        Long templateId,
        List<JourneyAssignmentResponse> assignments
) {
}
