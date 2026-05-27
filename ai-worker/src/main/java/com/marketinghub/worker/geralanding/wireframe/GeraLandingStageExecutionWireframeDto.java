package com.marketinghub.worker.geralanding.wireframe;

/** Representa uma execução pendente da etapa wireframe. */
public record GeraLandingStageExecutionWireframeDto(
        Long experimentId,
        String idJob,
        String stageCode) {

    /** Converte o DTO específico de wireframe para o DTO base do GeraLanding. */
    public com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto toBase() {
        return new com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto(experimentId, idJob, stageCode);
    }

    /** Cria um DTO wireframe a partir do DTO base do GeraLanding. */
    public static GeraLandingStageExecutionWireframeDto fromBase(
            com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto base) {
        return new GeraLandingStageExecutionWireframeDto(base.experimentId(), base.idJob(), base.stageCode());
    }
}
