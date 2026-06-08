package com.marketinghub.worker.pipeline.deliverables;

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

/** Responsabilidade: integrar a etapa deliverables do pipeline aos endpoints internos do backend. */
public class DeliverablesBackendClient implements StageBackendPort<DeliverablesInput, DeliverablesOutput> {
    private static final String STATUS_STARTED = "INICIADO";
    private final WebClient webClient;
    private final DeliverablesWorkerProperties properties;
    private final ObjectMapper objectMapper;

    /** Inicializa o cliente com WebClient, propriedades de deliverables e ObjectMapper para normalizar artefatos. */
    public DeliverablesBackendClient(WebClient.Builder builder, DeliverablesWorkerProperties properties, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Busca no backend os jobs de deliverables iniciados e aptos para processamento. */
    @Override
    public List<StageExecution<DeliverablesInput>> listPending(int limit) {
        int effectiveLimit = Math.max(1, limit);
        String uri = joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/geralanding/deliverables/stage-executions/pending");
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

    /** Marca a execução como em processamento para impedir recaptura em ciclos concorrentes. */
    @Override
    public void markRunning(StageExecution<DeliverablesInput> execution) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/running", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Persiste no backend o prompt renderizado, schema e request bruto enviados para OpenAI. */
    public void saveOpenAiRequest(StageExecution<DeliverablesInput> execution, OpenAiRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("prompt", request.prompt());
        body.put("promptMarkdownContent", request.promptMarkdownContent());
        body.put("schemaJson", request.schemaJson());
        body.put("requestBodyJson", request.requestBodyJson());
        body.put("openAiModel", request.model());
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-prompt", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia ao backend a resposta validada da OpenAI para concluir a etapa deliverables. */
    @Override
    public void markCompleted(StageExecution<DeliverablesInput> execution, StageResult<DeliverablesOutput> result) {
        OpenAiResult<String> openAiResult = openAiResult(result);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", openAiResult.modelResponse());
        body.put("inputTokens", openAiResult.inputTokens());
        body.put("outputTokens", openAiResult.outputTokens());
        body.put("costUsd", openAiResult.costUsd());
        body.put("openAiJobId", openAiResult.openAiJobId());
        body.put("errorMessage", null);
        body.put("errorDetail", null);
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia ao backend os dados de falha quando a etapa deliverables não é concluída. */
    @Override
    public void markFailed(StageExecution<DeliverablesInput> execution, Throwable error) {
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

    /** Converte o payload pendente do backend para o modelo interno de execução da etapa. */
    private StageExecution<DeliverablesInput> toStageExecution(Map<String, Object> item) {
        Long experimentId = asLong(item.get("experimentId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(firstPresent(item.get("jobid"), item.get("idJob")));
        DeliverablesInput input = new DeliverablesInput(experimentId, stageCode, idJob, buildPromptDataFromPending(item));
        return new StageExecution<>(idJob, experimentId, stageCode, STATUS_STARTED, asInstant(item.get("executionRequestedAt")), input, Map.of());
    }

    /** Monta os dados do prompt com os artefatos e informações de framework disponíveis no backend. */
    private Map<String, Object> buildPromptDataFromPending(Map<String, Object> pending) {
        Map<String, Object> experiment = asMap(pending.get("experiment"));
        Map<String, Object> hypothesis = asMap(pending.get("hypothesis"));
        Map<String, Object> framework = asMap(hypothesis.get("framework"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("experimentId", pending.get("experimentId"));
        payload.put("experimentName", experiment.get("name"));
        payload.put("hypothesisTitle", hypothesis.get("title"));
        payload.put("campaignAngle", normalizeJsonArtifact(experiment.get("campaignAngle")));
        payload.put("adCopy", normalizeJsonArtifact(experiment.get("adCopy")));
        payload.put("adImageBriefing", normalizeJsonArtifact(experiment.get("adImageBriefing")));
        payload.put("landingPageWireframe", normalizeJsonArtifact(experiment.get("landingPageWireframe")));
        payload.put("landingPageCopy", normalizeJsonArtifact(experiment.get("landingPageCopy")));
        payload.put("landingPageImagePlanning", normalizeJsonArtifact(experiment.get("landingPageImagePlanning")));
        payload.put("landingPageDesignPreset", normalizeJsonArtifact(experiment.get("landingPageDesignPreset")));
        payload.put("landingPageQualityReview", experiment.get("landingPageQualityReview"));
        payload.put("htmlGeraLanding", experiment.get("htmlGeraLanding"));
        payload.put("PAIN_JSON", framework.getOrDefault("pain", Map.of()));
        payload.put("RESULT_JSON", framework.getOrDefault("result", Map.of()));
        payload.put("MECHANISM_JSON", framework.getOrDefault("mechanism", Map.of()));
        payload.put("PROOF_JSON", framework.getOrDefault("proof", Map.of()));
        payload.put("OFFER_JSON", framework.getOrDefault("offer", Map.of()));
        payload.put("CASE_DATA_BLOCK", buildCaseDataBlock(payload));
        return payload;
    }

    /** Monta o bloco textual de contexto estratégico exigido pelo prompt da etapa deliverables. */
    private String buildCaseDataBlock(Map<String, Object> payload) {
        StringBuilder builder = new StringBuilder("[CASE_DATA_BEGIN]\n");
        appendCaseData(builder, "experimentId", payload.get("experimentId"));
        appendCaseData(builder, "experimentName", payload.get("experimentName"));
        appendCaseData(builder, "hypothesisTitle", payload.get("hypothesisTitle"));
        appendCaseData(builder, "PAIN_JSON", payload.get("PAIN_JSON"));
        appendCaseData(builder, "RESULT_JSON", payload.get("RESULT_JSON"));
        appendCaseData(builder, "MECHANISM_JSON", payload.get("MECHANISM_JSON"));
        appendCaseData(builder, "PROOF_JSON", payload.get("PROOF_JSON"));
        appendCaseData(builder, "OFFER_JSON", payload.get("OFFER_JSON"));
        appendCaseData(builder, "campaignAngle", payload.get("campaignAngle"));
        appendCaseData(builder, "adCopy", payload.get("adCopy"));
        appendCaseData(builder, "landingPageCopy", payload.get("landingPageCopy"));
        appendCaseData(builder, "landingPageQualityReview", payload.get("landingPageQualityReview"));
        builder.append("[CASE_DATA_END]");
        return builder.toString();
    }

    /** Acrescenta uma chave do contexto no bloco CASE_DATA com renderização segura. */
    private void appendCaseData(StringBuilder builder, String key, Object value) {
        builder.append(key).append(": ").append(toJsonOrText(value).trim()).append('\n');
    }

    /** Extrai o resultado OpenAI bruto armazenado nas métricas do resultado da etapa. */
    private OpenAiResult<String> openAiResult(StageResult<DeliverablesOutput> result) {
        Object value = result.metrics().get("openAiResult");
        if (value instanceof OpenAiResult<?> raw) {
            return new OpenAiResult<>(raw.openAiJobId(), raw.rawResponse(), raw.modelResponse(), raw.modelResponse(), raw.inputTokens(), raw.outputTokens(), raw.costUsd());
        }
        throw new IllegalStateException("Resultado da etapa deliverables sem métrica openAiResult");
    }

    /** Renderiza objetos estruturados como JSON formatado e preserva textos simples. */
    private String toJsonOrText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception error) {
            return value.toString();
        }
    }

    /** Normaliza artefatos que podem chegar como JSON textual, objeto estruturado ou valor simples. */
    private Object normalizeJsonArtifact(Object value) {
        if (value instanceof String text) {
            return parseJsonField(text);
        }
        return value != null ? value : Map.of();
    }

    /** Interpreta um campo textual JSON quando possível, preservando o texto original em caso de formato inválido. */
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

    /** Extrai um mapa tipado quando o valor recebido é um objeto JSON. */
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

    /** Converte um valor genérico para Long quando possível. */
    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    /** Converte um valor genérico para texto preservando nulo quando ausente. */
    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    /** Retorna o primeiro valor presente entre duas opções. */
    private Object firstPresent(Object first, Object second) {
        return first != null ? first : second;
    }

    /** Converte um valor genérico para Instant quando possível. */
    private Instant asInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Instant.parse(text.trim());
        }
        return null;
    }

    /** Monta a URL base dos endpoints internos de execução da etapa deliverables. */
    private String stageExecutionBaseUrl() {
        return joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/geralanding/deliverables/stage-executions");
    }

    /** Resume a stack trace em texto para envio controlado ao backend. */
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

    /** Junta partes de URL evitando barras duplicadas entre segmentos. */
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

    /** Remove a barra final de um segmento de URL quando presente. */
    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** Remove barras iniciais e finais de um segmento intermediário de URL. */
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
