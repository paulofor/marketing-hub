package com.marketinghub.experiment.dto;

import com.marketinghub.experiment.CreativeType;
import lombok.Data;

/**
 * Request to create a creative variant.
 */
@Data
public class CreateCreativeRequest {
    private Long experimentId;
    private CreativeType type;
    private String assetUrl;
    private String titles;
    private String descriptions;
}
