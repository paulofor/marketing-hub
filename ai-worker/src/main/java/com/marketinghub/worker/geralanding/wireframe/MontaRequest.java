package com.marketinghub.worker.geralanding.wireframe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.GeraLandingExperimentRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: montar o request da OpenAI para a etapa de wireframe do GeraLanding.
 */
@Component
public class MontaRequest {

    private static final String DEFAULT_MODEL = "gpt-5.2";
    private static final String DEFAULT_SYSTEM_NAME = "gera-landing-pipeline";
    private static final String DEFAULT_SYSTEM_MESSAGE =
            "Você é um Especialista em Marketing focado em vendas de produtos digitais pela Internet.";

    private final ObjectMapper objectMapper;
    private final Resource schemaResource;

    public MontaRequest(ObjectMapper objectMapper,
                        @Value("classpath:prompts/geralanding/landing-page-wireframe-schema.json") Resource schemaResource) {
        this.objectMapper = objectMapper;
        this.schemaResource = schemaResource;
    }

    /** Monta o payload da Responses API para a etapa, resolvendo internamente schema e parâmetros padrão. */
    public String montar(GeraLandingExperimentRequest experiment) throws JsonProcessingException {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "experiment_pipeline_landing_page_wireframe");
        format.put("schema", carregarSchema());
        format.put("strict", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", experiment.resolveText(null, DEFAULT_MODEL));
        body.put("input", List.of(
                Map.of("role", "system", "content", "[" + DEFAULT_SYSTEM_NAME + "] " + DEFAULT_SYSTEM_MESSAGE),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", experiment.prompt())))
        ));
        body.put("text", Map.of("format", format));
        return objectMapper.writeValueAsString(body);
    }

    /** Carrega e converte para mapa o schema JSON da etapa. */
    private Map<String, Object> carregarSchema() throws JsonProcessingException {
        try {
            return objectMapper.readValue(schemaResource.getInputStream(), Map.class);
        } catch (IOException ex) {
            throw new JsonProcessingException("Falha ao carregar schema da etapa wireframe") {
            };
        }
    }
}
