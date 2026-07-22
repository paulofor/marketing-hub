package com.marketinghub.experiment.monitoring.dto;

/** Resume a decisão operacional para promover um PDE de homologação para produção. */
public record PostDeployPdePromotionControlDto(
        boolean homologAvailable,
        boolean productionAvailable,
        boolean productionBehind,
        boolean productionUpToDate,
        boolean productionDeployAvailable,
        boolean productionDeployBlocked,
        String statusLabel,
        String recommendation,
        String sourceCommitSha,
        String productionCommitSha,
        String targetEnvironment,
        String workflowFile
) {}
