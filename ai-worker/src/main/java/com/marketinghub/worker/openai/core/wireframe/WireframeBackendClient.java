package com.marketinghub.worker.openai.core.wireframe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageBackendPort;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

public class WireframeBackendClient implements StageBackendPort<WireframeInput, WireframeOutput> {

    private static final String STATUS_STARTED = "INICIADO";

    private final WebClient webClient;
    private final WireframeWorkerProperties properties;
    private final ObjectMapper objectMapper;

    public WireframeBackendClient(
            WebClient.Builder builder,
            WireframeWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        this.webClient = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<StageExecution<WireframeInput>> listPending(int limit) {
        int effectiveLimit = Math.max(1, limit);
        String uri = joinPath(
                properties.backendBaseUrl(),
                properties.apiPrefix(),
                "/internal/geralanding/wireframe/stage-executions/pending"
        );

        List<Map<String, Object>> payload = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block(properties.timeout());

        if (payload == null || payload.isEmpty()) {
            return List.of();
        }

        return payload.stream()
                .map(this::toStageExecution)
                .filter(item -> item.aggregateId() != null && item.idJob() != null)
                .limit(effectiveLimit)
                .toList();
    }

    @Override
    public void markDispatched(StageExecution<WireframeInput> execution, OpenAiDispatch dispatch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", dispatch.prompt());
        body.put("jobidopenai", dispatch.openAiJobId());

        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-prompt", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    @Override
    public void markCompleted(StageExecution<WireframeInput> execution, OpenAiResult<WireframeOutput> result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", result.modelResponse());
        body.put("inputTokens", result.inputTokens());
        body.put("outputTokens", result.outputTokens());
        body.put("costUsd", result.costUsd());
        body.put("openAiJobId", result.openAiJobId());
        body.put("errorMessage", null);
        body.put("errorDetail", null);

        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    @Override
    public void markFailed(StageExecution<WireframeInput> execution, Throwable error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", null);
        body.put("inputTokens", null);
        body.put("outputTokens", null);
        body.put("costUsd", null);
        body.put("openAiJobId", null);
        body.put("errorMessage", error != null ? error.getMessage() : "Unknown error");
        body.put("errorDetail", error != null ? stackTraceSummary(error) : null);

        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    private StageExecution<WireframeInput> toStageExecution(Map<String, Object> item) {
        Long experimentId = asLong(item.get("experimentId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(item.get("jobid"));

        WireframeInput input = new WireframeInput(
                experimentId,
                stageCode,
                idJob,
                buildPromptDataFromPending(item)
        );

        return new StageExecution<>(
                idJob,
                experimentId,
                stageCode,
                STATUS_STARTED,
                asInstant(item.get("executionRequestedAt")),
                input
        );
    }

    private Map<String, Object> buildPromptDataFromPending(Map<String, Object> pending) {
        Map<String, Object> experiment = asMap(pending.get("experiment"));
        Map<String, Object> hypothesis = asMap(pending.get("hypothesis"));
        Map<String, Object> framework = asMap(hypothesis.get("framework"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignAngle", normalizeJsonArtifact(experiment.get("campaignAngle")));
        payload.put("adCopy", normalizeJsonArtifact(experiment.get("adCopy")));
        payload.put("adImageBriefing", normalizeJsonArtifact(experiment.get("adImageBriefing")));
        payload.put("landingPageCopy", normalizeJsonArtifact(experiment.get("landingPageCopy")));
        payload.put("landingPageWireframe", normalizeJsonArtifact(experiment.get("landingPageWireframe")));
        payload.put("NICHE_NAME", firstText(experiment.get("nicheName"), experiment.get("niche"), experiment.get("name")));
        payload.put("PAIN_JSON", framework.getOrDefault("pain", Map.of()));
        payload.put("RESULT_JSON", framework.getOrDefault("result", Map.of()));

        return payload;
    }

    private Object normalizeJsonArtifact(Object value) {
        if (value instanceof String text) {
            return parseJsonField(text);
        }
        return value != null ? value : Map.of();
    }

    private Object parseJsonField(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception error) {
            return raw;
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> converted = new LinkedHashMap<>();
            rawMap.forEach((key, rawValue) -> {
                if (key != null) {
                    converted.put(String.valueOf(key), rawValue);
                }
            });
            return converted;
        }

        return Map.of();
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }

        return null;
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private Instant asInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof String text && !text.isBlank()) {
            return Instant.parse(text.trim());
        }

        return null;
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }

    private String stageExecutionBaseUrl() {
        return joinPath(
                properties.backendBaseUrl(),
                properties.apiPrefix(),
                "/internal/geralanding/wireframe/stage-executions"
        );
    }

    private String stackTraceSummary(Throwable error) {
        StringBuilder builder = new StringBuilder(error.toString());
        for (StackTraceElement element : error.getStackTrace()) {
            builder.append("\n    at ").append(element);
            if (builder.length() > 4000) {
                break;
            }
        }
        return builder.toString();
    }

    private String joinPath(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String normalized = part.trim();
            if (builder.isEmpty()) {
                builder.append(stripTrailingSlash(normalized));
            } else {
                builder.append("/").append(stripSlashes(normalized));
            }
        }
        return builder.toString();
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String stripSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
