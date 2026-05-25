package com.marketinghub.worker.geralanding.copy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: montar o request da OpenAI para a etapa de copy do GeraLanding.
 */
@Component
public class MontaRequest {

    private final ObjectMapper objectMapper;

    public MontaRequest(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Monta o payload da Responses API para a etapa de copy. */
    public String montar(String model, String prompt, String systemName, String systemMessage, Map<String, Object> schema)
            throws JsonProcessingException {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "experiment_pipeline_landing_page_copy");
        format.put("schema", schema);
        format.put("strict", true);

        String resolvedModel = StringUtils.hasText(model) ? model.trim() : "gpt-5.2";
        String resolvedSystemName = StringUtils.hasText(systemName) ? systemName.trim() : "system";
        String resolvedSystemMessage = StringUtils.hasText(systemMessage)
                ? systemMessage.trim()
                : "Você é um Especialista em Marketing focado em vendas de produtos digitais pela Internet.";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", resolvedModel);
        body.put("input", List.of(
                Map.of("role", "system", "content", "[" + resolvedSystemName + "] " + resolvedSystemMessage),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", prompt)))
        ));
        body.put("text", Map.of("format", format));
        return objectMapper.writeValueAsString(body);
    }
}
