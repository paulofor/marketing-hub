package com.marketinghub.creative.service.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Entrada comercial para aproveitar um vídeo aprovado sem gerar outra mídia. */
public record VideoCreativeRequest(
    @NotBlank String headline,
    @NotBlank String primaryText,
    String description,
    @Positive Long replacesVideoAssetId) {}
