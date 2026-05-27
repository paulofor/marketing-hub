package com.marketinghub.worker.geralanding.imageplanning;

/** Representa uma execução pendente da etapa imageplanning. */
public record GeraLandingStageExecutionImagePlanningDto(
        Long experimentId,
        String idJob,
        String stageCode) {

    /** Converte o DTO específico da etapa para o DTO base do GeraLanding. */
    public com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto toBase() {
        return new com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto(experimentId, idJob, stageCode);
    }

    /** Cria um DTO da etapa a partir do DTO base do GeraLanding. */
    public static GeraLandingStageExecutionImagePlanningDto fromBase(com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto base) {
        return new GeraLandingStageExecutionImagePlanningDto(base.experimentId(), base.idJob(), base.stageCode());
    }
}
