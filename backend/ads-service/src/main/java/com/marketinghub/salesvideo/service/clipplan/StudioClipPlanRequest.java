package com.marketinghub.salesvideo.service.clipplan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: receber uma simulação de tomadas sem autorização ou chamada de geração. */
public record StudioClipPlanRequest(
    @NotBlank String providerName,
    @Min(1) @Max(3600) int finalSeconds,
    @NotBlank String resolution,
    boolean audio,
    @Min(0) int imageReferences,
    @Min(0) @Max(30) int referenceVideoSeconds) {}
