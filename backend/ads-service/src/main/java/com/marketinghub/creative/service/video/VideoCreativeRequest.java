package com.marketinghub.creative.service.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Entrada comercial para aproveitar um vídeo aprovado sem gerar outra mídia. */
public record VideoCreativeRequest(
    @NotBlank @Size(max = 255) String headline,
    @NotBlank @Size(max = 5000) String primaryText,
    @Size(max = 255) String description,
    @Positive Long replacesVideoAssetId) {}
