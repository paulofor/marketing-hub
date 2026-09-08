package com.marketinghub.businessprocesschain.learningcycle.v1.service.createCycle;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Responsabilidade: declarar a hipótese e os limites antes de iniciar uma iteração comercial. */
public record CreateLearningCycleRequest(
    @NotNull UUID requestKey,
    @NotNull @Positive Long chainDefinitionId,
    @NotNull @Positive Long experimentId,
    @Positive Long previousCycleId,
    boolean baseline,
    @NotBlank @Size(max = 160) String productVersion,
    @NotBlank @Size(max = 1000) String hypothesis,
    @NotBlank @Size(max = 1000) String mainChange,
    @NotBlank @Size(max = 1000) String successCriterion,
    @NotBlank @Size(max = 500) String audience,
    @NotBlank @Size(max = 500) String offer,
    @NotBlank @Size(max = 500) String acquisition,
    @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal budgetLimitBrl,
    @NotNull Instant windowStart,
    @NotNull Instant windowEnd,
    @Min(1) @Max(10000000) int sampleTarget,
    @Min(1) @Max(1000000) int minimumNetSales,
    @NotBlank @Size(max = 160) String operatorName) {}
