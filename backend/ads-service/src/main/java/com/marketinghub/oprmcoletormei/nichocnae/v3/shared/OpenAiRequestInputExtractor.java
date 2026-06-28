package com.marketinghub.oprmcoletormei.nichocnae.v3.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/** Extrai o campo input de requests brutos da OpenAI para persistência limpa no NichoCNAE v3. */
final class OpenAiRequestInputExtractor {
    private final ObjectMapper objectMapper;

    /** Inicializa o extrator com o mapper JSON compartilhado pelo service da etapa. */
    OpenAiRequestInputExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Retorna o campo input do request da OpenAI quando existir ou mantém nulo quando não houver input. */
    String extract(String rawRequest) {
        if (!StringUtils.hasText(rawRequest)) {
            return null;
        }
        try {
            JsonNode input = objectMapper.readTree(rawRequest).path("input");
            if (input.isTextual() && StringUtils.hasText(input.asText())) {
                return input.asText();
            }
            if (!input.isMissingNode() && !input.isNull()) {
                return objectMapper.writeValueAsString(input);
            }
            return null;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
