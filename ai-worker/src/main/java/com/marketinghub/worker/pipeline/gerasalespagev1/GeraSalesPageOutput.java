package com.marketinghub.worker.pipeline.gerasalespagev1;

import java.util.Map;

/** Responsabilidade: representar a saída JSON estruturada de uma etapa do GeraSalesPage v1. */
public record GeraSalesPageOutput(Map<String, Object> payload) {
    /** Normaliza o payload para evitar nulos no resultado da etapa. */
    public GeraSalesPageOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
