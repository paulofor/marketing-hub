package com.marketinghub.leadportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Payload enviado pelo worker com o resultado do processamento automático.
 */
public record LeadPortalWorkerImageResultRequest(
        @NotEmpty @Valid List<GeneratedImageRequest> images,
        String model,
        @NotBlank String prompt) {

    public record GeneratedImageRequest(
            @JsonProperty("stored_file_name") @NotBlank String storedFileName,
            @JsonProperty("public_url") @NotBlank String publicUrl,
            String model,
            String prompt,
            String source,
            Integer width,
            Integer height,
            String orientation) {
    }
}
