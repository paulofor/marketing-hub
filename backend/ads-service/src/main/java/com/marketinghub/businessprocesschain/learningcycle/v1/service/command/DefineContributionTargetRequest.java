package com.marketinghub.businessprocesschain.learningcycle.v1.service.command;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Responsabilidade: registrar a meta escolhida pelo usuário sem orçamento ou aprovação automática.
 */
public record DefineContributionTargetRequest(
    @NotNull UUID requestKey,
    @Min(0) long expectedRevision,
    @NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal minimumContributionPercent,
    @NotBlank @Size(max = 1000) String rationale) {}
