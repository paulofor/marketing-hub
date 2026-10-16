package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import java.math.BigDecimal;
import java.util.List;

/** Expõe somente custos incrementais estimados e a identidade das tentativas conciliadas. */
public record ReconcileNarrationCostsResponse(
    Long experimentId,
    BigDecimal estimatedCostUsd,
    int newProviderRequests,
    List<Long> reconciledJobIds,
    String evidenceHash,
    boolean providerInvoiceConfirmed) {}
