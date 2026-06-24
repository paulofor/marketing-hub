package com.marketinghub.worker.openai.core.presetdesign;

import java.util.LinkedHashMap;
import java.util.Map;

/** Responsabilidade: representar os dados de entrada e placeholders da etapa presetdesign. */
public record PresetDesignInput(
        Long experimentId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza mapas nulos para manter o contrato interno da etapa seguro. */
    public PresetDesignInput {
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
