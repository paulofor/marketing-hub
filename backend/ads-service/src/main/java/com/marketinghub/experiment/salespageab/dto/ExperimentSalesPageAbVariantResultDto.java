package com.marketinghub.experiment.salespageab.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor o desempenho rastreado de uma variante do teste A/B. */
public record ExperimentSalesPageAbVariantResultDto(
        ExperimentSalesPageAbVariantDto variant,
        long pageViews,
        long sessions,
        long averageVisibleMsPerSession,
        long checkoutClicks,
        long purchases,
        BigDecimal checkoutClickRate,
        BigDecimal purchaseRate,
        Instant lastEventAt) {
}
