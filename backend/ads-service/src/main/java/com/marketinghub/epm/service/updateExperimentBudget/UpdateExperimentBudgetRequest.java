package com.marketinghub.epm.service.updateExperimentBudget;

import com.marketinghub.epm.ExperimentBudgetStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Dados para atualizar um orçamento de experimento do EPM. */
public record UpdateExperimentBudgetRequest(Long externalExperimentId, @NotBlank String name, @NotNull @PositiveOrZero Long plannedDailyBudgetCents, @NotNull @Positive Integer plannedDurationDays, Long spendLimitCents, Long actualSpendCents, LocalDate startDate, LocalDate endDate, ExperimentBudgetStatus status, String notes) {}
