package com.marketinghub.nichocnaev3.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Cliente HTTP do executor NichoCNAE v3 para pending, complete e fail no backend. */
@Component
public class NichoCnaeV3BackendClient {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String backendBaseUrl;

    /** Inicializa o cliente com URL base do backend. */
    public NichoCnaeV3BackendClient(RestTemplateBuilder builder, ObjectMapper objectMapper,
            @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
        this.backendBaseUrl = backendBaseUrl;
    }

    /** Lista pendências de uma etapa v3 no endpoint canônico pending. */
    public List<NichoCnaeV3PendingExecution> listPending(NichoCnaeV3StageDefinition stage) {
        Map<String, Object>[] response = restTemplate.getForObject(backendBaseUrl + stage.backendPath() + "/pending", Map[].class);
        return response == null ? List.of() : Arrays.stream(response).map(this::toPending).toList();
    }

    /** Envia conclusão da etapa v3 para o backend. */
    public void complete(NichoCnaeV3StageDefinition stage, NichoCnaeV3PendingExecution pending, Map<String, Object> request) {
        restTemplate.postForObject(backendBaseUrl + stage.backendPath() + "/" + pending.stageExecutionId() + "/complete", request, Object.class);
    }


    /** Envia falha da etapa v3 para o backend. */
    public void fail(NichoCnaeV3StageDefinition stage, NichoCnaeV3PendingExecution pending, RuntimeException ex) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("errorMessage", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        restTemplate.postForObject(backendBaseUrl + stage.backendPath() + "/" + pending.stageExecutionId() + "/fail", request, Object.class);
    }

    /** Serializa payload estruturado para o backend. */
    public String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload NichoCNAE v3.", ex);
        }
    }

    /** Converte JSON textual para mapa de entrada da etapa. */
    public Map<String, Object> parseInput(String inputPayload) {
        if (inputPayload == null || inputPayload.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(inputPayload, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Payload NichoCNAE v3 inválido.", ex);
        }
    }

    /** Converte mapa bruto do backend para contrato interno do executor. */
    private NichoCnaeV3PendingExecution toPending(Map<String, Object> raw) {
        return new NichoCnaeV3PendingExecution(text(raw.get("stageExecutionId")), text(raw.get("jobId")), text(raw.get("cnaeCode")), text(raw.get("inputPayload")), new LinkedHashMap<>(raw));
    }

    /** Converte valor opcional em texto. */
    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
