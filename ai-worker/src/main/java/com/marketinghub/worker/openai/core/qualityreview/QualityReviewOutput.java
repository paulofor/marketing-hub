package com.marketinghub.worker.openai.core.qualityreview;

import java.util.Map;

/** Responsabilidade: transportar o JSON validado retornado pela OpenAI para a revisão visual. */
public record QualityReviewOutput(Map<String, Object> payload) {
    /** Normaliza o payload de saída para manter contrato imutável e sem mapa nulo. */
    public QualityReviewOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
