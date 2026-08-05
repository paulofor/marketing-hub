package com.marketinghub.imagegenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Responsabilidade: representar o prompt informado pelo usuário para gerar uma imagem. */
public record ImageGeneratorRequest(
    @NotNull Long productId,
    @NotNull Long commercialPlanId,
    Long experimentId,
    @NotBlank @Size(max = 4000) String prompt) {
  /**
   * Mantém compatibilidade de compilação para fluxos internos que devem informar o contexto depois.
   */
  public ImageGeneratorRequest(String prompt) {
    this(null, null, null, prompt);
  }
}
