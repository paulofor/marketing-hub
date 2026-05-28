package com.marketinghub.geralanding.wireframe.service;

/** Representa o item mínimo de pendência da etapa wireframe consumido pelo Worker AI. */
public record GeraLandingWireframePendingExecutionResponse(
        Long experimentId,
        String idJob,
        String stageCode
) {
}
