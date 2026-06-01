package com.marketinghub.epm.service.createExperimentMetric;

import jakarta.validation.constraints.*;
import java.time.Instant;

/** Dados manuais para registrar métricas financeiras de um experimento. */
public record CreateExperimentMetricRequest(Instant measuredAt, Long impressions, Long clicks, Integer visitors, Integer leads, Integer sampleRequests, Integer checkoutClicks, Integer purchases, Long adSpendCents, Long revenueCents, Long paymentFeeCents, Long platformFeeCents, Long aiCostCents, Long taxEstimateCents, String notes) {}
