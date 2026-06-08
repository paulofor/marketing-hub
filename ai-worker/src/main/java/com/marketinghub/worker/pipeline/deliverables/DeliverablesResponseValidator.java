package com.marketinghub.worker.pipeline.deliverables;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para o contrato da etapa deliverables. */
public class DeliverablesResponseValidator {
    private final ObjectMapper objectMapper;

    /** Inicializa o validador com ObjectMapper para parse seguro do JSON da etapa. */
    public DeliverablesResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida que a resposta tem listas não vazias para amostra e produto final. */
    public DeliverablesOutput validateAndParse(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            throw new IllegalArgumentException("Deliverables model response is blank");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(modelResponse, new TypeReference<Map<String, Object>>() {});
            requireNonEmptyList(payload, "sampleDeliverables");
            requireNonEmptyList(payload, "finalProductDeliverables");
            return new DeliverablesOutput(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Deliverables model response is not valid JSON with sampleDeliverables and finalProductDeliverables", ex);
        }
    }

    /** Garante que o campo informado existe e possui ao menos um item. */
    private void requireNonEmptyList(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("Deliverables field is missing or empty: " + field);
        }
    }
}
