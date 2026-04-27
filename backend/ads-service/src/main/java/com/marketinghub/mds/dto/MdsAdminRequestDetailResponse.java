package com.marketinghub.mds.dto;

import com.marketinghub.mds.MdsRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MdsAdminRequestDetailResponse(
        Long requestId,
        MdsRequestStatus status,
        String market,
        String problem,
        String desiredOutcome,
        String deliveryConstraint,
        String evidencePreference,
        String correlationId,
        String failureReason,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> context,
        List<MdsAdminProcessingEventResponse> timeline,
        String failureClassification,
        String artifactsUrl,
        String reportUrl,
        boolean retryEligible,
        String retryReason
) {
}
