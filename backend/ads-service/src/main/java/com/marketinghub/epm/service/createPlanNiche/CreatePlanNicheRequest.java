package com.marketinghub.epm.service.createPlanNiche;

import com.marketinghub.epm.FinancialPlanNicheStatus;
import jakarta.validation.constraints.*;

/** Dados para criar um nicho financeiro dentro de um plano do EPM. */
public record CreatePlanNicheRequest(Long externalNicheId, @NotBlank String nicheName, @NotNull @PositiveOrZero Long plannedBudgetCents, Long spendLimitCents, FinancialPlanNicheStatus status, String notes) {}
