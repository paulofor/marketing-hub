package com.marketinghub.imagegenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Responsabilidade: representar o prompt informado pelo usuário para gerar uma imagem. */
public record ImageGeneratorRequest(
    @NotNull Long productId,
    @NotNull Long commercialPlanId,
    Long experimentId,
    @NotBlank @Size(max = 4000) String prompt,
    @Size(max = 100) String operationKey) {
  /** Preserva chamadas existentes enquanto produtos sem ficha conservam seu contrato anterior. */
  public ImageGeneratorRequest(
      Long productId, Long commercialPlanId, Long experimentId, String prompt) {
    this(productId, commercialPlanId, experimentId, prompt, null);
  }

  /**
   * Mantém compatibilidade de compilação para fluxos internos que devem informar o contexto depois.
   */
  public ImageGeneratorRequest(String prompt) {
    this(null, null, null, prompt);
  }
}
