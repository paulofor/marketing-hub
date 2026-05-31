package com.marketinghub.geralanding.imagegeneration.service.pending;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework usados na fila interna da etapa image generation. */
public record RecordImageGenerationHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
}
