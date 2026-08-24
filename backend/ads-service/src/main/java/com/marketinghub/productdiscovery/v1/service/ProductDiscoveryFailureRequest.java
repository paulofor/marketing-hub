package com.marketinghub.productdiscovery.v1.service;

import jakarta.validation.constraints.NotBlank;

/** Contrato de falha operacional reportada pelo worker de descoberta. */
public record ProductDiscoveryFailureRequest(
    @NotBlank String executionLeaseId, @NotBlank String errorMessage) {}
