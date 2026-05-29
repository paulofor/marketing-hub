package com.marketinghub.worker.openai.core.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.reactive.function.client.WebClient;

public class ResponsesApiOpenAiClient implements OpenAiClientPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OpenAiClientProperties properties;
    private final OpenAiCostEstimator costEstimator;
    private final Map<String, OpenAiResult<String>> resultCache = new ConcurrentHashMap<>();

    public ResponsesApiOpenAiClient(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            OpenAiClientProperties properties
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.costEstimator = new OpenAiCostEstimator(
                properties.inputUsdPerMillionTokens(),
                properties.outputUsdPerMillionTokens()
        );
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public OpenAiDispatch dispatch(OpenAiRequest request) {
        try {
            Map<String, Object> requestBody = objectMapper.readValue(request.requestBodyJson(), Map.class);

            Map<String, Object> raw = webClient.post()
                    .uri("/responses")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(properties.timeout());

            if (raw == null) {
                throw new StageWorkerException("OpenAI returned an empty response");
            }

            String rawJson = objectMapper.writeValueAsString(raw);
            String openAiJobId = stringValue(raw.get("id"));
            String modelResponse = extractModelResponse(raw);
            Integer inputTokens = extractInteger(raw, "usage", "input_tokens");
            Integer outputTokens = extractInteger(raw, "usage", "output_tokens");

            OpenAiResult<String> result = new OpenAiResult<>(
                    openAiJobId,
                    rawJson,
                    modelResponse,
                    modelResponse,
                    inputTokens,
                    outputTokens,
                    costEstimator.estimate(inputTokens, outputTokens)
            );

            if (openAiJobId != null) {
                resultCache.put(openAiJobId, result);
            }

            return new OpenAiDispatch(
                    openAiJobId,
                    request.prompt(),
                    request.requestBodyJson(),
                    Instant.now()
            );
        } catch (JsonProcessingException error) {
            throw new StageWorkerException("Invalid OpenAI request JSON", error);
        }
    }

    @Override
    public OpenAiResult<String> awaitResult(OpenAiDispatch dispatch) {
        if (dispatch.openAiJobId() == null) {
            throw new StageWorkerException("Cannot await OpenAI result without openAiJobId");
        }

        OpenAiResult<String> result = resultCache.remove(dispatch.openAiJobId());

        if (result == null) {
            throw new StageWorkerException("OpenAI result not found in local cache: " + dispatch.openAiJobId());
        }

        return result;
    }

    private String extractModelResponse(Map<String, Object> raw) {
        Object outputText = raw.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            return outputText.toString();
        }

        Object output = raw.get("output");
        if (output instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();

            for (Object item : list) {
                if (item instanceof Map<?, ?> itemMap) {
                    Object content = itemMap.get("content");
                    if (content instanceof List<?> contentList) {
                        for (Object contentItem : contentList) {
                            if (contentItem instanceof Map<?, ?> contentMap) {
                                Object text = contentMap.get("text");
                                if (text != null) {
                                    builder.append(text);
                                }
                            }
                        }
                    }
                }
            }

            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }

        throw new StageWorkerException("Could not extract model response text from OpenAI response");
    }

    private Integer extractInteger(Map<String, Object> raw, String parentKey, String childKey) {
        Object parent = raw.get(parentKey);
        if (!(parent instanceof Map<?, ?> map)) {
            return null;
        }

        Object value = map.get(childKey);
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }

        return null;
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }
}
