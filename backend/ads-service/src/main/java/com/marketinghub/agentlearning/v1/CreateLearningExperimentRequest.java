package com.marketinghub.agentlearning.v1;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Contrato para congelar uma hipótese e seus conjuntos de replay antes da avaliação. */
public record CreateLearningExperimentRequest(
    @NotBlank @Pattern(regexp = "landing-generator|meta-ad-approver|apollo") String agentKey,
    @NotBlank @Size(max = 60) String scopeType,
    @NotBlank @Size(max = 120) String scopeId,
    @NotNull @Positive Long memoryId,
    @NotBlank @Size(max = 80) String candidateVersion,
    @NotBlank @Size(max = 80) String baselineVersion,
    @NotBlank @Size(max = 100000) String frozenReplaySetJson,
    @NotBlank @Size(max = 100000) String holdoutReplaySetJson,
    @NotNull @DecimalMin("0.01") BigDecimal minimumGain,
    @NotNull @DecimalMin("0.00") BigDecimal maximumCostIncreaseRatio) {}
