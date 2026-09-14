package com.marketinghub.product.executionprofile.v1.service.review;

import jakarta.validation.constraints.*;

/** Responsabilidade: registrar a decisão identificada após leitura do parecer financeiro. */
public record ReviewProfileRequest(
    @NotNull Checkpoint checkpoint,
    @NotNull Long financialExecutionId,
    boolean approved,
    @NotBlank @Size(max = 100) String reviewedBy,
    @NotBlank @Size(max = 2000) String rationale) {
  /** Pontos financeiros da cadeia comum que exigem evidência na mesma revisão. */
  public enum Checkpoint {
    OFFER,
    DELIVERY_DESIGN,
    HOMOLOGATION,
    OPERATION
  }
}
