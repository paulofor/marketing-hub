package com.marketinghub.epm.service.listProductPriceScenarios;

import java.util.List;

/** Resposta com os cenários de preço de um plano financeiro. */
public record ProductPriceScenarioListResponse(List<ProductPriceScenarioResponse> scenarios) {}
