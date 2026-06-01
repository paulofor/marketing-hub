package com.marketinghub.epm.service.getExperimentBudget;

import com.marketinghub.epm.ExperimentBudgetStatus;
import java.time.*;

/** Resposta com os dados de um orçamento de experimento do EPM. */
public record ExperimentBudgetResponse(Long id, Long financialPlanHypothesisId, Long externalExperimentId, String name, Long plannedDailyBudgetCents, Integer plannedDurationDays, Long plannedTotalBudgetCents, Long spendLimitCents, Long actualSpendCents, Long remainingBudgetCents, LocalDate startDate, LocalDate endDate, ExperimentBudgetStatus status, String notes, Instant createdAt, Instant updatedAt) {}
