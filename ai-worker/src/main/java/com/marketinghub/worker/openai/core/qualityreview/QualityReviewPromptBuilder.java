package com.marketinghub.worker.openai.core.qualityreview;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: montar prompt, schema e request multimodal OpenAI da revisão visual. */
public class QualityReviewPromptBuilder implements StagePromptBuilder<QualityReviewInput> {

    private final ObjectMapper objectMapper;
    private final OpenAiClientProperties openAiProperties;
    private final QualityReviewWorkerProperties properties;
    private final PromptTemplateResolver promptTemplateResolver;

    /** Inicializa o builder com serializador, propriedades OpenAI e configurações da revisão visual. */
    public QualityReviewPromptBuilder(
            ObjectMapper objectMapper,
            OpenAiClientProperties openAiProperties,
            QualityReviewWorkerProperties properties) {
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.properties = properties;
        this.promptTemplateResolver = new PromptTemplateResolver(this::loadResource, this::toJsonOrText);
    }

    /** Monta o request multimodal com texto e imagens disponíveis da landing para a Responses API. */
    @Override
    public OpenAiRequest build(StageExecution<QualityReviewInput> execution) {
        String promptResource = properties.promptResource();
        String promptMarkdownContent = loadResource(promptResource);
        String prompt = promptTemplateResolver.resolve(promptMarkdownContent, execution.input().promptData(), promptResource);
        String schemaJson = loadResource(properties.schemaResource());
        String requestBodyJson = buildResponsesApiRequest(prompt, schemaJson, execution.input().imageUrls());
        return new OpenAiRequest(
                openAiProperties.model(),
                prompt,
                requestBodyJson,
                properties.schemaName(),
                schemaJson,
                promptMarkdownContent,
                Map.of("stageCode", execution.stageCode(), "idJob", execution.idJob(), "experimentId", execution.aggregateId()));
    }

    /** Serializa o corpo compatível com Responses API usando schema estrito e entradas visuais por URL. */
    private String buildResponsesApiRequest(String prompt, String schemaJson, List<String> imageUrls) {
        try {
            Object schema = objectMapper.readValue(schemaJson, Object.class);
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", properties.schemaName());
            format.put("schema", schema);
            format.put("strict", true);

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("format", format);

            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "input_text", "text", prompt));
            for (String imageUrl : imageUrls) {
                content.add(Map.of("type", "input_image", "image_url", imageUrl, "detail", "high"));
            }

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "user");
            message.put("content", content);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", openAiProperties.model());
            body.put("input", List.of(message));
            body.put("text", text);
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            throw new StageWorkerException("Could not build quality review OpenAI request", error);
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
            return value.toString();
        }
    }

    /** Lê recursos do classpath usados como prompt markdown ou schema da revisão visual. */
    private String loadResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new StageWorkerException("Could not load resource from classpath: " + path, error);
        }
    }
}
