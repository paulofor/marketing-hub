package com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Responsabilidade: receber a autorização humana limitada à produção e revisão das duas peças. */
public record AuthorizeVideoBudgetRequest(
    @NotNull UUID requestKey,
    @NotNull @PositiveOrZero Long expectedRevision,
    @NotNull @Positive Long chainDefinitionId,
    @NotNull @Positive Long experimentId,
    @NotBlank @Size(max = 160) String productVersion,
    @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal budgetLimitUsd,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 4000) String justification,
    @NotNull @AssertTrue Boolean confirmed) {}
