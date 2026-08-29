package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Contrato de falha operacional reportada pelo worker de descoberta. */
public record ProductDiscoveryFailureRequest(
    @NotBlank String executionLeaseId,
    @NotBlank String errorMessage,
    @Valid AgentTaskExecutionAuditRequest executionAudit) {

  /** Mantém compatibilidade com falhas ocorridas antes da montagem ou chamada do modelo. */
  public ProductDiscoveryFailureRequest(String executionLeaseId, String errorMessage) {
    this(executionLeaseId, errorMessage, null);
  }
}
