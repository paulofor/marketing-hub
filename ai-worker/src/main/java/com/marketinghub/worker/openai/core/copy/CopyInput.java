package com.marketinghub.worker.openai.core.copy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da etapa copy no core OpenAI. */
public record CopyInput(
        Long experimentId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados do prompt para evitar mapas nulos durante a renderização da etapa copy. */
    public CopyInput {
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
