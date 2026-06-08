package com.marketinghub.geralanding.deliverables.service.pending;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework usados na fila interna da etapa deliverables. */
public record RecordDeliverablesHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
}
