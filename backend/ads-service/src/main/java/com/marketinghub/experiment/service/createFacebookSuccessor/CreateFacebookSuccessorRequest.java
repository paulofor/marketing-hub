package com.marketinghub.experiment.service.createFacebookSuccessor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Recebe o contrato financeiro e as identidades Meta do sucessor Facebook. */
public record CreateFacebookSuccessorRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal dailyBudget,
    @NotNull @DecimalMin(value = "0.01") BigDecimal mediaSpendLimit,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull Long facebookPageId,
    @NotNull Long instagramAccountId) {}
