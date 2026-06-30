package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.pipeline.StageBackendPort;
import com.marketinghub.worker.pipeline.StageExecution;
import com.marketinghub.worker.pipeline.StageResult;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: integrar o worker GeraSalesPage v1 aos endpoints internos do backend. */
public class GeraSalesPageBackendClient implements StageBackendPort<GeraSalesPageInput, GeraSalesPageOutput> {
    private final WebClient webClient;
    private final GeraSalesPageWorkerProperties properties;
    private final ObjectMapper objectMapper;

    /** Inicializa o client com WebClient, propriedades do worker e ObjectMapper para normalização. */
    public GeraSalesPageBackendClient(
            WebClient.Builder builder,
            GeraSalesPageWorkerProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Lista pendências em todas as etapas configuradas, respeitando o limite global do ciclo. */
    @Override
    public List<StageExecution<GeraSalesPageInput>> listPending(int limit) {
        int effectiveLimit = Math.max(1, limit);
        return properties.stageCodes().stream()
                .flatMap(stage -> pendingForStage(stage).stream())
                .limit(effectiveLimit)
                .toList();
    }

    /** Marca uma execução como em processamento no backend. */
    @Override
    public void markRunning(StageExecution<GeraSalesPageInput> execution) {
        webClient.post()
                .uri(stageExecutionBaseUrl(execution.stageCode()) + "/{idJob}/running", execution.idJob())
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Salva prompt, schema e request bruto enviados à OpenAI. */
    public void saveOpenAiRequest(StageExecution<GeraSalesPageInput> execution, OpenAiRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", request.prompt());
        body.put("promptMarkdownContent", request.promptMarkdownContent());
        body.put("schemaJson", request.schemaJson());
        body.put("requestBodyJson", request.requestBodyJson());
        body.put("openAiModel", request.model());
        webClient.post()
                .uri(stageExecutionBaseUrl(execution.stageCode()) + "/{idJob}/recebe-prompt", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia resposta final validada para o backend concluir a etapa. */
    @Override
    public void markCompleted(StageExecution<GeraSalesPageInput> execution, StageResult<GeraSalesPageOutput> result) {
        OpenAiResult<String> openAiResult = openAiResult(result);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", openAiResult.modelResponse());
        body.put("rawResponse", openAiResult.rawResponse());
        body.put("inputTokens", openAiResult.inputTokens());
        body.put("outputTokens", openAiResult.outputTokens());
        body.put("costUsd", openAiResult.costUsd());
        body.put("openAiJobId", openAiResult.openAiJobId());
        body.put("errorMessage", null);
        body.put("errorDetail", null);
        webClient.post()
                .uri(stageExecutionBaseUrl(execution.stageCode()) + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia falha técnica ou validação inválida para o backend. */
    @Override
    public void markFailed(StageExecution<GeraSalesPageInput> execution, Throwable error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", null);
        body.put("rawResponse", null);
        body.put("inputTokens", null);
        body.put("outputTokens", null);
        body.put("costUsd", null);
        body.put("openAiJobId", null);
        body.put("errorMessage", error != null ? error.getMessage() : "Unknown error");
        body.put("errorDetail", error != null ? stackTraceSummary(error) : null);
        webClient.post()
                .uri(stageExecutionBaseUrl(execution.stageCode()) + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Busca pendências de uma etapa específica no backend. */
    private List<StageExecution<GeraSalesPageInput>> pendingForStage(String stageCode) {
        String uri = stageExecutionBaseUrl(stageCode) + "/pending";
        List<Map<String, Object>> payload = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block(properties.timeout());
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        return payload.stream().map(this::toStageExecution).toList();
    }

    /** Converte o payload pendente do backend para o modelo interno do pipeline genérico. */
    private StageExecution<GeraSalesPageInput> toStageExecution(Map<String, Object> item) {
        Long experimentId = asLong(item.get("experimentId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(item.get("jobid"));
        Map<String, Object> template = asMap(item.get("promptTemplate"));
        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("experiment", item.get("experiment"));
        promptData.put("previousStageOutputs", item.get("previousStageOutputs"));
        GeraSalesPageInput input = new GeraSalesPageInput(
                experimentId,
                stageCode,
                idJob,
                asString(template.get("model")),
                asString(template.get("schemaName")),
                asString(template.get("promptMarkdownContent")),
                asString(template.get("schemaJson")),
                promptData);
        return new StageExecution<>(
                idJob,
                experimentId,
                stageCode,
                "INICIADO",
                asInstant(item.get("executionRequestedAt")),
                input,
                Map.of());
    }

    /** Extrai o resultado OpenAI bruto armazenado nas métricas do resultado. */
    private OpenAiResult<String> openAiResult(StageResult<GeraSalesPageOutput> result) {
        Object value = result.metrics().get("openAiResult");
        if (value instanceof OpenAiResult<?> raw) {
            return new OpenAiResult<>(
                    raw.openAiJobId(),
                    raw.rawResponse(),
                    raw.modelResponse(),
                    raw.modelResponse(),
                    raw.inputTokens(),
                    raw.outputTokens(),
                    raw.costUsd());
        }
        throw new IllegalStateException("Resultado do GeraSalesPage v1 sem métrica openAiResult");
    }

    /** Extrai um mapa tipado de payload JSON genérico. */
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

    /** Converte valor genérico para Long quando possível. */
    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    /** Converte valor genérico para String preservando nulo. */
    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    /** Converte valor textual de data para Instant quando presente. */
    private Instant asInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Instant.parse(text.trim());
        }
        return null;
    }

    /** Monta a URL base dos endpoints internos por etapa. */
    private String stageExecutionBaseUrl(String stageCode) {
        return joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/gerasalespage/v1", stageCode, "/stage-executions");
    }

    /** Resume stack trace para persistência controlada de erro no backend. */
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

    /** Junta partes de URL evitando barras duplicadas. */
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

    /** Remove barra final de segmento. */
    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** Remove barras externas de segmento intermediário. */
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
