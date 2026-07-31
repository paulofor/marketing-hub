package com.marketinghub.experiment.monitoring.pde;

import java.time.Instant;

/** Contrato de identidade de build retornado pelo backend PDE publicado. */
public record PdeBuildIdentity(
    String applicationName,
    String artifact,
    String buildVersion,
    String commitSha,
    String branch,
    String imageTag,
    String backendImage,
    String environment,
    String backendUrl,
    String frontendUrl,
    String marketingHubBaseUrl,
    Instant deployedAt) {}
