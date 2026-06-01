package com.marketinghub.epm.service.getFinancialPlan;

import com.marketinghub.epm.FinancialPlanStatus;
import java.time.*;

/** Resposta com os dados básicos de um plano financeiro do EPM. */
public record FinancialPlanResponse(Long id, String name, LocalDate cycleStartDate, LocalDate cycleEndDate, Long totalBudgetCents, Long defaultDailyBudgetCents, Integer defaultExperimentDurationDays, Integer defaultExperimentsPerHypothesis, FinancialPlanStatus status, String notes, Instant createdAt, Instant updatedAt) {}
