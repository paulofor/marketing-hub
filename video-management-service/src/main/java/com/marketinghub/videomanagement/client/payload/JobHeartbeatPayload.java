package com.marketinghub.videomanagement.client.payload;

public record JobHeartbeatPayload(
        String message,
        String detailsJson) {
}
