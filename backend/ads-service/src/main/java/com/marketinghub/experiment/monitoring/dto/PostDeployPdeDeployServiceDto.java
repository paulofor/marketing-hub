package com.marketinghub.experiment.monitoring.dto;

/** Resume um container declarado no deploy PDE monitorado. */
public record PostDeployPdeDeployServiceDto(
        String name,
        String containerName,
        String image,
        Integer publicPort,
        Integer targetPort,
        String role
) {}
