package com.marketinghub.gerasalespage.v1.service.republish;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Responsabilidade: confirmar a identidade auditada apresentada antes da republicação. */
public record RepublishPublicationRequest(
    @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String expectedSourceSha256) {}
