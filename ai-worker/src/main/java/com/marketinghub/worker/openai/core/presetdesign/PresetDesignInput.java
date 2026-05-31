package com.marketinghub.worker.openai.core.presetdesign;

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
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
