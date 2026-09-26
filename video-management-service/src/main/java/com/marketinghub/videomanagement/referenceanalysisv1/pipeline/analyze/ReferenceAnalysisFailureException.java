package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/** Preserva a auditoria disponível quando a análise multimodal falha após a inspeção da mídia. */
public class ReferenceAnalysisFailureException extends RuntimeException {
    private final JsonNode artifacts;
    private final JsonNode rawRequest;
    private final JsonNode rawResponse;
    private final String model;
    private final Long inputTokens;
    private final Long cachedInputTokens;
    private final Long outputTokens;
    private final BigDecimal costUsd;

    /** Encapsula a causa com os dados que devem chegar ao callback de falha. */
    public ReferenceAnalysisFailureException(String message,
                                             Throwable cause,
                                             JsonNode artifacts,
                                             JsonNode rawRequest,
                                             JsonNode rawResponse,
                                             String model) {
        this(message, cause, artifacts, rawRequest, rawResponse, model, null, null, null, null);
    }

    /** Encapsula a falha e também o consumo conhecido da tentativa externa. */
    public ReferenceAnalysisFailureException(String message,
                                             Throwable cause,
                                             JsonNode artifacts,
                                             JsonNode rawRequest,
                                             JsonNode rawResponse,
                                             String model,
                                             Long inputTokens,
                                             Long cachedInputTokens,
                                             Long outputTokens,
                                             BigDecimal costUsd) {
        super(message, cause);
        this.artifacts = artifacts;
        this.rawRequest = rawRequest;
        this.rawResponse = rawResponse;
        this.model = model;
        this.inputTokens = inputTokens;
        this.cachedInputTokens = cachedInputTokens;
        this.outputTokens = outputTokens;
        this.costUsd = costUsd;
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

    /** Devolve os tokens de entrada conhecidos mesmo quando não houve saída funcional. */
    public Long inputTokens() {
        return inputTokens;
    }

    /** Devolve a parcela de entrada em cache reportada pelo provedor. */
    public Long cachedInputTokens() {
        return cachedInputTokens;
    }

    /** Devolve os tokens de saída consumidos antes da falha. */
    public Long outputTokens() {
        return outputTokens;
    }

    /** Devolve o custo conservador conhecido da tentativa que falhou. */
    public BigDecimal costUsd() {
        return costUsd;
    }
}
