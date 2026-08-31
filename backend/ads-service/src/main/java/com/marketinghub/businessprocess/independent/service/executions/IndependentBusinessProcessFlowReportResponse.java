package com.marketinghub.businessprocess.independent.service.executions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contrato de negócio que torna visível a cadeia iniciada por um processo independente. */
public record IndependentBusinessProcessFlowReportResponse(
    String reportType,
    String status,
    String headline,
    String acquisitionChannel,
    int candidateCount,
    int dossierReadyCount,
    int plannedProductCount,
    List<SourceCoverage> sourceCoverage,
    List<Candidate> candidates) {

  /** Resume a cobertura factual de uma origem pesquisada por Argos. */
  public record SourceCoverage(
      String sourceCode, String label, String status, int itemCount, String summary) {}

  /** Expõe uma candidata factual e toda sua linhagem até o produto. */
  public record Candidate(
      Long opportunityId,
      String name,
      String primaryAudience,
      String rootPain,
      BigDecimal score,
      String maturity,
      String decision,
      String purchaseSituation,
      List<String> observedLanguage,
      List<String> currentAlternatives,
      String residualEffort,
      String instagramFitEvidence,
      String commercialRisk,
      Long dossierId,
      String dossierStatus,
      Long commercialPlanId,
      Long productId,
      String productName,
      String productStatus,
      String nextAction,
      List<Source> sources,
      List<Stage> stages) {}

  /** Representa uma fonte clicável, classificada sem converter sinal em venda. */
  public record Source(String sourceType, String title, String url, String evidence) {}

  /** Resume um gate persistido da cadeia Argos, Atena, Plutus, Dédalo e produto. */
  public record Stage(
      String stageCode,
      String label,
      String agent,
      String status,
      String decision,
      String summary,
      Long taskId,
      BigDecimal estimatedCostUsd,
      String blocker,
      Instant updatedAt) {}
}
