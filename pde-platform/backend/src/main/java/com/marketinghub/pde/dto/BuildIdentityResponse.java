package com.marketinghub.pde.dto;

import java.time.Instant;

/** Publica a identidade auditável da build PDE atualmente implantada. */
public record BuildIdentityResponse(
        String applicationName,
        String artifact,
        String buildVersion,
        String commitSha,
        String imageTag,
        String backendImage,
        String environment,
        String backendUrl,
        String frontendUrl,
        String marketingHubBaseUrl,
        Instant deployedAt
) {}
