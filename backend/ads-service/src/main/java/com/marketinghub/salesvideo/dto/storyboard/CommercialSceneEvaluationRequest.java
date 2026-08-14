package com.marketinghub.salesvideo.dto.storyboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Responsabilidade: receber a avaliação comercial de uma cena pela fronteira do Estúdio. */
public record CommercialSceneEvaluationRequest(
    @NotBlank String status,
    @NotNull @Min(0) @Max(100) Integer utilizationPercent,
    String notes,
    @NotBlank String evaluatedBy) {}
