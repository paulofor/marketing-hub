package com.marketinghub.pde.dto;

/** Expõe uma entrega personalizada vinculada ao acesso e ao marco operacional da cliente. */
public record DeliveryArtifactResponse(
        String missionId,
        String title,
        String version,
        String createdAt,
        String content,
        String downloadUrl
) {}
