package com.marketinghub.worker.pipeline.deliverables;

import java.util.Map;

/** Responsabilidade: transportar o JSON validado com entregáveis da amostra e do produto final. */
public record DeliverablesOutput(Map<String, Object> payload) {
    /** Normaliza o payload para manter contrato imutável e não nulo. */
    public DeliverablesOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
