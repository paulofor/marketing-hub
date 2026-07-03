package com.marketinghub.productaiworker.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.productaiworker.config.ProductAiWorkerProperties;
import com.marketinghub.productaiworker.core.StageContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir pendências e enviar callbacks ao backend principal. */
@Component
public class ProductAiBackendClient {
    private static final String BASE_PATH =
            "/api/internal/product-ai/personalizedsample/v1/paid-delivery/stage-executions";

    private final RestClient restClient;
    private final ProductAiWorkerProperties properties;
    private final ObjectMapper objectMapper;

    /** Inicializa o cliente HTTP com propriedades do backend. */
    public ProductAiBackendClient(RestClient.Builder builder, ProductAiWorkerProperties properties, ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl(properties.getBackendBaseUrl()).build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Lista entregas pagas pendentes pelo endpoint canônico pending. */
    public List<StageContext> pending() {
        StageContext[] response = restClient.get()
                .uri(BASE_PATH + "/pending")
                .retrieve()
                .body(StageContext[].class);
        if (response == null) {
            return List.of();
        }
        return Arrays.stream(response).limit(Math.max(1, properties.getPendingLimit())).toList();
    }

    /** Registra o request bruto enviado à OpenAI. */
    public void receiveRequest(String idJob, Map<String, Object> payload) {
        restClient.post()
                .uri(BASE_PATH + "/{idJob}/recebeRequest", idJob)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    /** Registra resposta ou erro da etapa. */
    public void receiveResponse(String idJob, Map<String, Object> payload) {
        restClient.post()
                .uri(BASE_PATH + "/{idJob}/recebeResponse", idJob)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    /** Converte objeto em JSON para auditoria de payload bruto. */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar payload do Product AI Worker", ex);
        }
    }
}
