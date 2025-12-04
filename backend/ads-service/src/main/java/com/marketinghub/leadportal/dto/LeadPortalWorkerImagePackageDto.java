package com.marketinghub.leadportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
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
        String treatment,
        @JsonProperty("image_model_id") Long imageModelId,
        @JsonProperty("image_model_quality_id") Long imageModelQualityId,
        @JsonProperty("image_orientation") String imageOrientation,
        @JsonProperty("image_width") Integer imageWidth,
        @JsonProperty("image_height") Integer imageHeight,
        @JsonProperty("image_unit_price_usd") BigDecimal imageUnitPriceUsd,
        @JsonProperty("image_total_price_usd") BigDecimal imageTotalPriceUsd,
        @JsonProperty("image_currency") String imageCurrency) {
}
