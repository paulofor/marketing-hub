package com.marketinghub.worker.pipeline.hypothesisoffer;

import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da etapa Oferta do pipeline de hipótese. */
public record HypothesisOfferInput(
        Long marketNicheId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados de prompt para evitar mapas nulos durante o processamento da etapa. */
    public HypothesisOfferInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
