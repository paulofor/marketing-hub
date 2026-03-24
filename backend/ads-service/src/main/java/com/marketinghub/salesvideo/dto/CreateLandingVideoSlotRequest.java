package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Criação de slots publicados na landing page.
 */
@Data
public class CreateLandingVideoSlotRequest {
    @NotNull
    private Long profileId;

    @NotBlank
    @Size(max = 64)
    private String slotName;

    @NotNull
    private Long assetId;

    private Long posterAssetId;
    private Long vttAssetId;

    private boolean autoplay = true;
    private boolean muted = true;
    private boolean loopVideo;
    private boolean controlsEnabled = true;
    private boolean lazyLoad = true;

    private String publishedBy;
}
