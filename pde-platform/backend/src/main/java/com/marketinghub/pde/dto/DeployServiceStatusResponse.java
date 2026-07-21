package com.marketinghub.pde.dto;

/** Resume um serviço declarado na stack Docker do PDE publicada. */
public record DeployServiceStatusResponse(
        String name,
        String containerName,
        String image,
        Integer publicPort,
        Integer targetPort,
        String role
) {}
