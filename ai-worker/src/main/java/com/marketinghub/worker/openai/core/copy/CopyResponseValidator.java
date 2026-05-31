package com.marketinghub.worker.openai.core.copy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;

import java.util.Map;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para o contrato da etapa copy. */
public class CopyResponseValidator implements StageResponseValidator<CopyOutput> {

    private final ObjectMapper objectMapper;

    /** Inicializa o validador com ObjectMapper para parse seguro do JSON da etapa copy. */
    public CopyResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida que a resposta do modelo não está vazia e contém JSON aderente ao contrato base. */
    @Override
    public CopyOutput validateAndParse(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            throw new InvalidModelResponseException("Copy model response is blank");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    modelResponse,
                    new TypeReference<Map<String, Object>>() {}
            );
            return new CopyOutput(payload);
        } catch (Exception error) {
            throw new InvalidModelResponseException("Copy model response is not valid JSON", error);
        }
    }
}
