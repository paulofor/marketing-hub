package com.marketinghub.worker.openai.core.wireframe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StagePromptBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class WireframePromptBuilder implements StagePromptBuilder<WireframeInput> {

    private final ObjectMapper objectMapper;
    private final String model;
    private final String promptResource;
    private final String schemaResource;
    private final String schemaName;

    public WireframePromptBuilder(
            ObjectMapper objectMapper,
            @Value("${openai.model:gpt-5.2}") String model,
            @Value("${wireframe.prompt.resource:prompts/geralanding-wireframe.md}") String promptResource,
            @Value("${wireframe.schema.resource:schemas/geralanding-wireframe.schema.json}") String schemaResource,
            @Value("${wireframe.schema.name:landing_page_wireframe}") String schemaName
    ) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.promptResource = promptResource;
        this.schemaResource = schemaResource;
        this.schemaName = schemaName;
    }

    @Override
    public OpenAiRequest build(StageExecution<WireframeInput> execution) {
        WireframeInput input = execution.input();
        Map<String, Object> data = input.promptData();

        String prompt = resolvePrompt(loadResource(promptResource), data);
        String schemaJson = loadResource(schemaResource);
        String requestBodyJson = buildResponsesApiRequest(prompt, schemaJson);

        return new OpenAiRequest(
                model,
                prompt,
                requestBodyJson,
                schemaName,
                schemaJson,
                Map.of(
                        "stageCode", execution.stageCode(),
                        "idJob", execution.idJob(),
                        "experimentId", execution.aggregateId()
                )
        );
    }

    private String buildResponsesApiRequest(String prompt, String schemaJson) {
        try {
            Object schema = objectMapper.readValue(schemaJson, Object.class);

            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", schemaName);
            format.put("schema", schema);
            format.put("strict", true);

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("format", format);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", prompt);
            body.put("text", text);

            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            throw new StageWorkerException("Could not build OpenAI Responses API request", error);
        }
    }

    private String resolvePrompt(String template, Map<String, Object> data) {
        String result = template;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = toJsonOrText(entry.getValue());

            result = result.replace("{{" + key + "}}", value);
            result = result.replace("${" + key + "}", value);
        }

        return result;
    }

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

    private String loadResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new StageWorkerException("Could not load resource from classpath: " + path, error);
        }
    }
}
