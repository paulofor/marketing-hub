package com.marketinghub.worker.geralanding.presetdesign;

/** Representa o job pendente da etapa preset design. */
public record GeraLandingStageExecutionPresetDesignDto(
        Long experimentId,
        String idJob,
        String stageCode) {

    /** Cria um DTO da etapa a partir do DTO base do GeraLanding. */
    public static GeraLandingStageExecutionPresetDesignDto fromBase(com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto base) {
        return new GeraLandingStageExecutionPresetDesignDto(base.experimentId(), base.idJob(), base.stageCode());
    }
}
