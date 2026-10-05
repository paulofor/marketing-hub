package com.marketinghub.imagegenerator.service.reconcileCost;

import java.math.BigDecimal;

/** Responsabilidade: expor custo estimado e sua fonte sem confundi-lo com fatura confirmada. */
public record ImageGenerationCostView(
    String jobId,
    String status,
    BigDecimal estimatedCostUsd,
    String evidence,
    String pricingSource,
    String pricingCheckedOn) {}
