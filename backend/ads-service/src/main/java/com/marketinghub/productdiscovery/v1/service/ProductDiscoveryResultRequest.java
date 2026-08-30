package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Contrato de resultado completo de pesquisa enviado pelo worker. */
public record ProductDiscoveryResultRequest(
    @NotBlank String executionLeaseId,
    @NotBlank String decisionSummary,
    @NotNull List<@Valid ProductDiscoveryOpportunityResultRequest> opportunities,
    JsonNode evidenceReport,
    @Valid ProductDiscoveryAnalysisAuditRequest analysisAudit) {

  /** Mantém compatibilidade com callbacks anteriores à síntese factual auditável. */
  public ProductDiscoveryResultRequest(
      String executionLeaseId,
      String decisionSummary,
      List<ProductDiscoveryOpportunityResultRequest> opportunities) {
    this(executionLeaseId, decisionSummary, opportunities, null, null);
  }
}
