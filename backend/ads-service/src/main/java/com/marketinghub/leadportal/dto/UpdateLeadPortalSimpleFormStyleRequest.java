package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateLeadPortalSimpleFormStyleRequest {

    private String name;

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug deve conter apenas letras minúsculas, números e hífens")
    private String slug;

    private String description;

    private String textModel;

    private String textPrompt;

    private String textParameters;

    private String imageModel;

    private String imagePrompt;

    private String imageNegativePrompt;

    private String imageParameters;

    @Positive(message = "imageBatchSize deve ser maior que zero")
    private Integer imageBatchSize;

    private String imageAspectRatio;

    private String previewImageUrl;

    @Valid
    private LeadPortalSimpleFormStyleDefinition definition;
}
