package com.marketinghub.salesvideo.dto;

import lombok.Data;

import java.time.Instant;

/**
 * DTO para slots de vídeo vinculados à landing page.
 */
@Data
public class LandingVideoSlotDto {
    private Long id;
    private Long landingPageId;
    private Long profileId;
    private String slotName;
    private Long assetId;
    private Long posterAssetId;
    private Long vttAssetId;
    private String assetUrl;
    private String posterAssetUrl;
    private String vttAssetUrl;
    private boolean autoplay;
    private boolean muted;
    private boolean loopVideo;
    private boolean controlsEnabled;
    private boolean lazyLoad;
    private Instant publishedAt;
    private String publishedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
