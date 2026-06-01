package com.marketinghub.epm.service.listPlanHypotheses;

import com.marketinghub.epm.service.getPlanHypothesis.FinancialPlanHypothesisResponse;
import java.util.List;

/** Resposta com as hipóteses financeiras de um nicho. */
public record FinancialPlanHypothesisListResponse(List<FinancialPlanHypothesisResponse> hypotheses) {}
