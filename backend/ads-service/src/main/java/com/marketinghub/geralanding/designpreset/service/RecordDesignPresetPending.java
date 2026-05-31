package com.marketinghub.geralanding.designpreset.service;

/** Representa o item mínimo de pendência da etapa design preset consumido pelo Worker AI. */
public record RecordDesignPresetPending(
        Long experimentId,
        String jobid,
        String idJob,
        String stageCode,
        String status,
        RecordDesignPresetExperiment experiment,
        RecordDesignPresetHypothesis hypothesis
) {
}
