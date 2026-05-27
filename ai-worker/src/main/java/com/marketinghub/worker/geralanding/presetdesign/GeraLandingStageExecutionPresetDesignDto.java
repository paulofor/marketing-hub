package com.marketinghub.worker.geralanding.presetdesign;

/** Representa uma execução pendente da etapa presetdesign. */
public record GeraLandingStageExecutionPresetDesignDto(
        Long experimentId,
        String idJob,
        String stageCode) {

    /** Converte o DTO específico da etapa para o DTO base do GeraLanding. */
    public com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto toBase() {
        return new com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto(experimentId, idJob, stageCode);
    }

    /** Cria um DTO da etapa a partir do DTO base do GeraLanding. */
    public static GeraLandingStageExecutionPresetDesignDto fromBase(com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto base) {
        return new GeraLandingStageExecutionPresetDesignDto(base.experimentId(), base.idJob(), base.stageCode());
    }
}
