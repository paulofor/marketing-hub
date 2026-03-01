package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
public class LeadPortalSimpleFormStyleDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String textModel;
    private String textPrompt;
    private String textParameters;
    private String imageModel;
    private String imagePrompt;
    private String imageNegativePrompt;
    private String imageParameters;
    private Integer imageBatchSize;
    private String imageAspectRatio;
    private String previewImageUrl;
    private LeadPortalSimpleFormStyleDefinition definition;
    private BigDecimal generationCostUsd;
    private String generationStatus;
    private String generationError;
    private Instant createdAt;
    private Instant updatedAt;
}
