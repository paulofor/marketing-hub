package com.marketinghub.mds.dto;

import java.util.Map;

public record MdsHeartbeatRequest(
        String stageName,
        String message,
        Map<String, Object> payload
) {
}
