package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.pipeline.StageContext;
import java.util.LinkedHashMap;
import java.util.Map;

/** Responsabilidade: montar o payload operacional da etapa Texto do GeracaoAnuncios v1. */
public class GeraAnuncioTextoPromptBuilder {
    private final ObjectMapper objectMapper;

    /** Recebe o ObjectMapper usado para serializar o payload auditável da etapa. */
    public GeraAnuncioTextoPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Monta um JSON com entrada, artefatos anteriores e metadados mínimos para auditoria. */
    public String buildRequestPayload(StageContext<GeraAnuncioTextoInput> context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stage", "texto");
        payload.put("jobId", context.input().jobId());
        payload.put("stageExecutionId", context.input().stageExecutionId());
        payload.put("experimentId", context.input().experimentId());
        payload.put("context", context.input().context());
        payload.put("previousArtifacts", context.input().previousArtifacts());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload da etapa Texto do GeracaoAnuncios v1", ex);
        }
    }
}
