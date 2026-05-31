package com.marketinghub.worker.openai.core.imageplanning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.openai.OpenAiClientProperties;
import com.marketinghub.worker.openai.core.port.StagePromptBuilder;
import com.marketinghub.worker.openai.core.prompt.PromptTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: montar o prompt, schema e request OpenAI da etapa imageplanning. */
public class ImagePlanningPromptBuilder implements StagePromptBuilder<ImagePlanningInput> {

    private static final Logger log = LoggerFactory.getLogger(ImagePlanningPromptBuilder.class);

    private final ObjectMapper objectMapper;
    private final OpenAiClientProperties openAiProperties;
    private final ImagePlanningWorkerProperties imagePlanningProperties;
    private final PromptTemplateResolver promptTemplateResolver;

    /** Inicializa o builder com serializador, propriedades OpenAI e propriedades da etapa imageplanning. */
    public ImagePlanningPromptBuilder(
            ObjectMapper objectMapper,
            OpenAiClientProperties openAiProperties,
            ImagePlanningWorkerProperties imagePlanningProperties
    ) {
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.imagePlanningProperties = imagePlanningProperties;
        this.promptTemplateResolver = new PromptTemplateResolver(this::loadResource, this::toJsonOrText);
    }

    /** Monta o request completo da OpenAI mantendo o conteúdo bruto do markdown usado no prompt. */
    @Override
    public OpenAiRequest build(StageExecution<ImagePlanningInput> execution) {
        Map<String, Object> data = execution.input().promptData();

        String promptResource = imagePlanningProperties.promptResource();
        String promptMarkdownContent = loadResource(promptResource);
        String prompt = promptTemplateResolver.resolve(promptMarkdownContent, data, promptResource);
        String schemaJson = loadResource(imagePlanningProperties.schemaResource());
        String requestBodyJson = buildResponsesApiRequest(prompt, schemaJson);

        return new OpenAiRequest(
                openAiProperties.model(),
                prompt,
                requestBodyJson,
                imagePlanningProperties.schemaName(),
                schemaJson,
                promptMarkdownContent,
                Map.of(
                        "stageCode", execution.stageCode(),
                        "idJob", execution.idJob(),
                        "experimentId", execution.aggregateId()
                )
        );
    }

    /** Serializa o corpo compatível com Responses API usando schema JSON estrito. */
    private String buildResponsesApiRequest(String prompt, String schemaJson) {
        try {
            Object schema = objectMapper.readValue(schemaJson, Object.class);

            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", imagePlanningProperties.schemaName());
            format.put("schema", schema);
            format.put("strict", true);

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("format", format);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", openAiProperties.model());
            body.put("input", prompt);
            body.put("text", text);

            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            log.error(
                    "Could not build OpenAI Responses API request for image planning. schemaName={}",
                    imagePlanningProperties.schemaName(),
                    error
            );
            throw new StageWorkerException("Could not build OpenAI Responses API request", error);
        }
    }

    /** Converte valores de placeholder para texto ou JSON formatado antes da substituição. */
    private String toJsonOrText(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof String text) {
            return text;
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException error) {
            log.warn(
                    "Could not serialize image planning prompt value; using toString fallback. valueType={}",
                    value.getClass().getName(),
                    error
            );
            return value.toString();
        }
    }

    /** Lê recursos do classpath usados como prompt markdown ou schema da etapa. */
    private String loadResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            log.error("Could not load image planning resource from classpath. path={}", path, error);
            throw new StageWorkerException("Could not load resource from classpath: " + path, error);
        }
    }
}
