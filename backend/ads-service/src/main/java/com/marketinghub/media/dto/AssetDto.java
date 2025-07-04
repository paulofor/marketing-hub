package com.marketinghub.media.dto;

import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import lombok.Data;

import java.time.Instant;

/**
 * Data transfer object for {@link com.marketinghub.media.Asset}.
 */
@Data
public class AssetDto {
    private Long id;
    private AssetType type;
    private MediaProvider provider;
    private String externalId;
    private AssetStatus status;
    private String url;
    private String payload;
    private Long campaignId;
    private Instant createdAt;
    private Instant updatedAt;
}
