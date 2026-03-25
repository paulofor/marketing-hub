package com.marketinghub.videomanagement.client.payload;

/**
 * Payload para claim de job no backend.
 */
public record JobClaimPayload(String workerId, String message) {
}
