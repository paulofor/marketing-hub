package com.marketinghub.worker.geralanding.imageplanning;

/** Representa o job pendente da etapa image planning. */
public record GeraLandingStageExecutionImagePlanningDto(
        Long experimentId,
        String idJob,
        String stageCode) {

    /** Cria um DTO da etapa a partir do DTO base do GeraLanding. */
    public static GeraLandingStageExecutionImagePlanningDto fromBase(com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto base) {
        return new GeraLandingStageExecutionImagePlanningDto(base.experimentId(), base.idJob(), base.stageCode());
    }
}
