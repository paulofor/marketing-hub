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
                                   String detailsJson,
                                   String streamPlaybackUrl) {
    /** Mantém compatibilidade com callbacks que não entregam streaming. */
    public JobCompletionPayload(SalesVideoStatus status, Long assetId, Long posterAssetId, Long vttAssetId,
                                String providerJobId, String metadataJson, BigDecimal costUsd,
                                String message, String detailsJson) {
        this(status, assetId, posterAssetId, vttAssetId, providerJobId, metadataJson, costUsd, message, detailsJson, null);
    }
}
