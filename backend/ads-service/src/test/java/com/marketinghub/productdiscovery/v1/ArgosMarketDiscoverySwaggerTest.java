package com.marketinghub.productdiscovery.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: manter documentados os contratos HTTP da descoberta ampla de Argos. */
class ArgosMarketDiscoverySwaggerTest {
  private static final Path SWAGGER_ROOT = Path.of("../../docs/swagger");

  /** Exige modo, comprador, aprofundamento público, retomada e auditoria no contrato do worker. */
  @Test
  void documentsProductDiscoveryV16() throws Exception {
    String swagger = Files.readString(SWAGGER_ROOT.resolve("product-discovery-swagger.yaml"));

    assertThat(swagger)
        .contains(
            "version: 1.6.0",
            "DISCOVER_MARKETS",
            "VALIDATE_MARKET",
            "marketType",
            "referenceSources",
            "ProductDiscoveryAnalysisAudit",
            "evidenceReport",
            "accessedUrls",
            "/supervised-meta-session",
            "ProductDiscoverySupervisedMetaObservation",
            "ProductDiscoverySupervisedMetaSession",
            "/gap-deepening/interviews",
            "/gap-deepening/public-research/resume",
            "canResumePublicResearch",
            "analysisAudit",
            "/candidate-gap-deepening/stage-executions/pending",
            "ProductDiscoveryCustomerInterviewInput",
            "ProductDiscoveryGapDeepening",
            "maximumPublicQueriesPerAttempt",
            "maximumModelInvocations",
            "maximumSearchCostUsd",
            "REJECTED_REPEATED_RESEARCH_LENS",
            "AGENT_TASK_AUDIT_AFTER_CALLBACK");
  }

  /** Exige que o catálogo genérico exponha e documente opções seletivas do backend. */
  @Test
  void documentsIndependentSelectFields() throws Exception {
    String swagger = Files.readString(SWAGGER_ROOT.resolve("business-processes-swagger.yaml"));

    assertThat(swagger)
        .contains(
            "version: 1.12.0",
            "enum: [TEXT, TEXTAREA, SELECT]",
            "IndependentBusinessProcessInputOption",
            "Opções canônicas exigidas quando controlType for SELECT");
  }

  /** Exige a causa e os números da cobertura Meta em cada tentativa de ampliação. */
  @Test
  void documentsMetaCoverageByMarketExpansionAttempt() throws Exception {
    String swagger = Files.readString(SWAGGER_ROOT.resolve("business-processes-swagger.yaml"));

    assertThat(swagger)
        .contains(
            "metaQuery",
            "metaCoverageStatus",
            "metaCollectionMode",
            "metaAdsObserved",
            "metaAdvertisersObserved",
            "metaCoverageSummary",
            "metaSearchUrl",
            "OBSERVED_EMPTY",
            "AWAITING_OBSERVATION",
            "UNAVAILABLE");
  }
}
