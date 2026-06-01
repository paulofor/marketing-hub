package com.marketinghub.epm.service.listExperimentDecisions;

import com.marketinghub.epm.ExperimentFinancialDecisionType;
import java.time.Instant;

/** Resposta com uma decisão financeira registrada para um experimento. */
public record ExperimentDecisionResponse(Long id, Long experimentBudgetId, ExperimentFinancialDecisionType decisionType, String reason, Instant decidedAt, String decidedBy, Instant createdAt) {}
