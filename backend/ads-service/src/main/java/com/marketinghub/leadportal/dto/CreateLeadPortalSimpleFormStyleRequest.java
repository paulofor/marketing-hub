package com.marketinghub.leadportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateLeadPortalSimpleFormStyleRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug deve conter apenas letras minúsculas, números e hífens")
    private String slug;

    private String description;

    @NotBlank
    private String textModel;

    @NotBlank
    private String textPrompt;

    private String previewImageUrl;
}
