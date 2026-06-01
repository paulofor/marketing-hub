package com.marketinghub.epm.service.createFinancialPlan;

import com.marketinghub.epm.FinancialPlanStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Dados para criar um plano financeiro do EPM. */
public record CreateFinancialPlanRequest(@NotBlank String name, @NotNull LocalDate cycleStartDate, @NotNull LocalDate cycleEndDate, @NotNull @PositiveOrZero Long totalBudgetCents, Long defaultDailyBudgetCents, Integer defaultExperimentDurationDays, Integer defaultExperimentsPerHypothesis, FinancialPlanStatus status, String notes) {}
