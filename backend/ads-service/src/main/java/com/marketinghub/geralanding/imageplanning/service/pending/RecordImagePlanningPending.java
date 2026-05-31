package com.marketinghub.geralanding.imageplanning.service.pending;

/** Representa o item mínimo de pendência da etapa image planning consumido pelo Worker AI. */
public record RecordImagePlanningPending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordImagePlanningExperiment experiment,
        RecordImagePlanningHypothesis hypothesis
) {
}
