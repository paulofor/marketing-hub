package com.marketinghub.geralanding.designpreset.service;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework usados na fila interna da etapa design preset. */
public record RecordDesignPresetHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
}
