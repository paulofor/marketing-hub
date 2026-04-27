package com.marketinghub.mds.dto;

import com.marketinghub.mds.MdsEventType;

import java.time.Instant;
import java.util.Map;

public record MdsAdminProcessingEventResponse(
        Long eventId,
        String stageName,
        MdsEventType eventType,
        String message,
        Map<String, Object> payload,
        Instant createdAt
) {
}
