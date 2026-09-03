package com.marketinghub.financialagent.service;

import java.math.BigDecimal;

/** Responsabilidade: expor a cobertura financeira e editorial agregada de uma rota de provider. */
public record ProviderRouteEfficiencyView(
    String model,
    long taskCount,
    long settledTaskCount,
    BigDecimal knownCostUsd,
    long evaluatedTaskCount,
    BigDecimal utilizationPoints) {}
