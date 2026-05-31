package com.marketinghub.geralanding.presetdesign.service.pending;

/** Representa o item mínimo de pendência da etapa presetdesign consumido pelo Worker AI. */
public record RecordPresetDesignPending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordPresetDesignExperiment experiment,
        RecordPresetDesignHypothesis hypothesis
) {
}
