package com.marketinghub.epm.service.getPlanHypothesis;

import com.marketinghub.epm.FinancialPlanHypothesisStatus;
import java.time.Instant;

/** Resposta com os dados de uma hipótese financeira planejada. */
public record FinancialPlanHypothesisResponse(Long id, Long financialPlanNicheId, String externalHypothesisId, String title, Integer plannedExperiments, Long plannedCostPerExperimentCents, Long lossLimitCents, FinancialPlanHypothesisStatus status, String notes, Instant createdAt, Instant updatedAt) {}
