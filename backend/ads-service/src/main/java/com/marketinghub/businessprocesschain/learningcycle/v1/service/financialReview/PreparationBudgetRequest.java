package com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Responsabilidade: receber decisão humana cumulativa sem transferir teto entre ciclos. */
public record PreparationBudgetRequest(
    @NotNull UUID requestKey,
    @NotNull @PositiveOrZero Long expectedRevision,
    @NotNull @Positive Long chainDefinitionId,
    @NotNull @Positive Long experimentId,
    @NotBlank @Size(max = 160) String productVersion,
    @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal totalLimitUsd,
    @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal reserveUsd,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 4000) String justification,
    @NotNull @AssertTrue Boolean confirmed) {}
