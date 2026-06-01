package com.marketinghub.epm.service.createExperimentBudget;

import com.marketinghub.epm.ExperimentBudgetStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Dados para criar um orçamento de experimento do EPM. */
public record CreateExperimentBudgetRequest(Long externalExperimentId, @NotBlank String name, @NotNull @PositiveOrZero Long plannedDailyBudgetCents, @NotNull @Positive Integer plannedDurationDays, Long spendLimitCents, LocalDate startDate, LocalDate endDate, ExperimentBudgetStatus status, String notes) {}
