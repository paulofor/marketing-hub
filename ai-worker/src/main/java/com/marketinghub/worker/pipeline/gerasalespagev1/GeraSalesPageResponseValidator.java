package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: validar que a resposta da OpenAI é JSON estruturado para o GeraSalesPage v1. */
public class GeraSalesPageResponseValidator {
    private static final Logger log = LoggerFactory.getLogger(GeraSalesPageResponseValidator.class);
    private final ObjectMapper objectMapper;

    /** Inicializa o validador com ObjectMapper para conversão segura. */
    public GeraSalesPageResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Converte a resposta textual em objeto JSON e bloqueia payload vazio. */
    public GeraSalesPageOutput validateAndParse(String modelResponse) {
        try {
            Map<String, Object> payload = objectMapper.readValue(modelResponse, new TypeReference<>() {});
            if (payload.isEmpty()) {
                throw new StageWorkerException("Resposta vazia do GeraSalesPage v1");
            }
            return new GeraSalesPageOutput(payload);
        } catch (Exception ex) {
            log.error("Resposta inválida retornada pela OpenAI no GeraSalesPage v1. modelResponse={}", modelResponse, ex);
            throw new StageWorkerException("Resposta inválida do GeraSalesPage v1", ex);
        }
    }
}
