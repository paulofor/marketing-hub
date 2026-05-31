package com.marketinghub.worker.openai.core.imageplanning;

import java.util.Map;

/** Responsabilidade: transportar o JSON validado produzido pela OpenAI para a etapa image planning. */
public record ImagePlanningOutput(
        Map<String, Object> payload
) {
    /** Normaliza o payload validado para evitar valores nulos no fluxo do worker. */
    public ImagePlanningOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
