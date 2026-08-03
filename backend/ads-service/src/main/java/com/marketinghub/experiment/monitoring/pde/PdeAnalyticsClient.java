package com.marketinghub.experiment.monitoring.pde;

/** Cliente responsável por consultar analytics públicos administrativos do PDE. */
public interface PdeAnalyticsClient {

  /** Busca o resumo de analytics do produto PDE informado. */
  PdeAnalyticsSummary fetchSummary(String productSlug);

  /** Busca o resumo de analytics usando a URL pública do slot PDE monitorado. */
  default PdeAnalyticsSummary fetchSummary(String productSlug, String publicBaseUrl) {
    return fetchSummary(productSlug);
  }

  /** Busca o resumo limitado à versão exata ligada ao experimento. */
  default PdeAnalyticsSummary fetchSummary(
      String productSlug, String publicBaseUrl, String experienceVersion) {
    return fetchSummary(productSlug, publicBaseUrl);
  }

  /** Busca o resumo incluindo tráfego técnico para experimentos fake de diagnóstico. */
  default PdeAnalyticsSummary fetchSummaryIncludingNonHumanTraffic(
      String productSlug, String publicBaseUrl) {
    return fetchSummary(productSlug, publicBaseUrl);
  }

  /** Busca a identidade de build usando a URL pública do PDE monitorado. */
  default PdeBuildIdentity fetchBuildIdentity(String publicBaseUrl) {
    return null;
  }
}
