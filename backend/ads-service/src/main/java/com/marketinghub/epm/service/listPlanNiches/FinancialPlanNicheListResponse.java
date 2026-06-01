package com.marketinghub.epm.service.listPlanNiches;

import com.marketinghub.epm.service.getPlanNiche.FinancialPlanNicheResponse;
import java.util.List;

/** Resposta com os nichos financeiros de um plano. */
public record FinancialPlanNicheListResponse(List<FinancialPlanNicheResponse> niches) {}
