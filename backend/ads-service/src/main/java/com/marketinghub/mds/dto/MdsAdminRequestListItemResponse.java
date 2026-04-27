package com.marketinghub.mds.dto;

import com.marketinghub.mds.MdsRequestStatus;

import java.time.Instant;

public record MdsAdminRequestListItemResponse(
        Long requestId,
        String market,
        String problem,
        String desiredOutcome,
        MdsRequestStatus status,
        String currentStage,
        int attempt,
        Instant lastHeartbeatAt,
        Instant updatedAt,
        boolean retryEligible,
        String retryReason
) {
}
