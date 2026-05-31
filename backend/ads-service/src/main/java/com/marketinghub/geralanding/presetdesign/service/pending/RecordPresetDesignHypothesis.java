package com.marketinghub.geralanding.presetdesign.service.pending;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework usados na fila interna da etapa preset design. */
public record RecordPresetDesignHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
}
