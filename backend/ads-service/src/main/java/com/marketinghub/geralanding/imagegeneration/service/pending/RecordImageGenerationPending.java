package com.marketinghub.geralanding.imagegeneration.service.pending;

/** Representa o item mínimo de pendência da etapa image generation consumido pelo Worker AI. */
public record RecordImageGenerationPending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordImageGenerationExperiment experiment,
        RecordImageGenerationHypothesis hypothesis
) {
}
