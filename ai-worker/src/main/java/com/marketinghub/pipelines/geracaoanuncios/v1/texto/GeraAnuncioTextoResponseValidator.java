package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import java.util.List;
import java.util.Map;

/** Responsabilidade: validar e normalizar a resposta funcional da etapa Texto do GeracaoAnuncios v1. */
public class GeraAnuncioTextoResponseValidator {
    /** Converte o payload gerado pela etapa em saída estruturada segura para callback ao backend. */
    public GeraAnuncioTextoOutput validateAndParse(String requestPayload) {
        if (requestPayload == null || requestPayload.isBlank()) {
            throw new IllegalArgumentException("Payload da etapa Texto do GeracaoAnuncios v1 não pode ser vazio");
        }
        return new GeraAnuncioTextoOutput(
                List.of(Map.of("type", "TEXTO_REQUEST", "payload", requestPayload)),
                Map.of("status", "TEXT_READY", "source", "geracaoanuncios-v1-texto"));
    }
}
