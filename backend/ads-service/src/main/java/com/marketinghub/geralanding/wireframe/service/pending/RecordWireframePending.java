package com.marketinghub.geralanding.wireframe.service.pending;

/** Representa o item mínimo de pendência da etapa wireframe consumido pelo Worker AI. */
public record RecordWireframePending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordWireframeExperiment experiment,
        RecordWireframeHypothesis hypothesis
) {
}
