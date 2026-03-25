package com.marketinghub.videomanagement.client.payload;

import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;

/**
 * Atualização de progresso enviada ao backend.
 */
public record JobProgressPayload(Integer progressPercent,
                                 SalesVideoStatus status,
                                 String message,
                                 String detailsJson) {
}
