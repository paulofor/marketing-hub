package com.marketinghub.videomanagement.client.payload;

import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;

/**
 * Payload para notificar falhas.
 */
public record JobFailurePayload(String failureCode,
                                String failureDetail,
                                SalesVideoStatus status,
                                String message,
                                Boolean retryable,
                                String retryReason) {
}
