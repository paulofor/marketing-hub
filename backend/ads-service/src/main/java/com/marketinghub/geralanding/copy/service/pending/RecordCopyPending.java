package com.marketinghub.geralanding.copy.service.pending;

/** Representa o item mínimo de pendência da etapa copy consumido pelo Worker AI. */
public record RecordCopyPending(
        Long experimentId,
        String jobid,
        String idJob,
        String stageCode,
        String status,
        RecordCopyExperiment experiment,
        RecordCopyHypothesis hypothesis
) {
}
