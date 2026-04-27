package com.marketinghub.mds.dto;

import com.marketinghub.mds.MdsRequestStatus;

public record MdsAdminRetryResponse(
        Long requestId,
        MdsRequestStatus previousStatus,
        MdsRequestStatus currentStatus,
        String message
) {
}
