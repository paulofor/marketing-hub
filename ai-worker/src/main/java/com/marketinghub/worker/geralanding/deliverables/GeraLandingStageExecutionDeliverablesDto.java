package com.marketinghub.worker.geralanding.deliverables;

/** Representa uma execução pendente da etapa deliverables. */
public record GeraLandingStageExecutionDeliverablesDto(
        Long experimentId,
        String idJob,
        String stageCode) {

    /** Converte o DTO específico da etapa para o DTO base do GeraLanding. */
    public com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto toBase() {
        return new com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto(experimentId, idJob, stageCode);
    }

    /** Cria um DTO da etapa a partir do DTO base do GeraLanding. */
    public static GeraLandingStageExecutionDeliverablesDto fromBase(com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto base) {
        return new GeraLandingStageExecutionDeliverablesDto(base.experimentId(), base.idJob(), base.stageCode());
    }
}
