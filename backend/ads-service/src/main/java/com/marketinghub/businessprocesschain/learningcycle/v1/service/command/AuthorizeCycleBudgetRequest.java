package com.marketinghub.businessprocesschain.learningcycle.v1.service.command;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Responsabilidade: receber o aceite administrativo dos dois limites de mídia. */
public record AuthorizeCycleBudgetRequest(
    @NotNull UUID requestKey,
    @Min(0) long expectedRevision,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal dailyBudgetBrl,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal budgetLimitBrl) {}
