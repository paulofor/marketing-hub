package com.marketinghub.worker.geralanding.wireframe.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.comum.MontaRequestSupport;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: montar o request da OpenAI para a etapa de wireframe do GeraLanding.
 */
@Component("geraLandingWireframeMontaRequest")
public class MontaRequest {

    private static final String DEFAULT_MODEL = "gpt-5.2";
    private static final String DEFAULT_SYSTEM_NAME = "gera-landing-pipeline";
    private static final String DEFAULT_SYSTEM_MESSAGE =
            "Você é um Especialista em Marketing focado em vendas de produtos digitais pela Internet.";
    private static final String SCHEMA_RESOURCE_PATH = "prompts/geralanding/landing-page-wireframe-schema.json";
    private static final String SCHEMA_NAME = "experiment_pipeline_landing_page_wireframe";
    private static final String PROMPT_MARKDOWN_FILE = "landing-page-wireframe.md";

    private final ObjectMapper objectMapper;

    public MontaRequest(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Monta o payload da Responses API para a etapa, resolvendo internamente schema, markdown e placeholders. */
    public String montar(GeraLandingExperimentWireframeRequest experiment) throws IOException {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", SCHEMA_NAME);
        format.put("schema", MontaRequestSupport.carregarSchema(objectMapper, SCHEMA_RESOURCE_PATH));
        format.put("strict", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", experiment.resolveText(null, DEFAULT_MODEL));
        body.put("input", List.of(
                Map.of("role", "system", "content", "[" + DEFAULT_SYSTEM_NAME + "] " + DEFAULT_SYSTEM_MESSAGE),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", montarPrompt(experiment))))
        ));
        body.put("text", Map.of("format", format));
        return objectMapper.writeValueAsString(body);
    }

    /** Retorna o nome do arquivo markdown da etapa. */
    public String nomePromptMarkdown() {
        return PROMPT_MARKDOWN_FILE;
    }

    /** Retorna o nome do arquivo schema json da etapa. */
    public String nomeSchemaJson() {
        return SCHEMA_RESOURCE_PATH.substring(SCHEMA_RESOURCE_PATH.lastIndexOf('/') + 1);
    }

    /** Carrega o markdown bruto da etapa usando o arquivo padrão declarado no montador. */
    public String carregarPromptMarkdownCru() throws IOException {
        return MontaRequestSupport.carregarPromptMarkdownCru(PROMPT_MARKDOWN_FILE);
    }

    /** Monta o prompt final da etapa resolvendo placeholders de dados e prompts auxiliares. */
    public String montarPrompt(GeraLandingExperimentWireframeRequest experiment) throws IOException {
        return MontaRequestSupport.montarPrompt("prompts/geralanding/" + PROMPT_MARKDOWN_FILE, PROMPT_MARKDOWN_FILE, experiment.dados(), objectMapper);
    }
}
