package com.marketinghub.worker.pipeline.hypothesisproof;

import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da etapa Prova do pipeline de hipótese. */
public record HypothesisProofInput(
        Long marketNicheId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados de prompt para evitar mapas nulos durante o processamento da etapa. */
    public HypothesisProofInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
