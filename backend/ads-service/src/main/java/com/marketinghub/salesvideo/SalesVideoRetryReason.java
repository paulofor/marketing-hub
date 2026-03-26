package com.marketinghub.salesvideo;

/**
 * Motivos categorizados para reprocessar jobs de vídeo.
 */
public enum SalesVideoRetryReason {
    MANUAL_INTERVENTION,
    PROVIDER_FAILURE,
    ASSET_EXPIRED,
    QUALITY_ASSURANCE,
    AUTO_RECOVERY,
    OTHER
}
