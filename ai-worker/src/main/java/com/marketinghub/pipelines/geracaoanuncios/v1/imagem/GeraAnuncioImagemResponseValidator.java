package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import java.util.List;
import java.util.Map;

/** Responsabilidade: validar e normalizar a resposta funcional da etapa Imagem do GeracaoAnuncios v1. */
public class GeraAnuncioImagemResponseValidator {
    /** Converte o payload gerado pela etapa em saída estruturada segura para callback ao backend. */
    public GeraAnuncioImagemOutput validateAndParse(String requestPayload) {
        if (requestPayload == null || requestPayload.isBlank()) {
            throw new IllegalArgumentException("Payload da etapa Imagem do GeracaoAnuncios v1 não pode ser vazio");
        }
        return new GeraAnuncioImagemOutput(
                List.of(Map.of("type", "IMAGEM_REQUEST", "payload", requestPayload)),
                Map.of("status", "IMAGE_READY", "source", "geracaoanuncios-v1-imagem"));
    }
}
