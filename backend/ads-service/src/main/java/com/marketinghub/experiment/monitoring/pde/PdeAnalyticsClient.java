package com.marketinghub.experiment.monitoring.pde;

/** Cliente responsável por consultar analytics públicos administrativos do PDE. */
public interface PdeAnalyticsClient {

  /** Busca o resumo de analytics do produto PDE informado. */
  PdeAnalyticsSummary fetchSummary(String productSlug);

  /** Busca o resumo de analytics usando a URL pública do slot PDE monitorado. */
  default PdeAnalyticsSummary fetchSummary(String productSlug, String publicBaseUrl) {
    return fetchSummary(productSlug);
  }
}
