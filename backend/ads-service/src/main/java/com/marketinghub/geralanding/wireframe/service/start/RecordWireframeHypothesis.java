package com.marketinghub.geralanding.wireframe.service.start;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework usados na fila interna da etapa wireframe. */
public record RecordWireframeHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
}
