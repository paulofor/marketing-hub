package com.marketinghub.experiment.monitoring.pde;

/** Cliente responsável por consultar analytics públicos administrativos do PDE. */
public interface PdeAnalyticsClient {

  /** Busca o resumo de analytics do produto PDE informado. */
  PdeAnalyticsSummary fetchSummary(String productSlug);
}
