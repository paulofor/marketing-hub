package com.marketinghub.imagegenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Responsabilidade: solicitar a aplicação de uma imagem gerada a um slot canônico da landing. */
public record LandingImagePromotionRequest(
    @NotNull Long experimentId,
    @NotBlank @Pattern(regexp = "hero-media-img|prova-img", message = "Slot de landing inválido")
        String slotId) {}
