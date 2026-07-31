package com.marketinghub.experiment.monitoring.dto;

import java.time.Instant;

/** Identifica a build PDE consultada pelo cockpit pós-deploy. */
public record PostDeployPdeBuildIdentityDto(
    boolean available,
    String status,
    String errorMessage,
    String requestedBaseUrl,
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
    Instant deployedAt) {}
