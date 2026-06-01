package com.marketinghub.epm.service.listExperimentBudgets;

import com.marketinghub.epm.service.getExperimentBudget.ExperimentBudgetResponse;
import java.util.List;

/** Resposta com os orçamentos de experimento de uma hipótese. */
public record ExperimentBudgetListResponse(List<ExperimentBudgetResponse> experiments) {}
