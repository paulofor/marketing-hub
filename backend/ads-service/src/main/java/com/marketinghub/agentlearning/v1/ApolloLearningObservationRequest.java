package com.marketinghub.agentlearning.v1;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Contrato de uma comparação sombra produzida por Apolo e revisada fora do executor. */
public record ApolloLearningObservationRequest(
    @NotNull @Positive Long jobId,
    @NotBlank @Size(max = 120) String scopeId,
    @NotBlank @Size(max = 80) String baselineVersion,
    @NotBlank @Size(max = 80) String candidateVersion,
    @NotNull @DecimalMin("0") BigDecimal baselineScore,
    @NotNull @DecimalMin("0") BigDecimal candidateScore,
    @NotNull @DecimalMin("0") BigDecimal baselineCost,
    @NotNull @DecimalMin("0") BigDecimal candidateCost,
    @NotBlank @Size(max = 100000) String comparisonJson,
    boolean providerCalled,
    boolean spendingAuthorized,
    boolean publicationPerformed) {}
