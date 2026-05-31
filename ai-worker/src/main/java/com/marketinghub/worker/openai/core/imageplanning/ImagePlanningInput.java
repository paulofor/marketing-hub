package com.marketinghub.worker.openai.core.imageplanning;

import java.util.Map;

/** Responsabilidade: transportar os dados de entrada usados no prompt da etapa image planning. */
public record ImagePlanningInput(
        Long experimentId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza o mapa de dados do prompt para evitar valores nulos durante a renderização. */
    public ImagePlanningInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
