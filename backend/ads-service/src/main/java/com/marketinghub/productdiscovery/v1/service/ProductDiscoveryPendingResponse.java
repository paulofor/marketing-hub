package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryResearchMode;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceSelectionResponse;
import java.util.List;

/** Contrato de pendência que o worker consome para pesquisar oportunidades PDE. */
public record ProductDiscoveryPendingResponse(
    Long cycleId,
    String pipelineCode,
    String stageCode,
    String theme,
    String targetAudience,
    String country,
    String language,
    String acquisitionChannel,
    String commercialConstraints,
    String forbiddenCategories,
    String objective,
    ProductDiscoveryResearchMode researchMode,
    ProductDiscoveryMarketType marketType,
    String referenceSources,
    String executionLeaseId,
    int executionAttempt,
    ResearchIntelligenceSelectionResponse researchIntelligence,
    List<ProductDiscoveryOpportunityResponse> previousCandidates,
    String previousEvidenceReportJson,
    List<ProductDiscoveryCustomerInterviewResponse> customerInterviews,
    ProductDiscoveryGapResearchPolicyResponse gapResearchPolicy,
    String evidencePolicy,
    ProductDiscoverySupervisedMetaReanalysisContext supervisedMetaReanalysis) {

  /** Mantém compatibilidade dos produtores e testes da etapa inicial sem contexto de lacunas. */
  public ProductDiscoveryPendingResponse(
      Long cycleId,
      String pipelineCode,
      String stageCode,
      String theme,
      String targetAudience,
      String country,
      String language,
      String acquisitionChannel,
      String commercialConstraints,
      String forbiddenCategories,
      String objective,
      ProductDiscoveryResearchMode researchMode,
      ProductDiscoveryMarketType marketType,
      String referenceSources,
      String executionLeaseId,
      int executionAttempt,
      ResearchIntelligenceSelectionResponse researchIntelligence) {
    this(
        cycleId,
        pipelineCode,
        stageCode,
        theme,
        targetAudience,
        country,
        language,
        acquisitionChannel,
        commercialConstraints,
        forbiddenCategories,
        objective,
        researchMode,
        marketType,
        referenceSources,
        executionLeaseId,
        executionAttempt,
        researchIntelligence,
        List.of(),
        null,
        List.of(),
        null,
        "CONSENTED_INTERVIEWS_V1",
        null);
  }
}
