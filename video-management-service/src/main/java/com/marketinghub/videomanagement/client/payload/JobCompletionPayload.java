package com.marketinghub.videomanagement.client.payload;

import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;

/**
 * Payload para finalizar jobs no backend.
 */
public record JobCompletionPayload(SalesVideoStatus status,
                                   Long assetId,
                                   Long posterAssetId,
                                   Long vttAssetId,
                                   String assetUrl,
                                   String posterAssetUrl,
                                   String vttAssetUrl,
                                   String providerJobId,
                                   String metadataJson,
                                   String message,
                                   String detailsJson) {
}
