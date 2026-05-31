package com.marketinghub.geralanding.copy.service.pending;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework usados na fila interna da etapa copy. */
public record RecordCopyHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
}
