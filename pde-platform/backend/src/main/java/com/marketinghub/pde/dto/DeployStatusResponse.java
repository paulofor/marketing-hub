package com.marketinghub.pde.dto;

import java.time.Instant;
import java.util.List;

/** Retorna o manifesto operacional do deploy PDE carregado pelo ambiente publicado. */
public record DeployStatusResponse(
        String environment,
        String composeFile,
        String commitSha,
        String imageTag,
        String experienceVersion,
        String frontendUrl,
        String backendUrl,
        Instant deployedAt,
        List<DeployServiceStatusResponse> services,
        DeploySchemaStatusResponse schemaStatus,
        List<DeployOperationalAlertResponse> operationalAlerts
) {}
