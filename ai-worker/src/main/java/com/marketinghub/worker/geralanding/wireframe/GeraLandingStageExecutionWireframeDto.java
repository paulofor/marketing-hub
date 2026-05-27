package com.marketinghub.worker.geralanding.wireframe;

/** Representa uma execução pendente da etapa wireframe. */
public record GeraLandingStageExecutionWireframeDto(
        Long experimentId,
        String idJob,
        String stageCode) {
}
