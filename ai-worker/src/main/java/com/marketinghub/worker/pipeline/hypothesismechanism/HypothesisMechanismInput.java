package com.marketinghub.worker.pipeline.hypothesismechanism;

import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da etapa Mecanismo do pipeline de hipótese. */
public record HypothesisMechanismInput(
        Long marketNicheId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados de prompt para evitar mapas nulos durante o processamento da etapa. */
    public HypothesisMechanismInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
