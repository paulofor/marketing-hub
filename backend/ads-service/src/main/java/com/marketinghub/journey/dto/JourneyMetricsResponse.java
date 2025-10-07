package com.marketinghub.journey.dto;

import com.marketinghub.journey.model.JourneyStatus;

import java.util.Map;

/**
 * Aggregated metrics for journey instances exposed via the API.
 */
public record JourneyMetricsResponse(
        long totalJourneys,
        Map<JourneyStatus, Long> statusBreakdown
) {
}
