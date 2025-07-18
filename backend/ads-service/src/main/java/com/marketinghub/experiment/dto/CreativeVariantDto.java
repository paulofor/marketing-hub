package com.marketinghub.experiment.dto;

import com.marketinghub.experiment.CreativeType;
import java.time.Instant;
import lombok.Data;

/**
 * DTO for CreativeVariant.
 */
@Data
public class CreativeVariantDto {
    private Long id;
    private Long experimentId;
    private CreativeType type;
    private String assetUrl;
    private String titles;
    private String descriptions;
    private Instant createdAt;
    private Instant updatedAt;
}
