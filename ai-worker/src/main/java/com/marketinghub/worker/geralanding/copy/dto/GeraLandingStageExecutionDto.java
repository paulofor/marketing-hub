package com.marketinghub.worker.geralanding.copy.dto;

/** Responsabilidade: representar a execução pendente da etapa de copy no GeraLanding. */
public record GeraLandingStageExecutionDto(
        Long experimentId,
        String idJob,
        String stageCode
) {
}
