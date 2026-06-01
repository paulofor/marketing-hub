package com.marketinghub.epm.service.getLatestExperimentMetric;

import java.math.BigDecimal;
import java.time.Instant;

/** Resposta com métricas financeiras calculadas de um experimento. */
public record ExperimentMetricResponse(Long id, Long experimentBudgetId, Instant measuredAt, Long impressions, Long clicks, Integer visitors, Integer leads, Integer sampleRequests, Integer checkoutClicks, Integer purchases, Long adSpendCents, Long revenueCents, Long paymentFeeCents, Long platformFeeCents, Long aiCostCents, Long taxEstimateCents, Long grossProfitCents, Long estimatedNetProfitCents, BigDecimal ctrDecimal, Long cpcCents, Long cplCents, Long cpaCents, BigDecimal roasDecimal, BigDecimal landingConversionDecimal, BigDecimal purchaseConversionDecimal, String notes, Instant createdAt) {}
