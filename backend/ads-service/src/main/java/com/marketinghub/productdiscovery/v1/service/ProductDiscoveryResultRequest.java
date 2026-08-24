package com.marketinghub.productdiscovery.v1.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Contrato de resultado completo de pesquisa enviado pelo worker. */
public record ProductDiscoveryResultRequest(
    @NotBlank String executionLeaseId,
    @NotBlank String decisionSummary,
    @NotNull List<@Valid ProductDiscoveryOpportunityResultRequest> opportunities) {}
