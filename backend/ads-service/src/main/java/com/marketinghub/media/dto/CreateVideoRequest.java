package com.marketinghub.media.dto;

import com.marketinghub.media.MediaProvider;
import lombok.Data;

/**
 * Request body for creating a video asset.
 */
@Data
public class CreateVideoRequest {
    private MediaProvider provider;
    private String avatar;
    private String voice;
    private String script;
    private Long campaignId;
}
