package com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Responsabilidade: receber somente o teto escolhido e a confirmação do seu contexto financeiro.
 */
public record ApproveCostCeilingRequest(
    @NotNull UUID requestKey,
    @NotNull @PositiveOrZero Long expectedRevision,
    @NotNull @Positive Long chainDefinitionId,
    @NotNull @Positive Long experimentId,
    @NotBlank @Size(max = 160) String productVersion,
    @NotNull Group code,
    @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal approved,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 4000) String justification,
    @NotNull @AssertTrue Boolean confirmed) {
  /** Responsabilidade: limitar a decisão aos seis grupos financeiros canônicos. */
  public enum Group {
    AGENTS,
    PRODUCTION,
    MEDIA,
    DELIVERY,
    SALES_DEDUCTIONS,
    FIXED_OPERATION
  }
}
