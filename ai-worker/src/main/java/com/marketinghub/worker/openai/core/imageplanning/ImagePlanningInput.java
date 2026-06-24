package com.marketinghub.worker.openai.core.imageplanning;

import java.util.LinkedHashMap;
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
        promptData = normalizePromptData(promptData);
    }

    /** Copia os dados do prompt trocando valores nulos por texto vazio para preservar placeholders opcionais. */
    private static Map<String, Object> normalizePromptData(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                normalized.put(key, value != null ? value : "");
            }
        });
        return Map.copyOf(normalized);
    }
}
