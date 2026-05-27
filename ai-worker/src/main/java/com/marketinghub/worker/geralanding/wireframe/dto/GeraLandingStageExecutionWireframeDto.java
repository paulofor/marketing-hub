package com.marketinghub.worker.geralanding.wireframe.dto;

/** Representa uma execução pendente da etapa wireframe. */
public record GeraLandingStageExecutionWireframeDto(
        Long experimentId,
        String idJob,
        String stageCode) {
}
