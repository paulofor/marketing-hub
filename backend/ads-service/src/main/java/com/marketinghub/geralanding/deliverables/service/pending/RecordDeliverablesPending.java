package com.marketinghub.geralanding.deliverables.service.pending;

import java.time.Instant;

/** Representa o item mínimo de pendência da etapa deliverables consumido pelo Worker AI. */
public record RecordDeliverablesPending(
        Long experimentId,
        String jobid,
        String idJob,
        String stageCode,
        String status,
        Instant executionRequestedAt,
        RecordDeliverablesExperiment experiment,
        RecordDeliverablesHypothesis hypothesis
) {
}
