package com.marketinghub.salesvideo.dto;

import lombok.Data;

/**
 * Atualização parcial de slots publicados.
 */
@Data
public class UpdateLandingVideoSlotRequest {
    private Long profileId;
    private String slotName;
    private Long assetId;
    private Long posterAssetId;
    private Long vttAssetId;
    private Boolean autoplay;
    private Boolean muted;
    private Boolean loopVideo;
    private Boolean controlsEnabled;
    private Boolean lazyLoad;
    private String publishedBy;
}
