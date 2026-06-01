package com.marketinghub.epm.service.updateFinancialPlan;

import com.marketinghub.epm.FinancialPlanStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Dados para atualizar um plano financeiro do EPM. */
public record UpdateFinancialPlanRequest(@NotBlank String name, @NotNull LocalDate cycleStartDate, @NotNull LocalDate cycleEndDate, @NotNull @PositiveOrZero Long totalBudgetCents, Long defaultDailyBudgetCents, Integer defaultExperimentDurationDays, Integer defaultExperimentsPerHypothesis, FinancialPlanStatus status, String notes) {}
