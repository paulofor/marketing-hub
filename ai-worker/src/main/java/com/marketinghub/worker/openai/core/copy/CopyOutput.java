package com.marketinghub.worker.openai.core.copy;

import java.util.Map;

/** Responsabilidade: transportar o JSON validado retornado pela OpenAI para a etapa copy. */
public record CopyOutput(
        Map<String, Object> payload
) {
    /** Normaliza o payload de saída para manter contrato imutável e sem mapa nulo. */
    public CopyOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
