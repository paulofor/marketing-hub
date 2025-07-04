package com.marketinghub.media.dto;

import com.marketinghub.media.MediaProvider;
import lombok.Data;

/**
 * Request body for creating an audio narration.
 */
@Data
public class CreateAudioRequest {
    private MediaProvider provider;
    private String voice;
    private String script;
    private Long campaignId;
}
