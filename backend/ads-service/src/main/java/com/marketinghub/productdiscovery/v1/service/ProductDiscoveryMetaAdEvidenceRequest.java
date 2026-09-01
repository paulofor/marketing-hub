package com.marketinghub.productdiscovery.v1.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Solicita evidência Meta correlacionada à execução vigente de descoberta PDE. */
public record ProductDiscoveryMetaAdEvidenceRequest(
    @NotBlank String executionLeaseId,
    @NotNull @Min(1) @Max(3) Integer attemptNumber,
    @NotBlank @Size(max = 60) String query,
    @Pattern(regexp = "(?i)[A-Z]{2}") String country,
    @Pattern(
            regexp = "(?i)INSTAGRAM",
            message = "a descoberta B2C v1 aceita somente evidências do Instagram")
        String publisherPlatform,
    @Min(1) @Max(50) Integer limit) {}
