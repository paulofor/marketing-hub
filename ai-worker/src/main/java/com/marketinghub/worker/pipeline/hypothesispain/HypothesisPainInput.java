package com.marketinghub.worker.pipeline.hypothesispain;

import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da etapa Dor do pipeline de hipótese. */
public record HypothesisPainInput(
        Long marketNicheId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados de prompt para evitar mapas nulos durante o processamento da etapa. */
    public HypothesisPainInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
