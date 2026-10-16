package com.marketinghub.financialagent.service.narrationCost;

import java.math.BigDecimal;

/** Expõe o custo de uma tentativa sem permitir acesso externo à entidade financeira. */
public record NarrationCostView(
    Long productId,
    Long experimentId,
    String assetType,
    BigDecimal providerCostUsd,
    BigDecimal estimatedCostUsd,
    String costEvidence) {}
