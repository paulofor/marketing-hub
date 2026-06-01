package com.marketinghub.epm.service.createPlanHypothesis;

import com.marketinghub.epm.FinancialPlanHypothesisStatus;
import jakarta.validation.constraints.*;

/** Dados para criar uma hipótese financeira dentro de um nicho do EPM. */
public record CreatePlanHypothesisRequest(String externalHypothesisId, @NotBlank String title, @NotNull @Positive Integer plannedExperiments, Long plannedCostPerExperimentCents, Long lossLimitCents, FinancialPlanHypothesisStatus status, String notes) {}
