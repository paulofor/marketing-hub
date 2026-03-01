package com.marketinghub.leadportal.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateLeadPortalSimpleFormStyleRequest {

    private String name;

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug deve conter apenas letras minúsculas, números e hífens")
    private String slug;

    private String description;

    private String textModel;

    private String textPrompt;

    private String previewImageUrl;

    private Boolean regenerate;
}
