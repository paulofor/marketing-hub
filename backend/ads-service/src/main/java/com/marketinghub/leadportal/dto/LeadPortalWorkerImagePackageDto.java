package com.marketinghub.leadportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Dados retornados para o worker com os pacotes de imagens pendentes.
 */
public record LeadPortalWorkerImagePackageDto(
        long id,
        @JsonProperty("submission_id") UUID submissionId,
        @JsonProperty("stored_file_name") String storedFileName,
        @JsonProperty("planned_outputs") Integer plannedOutputs,
        @JsonProperty("free_images") Integer freeImages,
        String model,
        String prompt,
        String treatment) {
}
