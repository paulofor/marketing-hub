package com.marketinghub.worker.openai.core.copy;

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
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
