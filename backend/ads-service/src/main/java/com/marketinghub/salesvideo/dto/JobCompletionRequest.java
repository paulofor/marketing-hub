package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoStatus;
import lombok.Data;

/**
 * Payload para finalizar um job.
 */
@Data
public class JobCompletionRequest {
    private SalesVideoStatus status;
    private Long assetId;
    private Long posterAssetId;
    private Long vttAssetId;
    private String providerJobId;
    private String metadataJson;
    private String message;
    private String detailsJson;
}
