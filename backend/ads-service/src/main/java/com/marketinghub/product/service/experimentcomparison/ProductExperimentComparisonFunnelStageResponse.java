package com.marketinghub.product.service.experimentcomparison;

/** Contagem agregada de uma etapa do funil de vendas no comparativo do produto. */
public record ProductExperimentComparisonFunnelStageResponse(
    String stageCode, String stageLabel, long total) {}
