package com.marketinghub.experiment.monitoring.dto;

/** Recebe a confirmação administrativa para publicar o PDE em produção. */
public record PostDeployPdeProductionDeployRequestDto(
        String requestedBy,
        String sourceCommitSha
) {}
