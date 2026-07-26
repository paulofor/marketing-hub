package com.marketinghub.productdiscovery.v1.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Contrato de resultado completo de pesquisa enviado pelo worker. */
public record ProductDiscoveryResultRequest(
        @NotBlank String decisionSummary,
        @NotEmpty List<@Valid ProductDiscoveryOpportunityResultRequest> opportunities) {}
