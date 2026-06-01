package com.marketinghub.epm.service.listFinancialPlans;

import com.marketinghub.epm.service.getFinancialPlan.FinancialPlanResponse;
import java.util.List;

/** Resposta com a lista de planos financeiros do EPM. */
public record FinancialPlanListResponse(List<FinancialPlanResponse> plans) {}
