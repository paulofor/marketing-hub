package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.LandingVideoSlotChangeType;
import lombok.Data;

import java.time.Instant;

/**
 * Histórico serializado de alterações de slot.
 */
@Data
public class LandingVideoSlotHistoryDto {
    private Long id;
    private Long slotId;
    private Long profileId;
    private Long landingPageId;
    private String tenantId;
    private String slotName;
    private Long assetId;
    private Long posterAssetId;
    private Long vttAssetId;
    private boolean autoplay;
    private boolean muted;
    private boolean loopVideo;
    private boolean controlsEnabled;
    private boolean lazyLoad;
    private LandingVideoSlotChangeType changeType;
    private String changedBy;
    private Instant changedAt;
    private String publishedBy;
    private Instant publishedAt;
    private String notes;
}
