package com.marketinghub.worker.pipeline.hypothesisresult;

import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da etapa Resultado do pipeline de hipótese. */
public record HypothesisResultInput(
        Long marketNicheId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados de prompt para evitar mapas nulos durante o processamento da etapa. */
    public HypothesisResultInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
