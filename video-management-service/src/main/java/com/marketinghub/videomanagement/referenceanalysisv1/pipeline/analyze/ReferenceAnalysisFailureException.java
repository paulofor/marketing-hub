package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import com.fasterxml.jackson.databind.JsonNode;

/** Preserva a auditoria disponível quando a análise multimodal falha após a inspeção da mídia. */
public class ReferenceAnalysisFailureException extends RuntimeException {
    private final JsonNode artifacts;
    private final JsonNode rawRequest;
    private final JsonNode rawResponse;
    private final String model;

    /** Encapsula a causa com os dados que devem chegar ao callback de falha. */
    public ReferenceAnalysisFailureException(String message,
                                             Throwable cause,
                                             JsonNode artifacts,
                                             JsonNode rawRequest,
                                             JsonNode rawResponse,
                                             String model) {
        super(message, cause);
        this.artifacts = artifacts;
        this.rawRequest = rawRequest;
        this.rawResponse = rawResponse;
        this.model = model;
    }

    /** Devolve as evidências técnicas extraídas antes da falha. */
    public JsonNode artifacts() {
        return artifacts;
    }

    /** Devolve o request bruto enviado ao modelo, quando disponível. */
    public JsonNode rawRequest() {
        return rawRequest;
    }

    /** Devolve a resposta de erro bruta do modelo, quando disponível. */
    public JsonNode rawResponse() {
        return rawResponse;
    }

    /** Identifica o modelo configurado na tentativa que falhou. */
    public String model() {
        return model;
    }
}
