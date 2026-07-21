package com.marketinghub.experiment.monitoring.dto;

import java.time.Instant;
import java.util.List;

/** Resume o status de deploy de um ambiente PDE no painel pós-deploy. */
public record PostDeployPdeDeployEnvironmentDto(
        String environment,
        boolean available,
        String status,
        String errorMessage,
        String composeFile,
        String commitSha,
        String imageTag,
        String experienceVersion,
        String frontendUrl,
        String backendUrl,
        boolean frontendReachable,
        boolean backendReachable,
        Instant deployedAt,
        List<PostDeployPdeDeployServiceDto> services
) {}
