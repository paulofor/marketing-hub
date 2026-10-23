package com.marketinghub.product.bigidea.v1.service.research;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/** Mostra o estado canônico da pesquisa e a proposta preservada para adoção explícita. */
public record BigIdeaResearchView(
    Long executionId,
    Long productId,
    String status,
    boolean canRequest,
    boolean canAdopt,
    String guidance,
    JsonNode proposal,
    BigDecimal estimatedCostUsd,
    String modelName) {}
