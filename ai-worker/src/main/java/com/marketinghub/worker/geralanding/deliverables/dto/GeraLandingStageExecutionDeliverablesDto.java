package com.marketinghub.worker.geralanding.deliverables.dto;

/** Representa o job pendente da etapa deliverables. */
public record GeraLandingStageExecutionDeliverablesDto(
        Long experimentId,
        String idJob,
        String stageCode) {

    /** Cria um DTO da etapa a partir do DTO base do GeraLanding. */
    public static GeraLandingStageExecutionDeliverablesDto fromBase(com.marketinghub.worker.geralanding.copy.dto.GeraLandingStageExecutionDto base) {
        return new GeraLandingStageExecutionDeliverablesDto(base.experimentId(), base.idJob(), base.stageCode());
    }
}
