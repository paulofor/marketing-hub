package com.marketinghub.agentlearning.v1;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Contrato dos resultados reproduzíveis obtidos na sandbox local. */
public record EvaluateLearningExperimentRequest(
    @NotNull @DecimalMin("0") BigDecimal baselineHoldoutScore,
    @NotNull @DecimalMin("0") BigDecimal candidateHoldoutScore,
    @NotNull @DecimalMin("0") BigDecimal baselineCost,
    @NotNull @DecimalMin("0") BigDecimal candidateCost,
    @Positive int replayCaseCount,
    @Positive int holdoutCaseCount,
    boolean regressionPassed,
    boolean localValidationPassed,
    boolean externalProviderCalled,
    boolean spendingAuthorized,
    boolean publicationPerformed,
    @NotBlank @Size(max = 100000) String baselineResultJson,
    @NotBlank @Size(max = 100000) String candidateResultJson) {}
