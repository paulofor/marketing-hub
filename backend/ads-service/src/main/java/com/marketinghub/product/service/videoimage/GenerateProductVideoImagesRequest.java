package com.marketinghub.product.service.videoimage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: receber o prompt usado para gerar imagens de vídeo do produto. */
public record GenerateProductVideoImagesRequest(@NotBlank @Size(max = 4000) String prompt) {}
