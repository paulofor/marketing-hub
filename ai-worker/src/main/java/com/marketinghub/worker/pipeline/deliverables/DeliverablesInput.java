package com.marketinghub.worker.pipeline.deliverables;

import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da etapa landing-page-deliverables. */
public record DeliverablesInput(
        Long experimentId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados de prompt para evitar mapas nulos durante o processamento da etapa. */
    public DeliverablesInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
