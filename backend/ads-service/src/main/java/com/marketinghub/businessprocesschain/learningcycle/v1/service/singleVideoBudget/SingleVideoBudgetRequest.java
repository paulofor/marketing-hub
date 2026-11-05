package com.marketinghub.businessprocesschain.learningcycle.v1.service.singleVideoBudget;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Preserva uma autorização incremental integral para um único projeto existente. */
public record SingleVideoBudgetRequest(
    @NotNull UUID requestKey,
    @NotNull Long expectedRevision,
    @NotNull Long chainDefinitionId,
    @NotNull Long experimentId,
    @NotBlank String productVersion,
    @NotNull Long videoProjectId,
    @NotNull Long profileId,
    @NotNull @DecimalMin("0.01") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
        BigDecimal totalLimitUsd,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 4000) String authorizationSource,
    @NotNull Boolean confirmed) {}
