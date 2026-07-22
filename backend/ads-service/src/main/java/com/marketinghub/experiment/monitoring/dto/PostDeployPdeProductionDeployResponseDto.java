package com.marketinghub.experiment.monitoring.dto;

import java.time.Instant;

/** Retorna o resultado da solicitação de deploy de produção do PDE. */
public record PostDeployPdeProductionDeployResponseDto(
        boolean accepted,
        String status,
        String message,
        String targetEnvironment,
        String workflowFile,
        String sourceCommitSha,
        Instant requestedAt
) {}
