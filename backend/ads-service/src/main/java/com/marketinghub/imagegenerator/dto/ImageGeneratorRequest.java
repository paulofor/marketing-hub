package com.marketinghub.imagegenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: representar o prompt informado pelo usuário para gerar uma imagem. */
public record ImageGeneratorRequest(@NotBlank @Size(max = 4000) String prompt) {}
