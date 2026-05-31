package com.marketinghub.geralanding.imageplanning.service.pending;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework usados na fila interna da etapa image planning. */
public record RecordImagePlanningHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
}
