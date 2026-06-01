package com.marketinghub.epm.service.getPlanNiche;

import com.marketinghub.epm.FinancialPlanNicheStatus;
import java.time.Instant;

/** Resposta com os dados de um nicho financeiro planejado. */
public record FinancialPlanNicheResponse(Long id, Long financialPlanId, Long externalNicheId, String nicheName, Long plannedBudgetCents, Long spendLimitCents, FinancialPlanNicheStatus status, String notes, Instant createdAt, Instant updatedAt) {}
