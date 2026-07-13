package com.marketinghub.videomanagement.client.payload;

import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import java.math.BigDecimal;

/**
 * Payload para finalizar jobs no backend.
 */
public record JobCompletionPayload(SalesVideoStatus status,
                                   Long assetId,
                                   Long posterAssetId,
                                   Long vttAssetId,
                                   String providerJobId,
                                   String metadataJson,
                                   BigDecimal costUsd,
                                   String message,
                                   String detailsJson) {
}
