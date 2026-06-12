package com.marketinghub.worker.pipeline.hypothesisoffer;

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

/** Responsabilidade: integrar a etapa Oferta do pipeline de hipótese aos endpoints internos do backend. */
public class HypothesisOfferBackendClient implements StageBackendPort<HypothesisOfferInput, HypothesisOfferOutput> {
    private final WebClient webClient;
    private final HypothesisOfferWorkerProperties properties;
    private final ObjectMapper objectMapper;

    /** Inicializa o cliente com WebClient, propriedades da etapa Oferta e ObjectMapper para normalizar contexto. */
    public HypothesisOfferBackendClient(WebClient.Builder builder, HypothesisOfferWorkerProperties properties, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Busca no backend os jobs da etapa Oferta iniciados e aptos para processamento. */
    @Override
    public List<StageExecution<HypothesisOfferInput>> listPending(int limit) {
        int effectiveLimit = Math.max(1, limit);
        String uri = joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/hypothesis-pipeline/offer/stage-executions/pending");
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
    public void markRunning(StageExecution<HypothesisOfferInput> execution) {
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/running", execution.idJob())
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Persiste no backend o prompt renderizado, schema e request bruto enviados para OpenAI. */
    public void saveOpenAiRequest(StageExecution<HypothesisOfferInput> execution, OpenAiRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", request.prompt());
        body.put("promptMarkdownContent", request.promptMarkdownContent());
        body.put("schemaJson", request.schemaJson());
        body.put("requestBodyJson", request.requestBodyJson());
        body.put("openAiModel", request.model());
        logBackendPayload(execution.idJob(), body);
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-prompt", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia ao backend a resposta validada da OpenAI para concluir a etapa Oferta. */
    @Override
    public void markCompleted(StageExecution<HypothesisOfferInput> execution, StageResult<HypothesisOfferOutput> result) {
        OpenAiResult<String> openAiResult = openAiResult(result);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("marketNicheId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", openAiResult.modelResponse());
        body.put("inputTokens", openAiResult.inputTokens());
        body.put("outputTokens", openAiResult.outputTokens());
        body.put("costUsd", openAiResult.costUsd());
        body.put("openAiJobId", openAiResult.openAiJobId());
        body.put("errorMessage", null);
        body.put("errorDetail", null);
        logBackendPayload(execution.idJob(), body);
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia ao backend os dados de falha quando a etapa Oferta não é concluída. */
    @Override
    public void markFailed(StageExecution<HypothesisOfferInput> execution, Throwable error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("marketNicheId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", null);
        body.put("inputTokens", null);
        body.put("outputTokens", null);
        body.put("costUsd", null);
        body.put("openAiJobId", null);
        body.put("errorMessage", error != null ? error.getMessage() : "Unknown error");
        body.put("errorDetail", error != null ? stackTraceSummary(error) : null);
        logBackendPayload(execution.idJob(), body);
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Converte o payload pendente do backend para o modelo interno de execução da etapa. */
    private StageExecution<HypothesisOfferInput> toStageExecution(Map<String, Object> item) {
        Long marketNicheId = asLong(item.get("marketNicheId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(firstPresent(item.get("jobid"), item.get("idJob")));
        HypothesisOfferInput input = new HypothesisOfferInput(marketNicheId, stageCode, idJob, buildPromptDataFromPending(item));
        String status = asString(firstPresent(item.get("status"), "INICIADO"));
        return new StageExecution<>(idJob, marketNicheId, stageCode, status, asInstant(item.get("executionRequestedAt")), input, Map.of());
    }

    /** Monta os dados do prompt com as informações do nicho e contexto comercial disponível. */
    Map<String, Object> buildPromptDataFromPending(Map<String, Object> pending) {
        Map<String, Object> niche = asMap(pending.get("niche"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("marketNicheId", pending.get("marketNicheId"));
        payload.put("nicheName", optionalText(niche.get("name")));
        payload.put("nicheDescription", optionalText(niche.get("description")));
        payload.put("demandVolume", optionalText(niche.get("demandVolume")));
        payload.put("promises", optionalText(niche.get("promises")));
        payload.put("offers", optionalText(niche.get("offers")));
        payload.put("baseSegmentation", optionalText(niche.get("baseSegmentation")));
        payload.put("interests", optionalText(niche.get("interests")));
        payload.put("demographicFilters", optionalText(niche.get("demographicFilters")));
        payload.put("extraTips", optionalText(niche.get("extraTips")));
        payload.put("painModelResponse", optionalText(pending.get("painModelResponse")));
        payload.put("resultModelResponse", optionalText(pending.get("resultModelResponse")));
        payload.put("mechanismModelResponse", optionalText(pending.get("mechanismModelResponse")));
        payload.put("proofModelResponse", optionalText(pending.get("proofModelResponse")));
        payload.put("CASE_DATA_BLOCK", buildCaseDataBlock(payload));
        return payload;
    }

    /** Normaliza campos textuais opcionais do nicho para evitar nulos no contexto do prompt. */
    private String optionalText(Object value) {
        return value != null ? value.toString() : "";
    }

    /** Monta o bloco textual de contexto estratégico exigido pelo prompt da etapa Oferta. */
    private String buildCaseDataBlock(Map<String, Object> payload) {
        StringBuilder builder = new StringBuilder("[CASE_DATA_BEGIN]\n");
        appendCaseData(builder, "marketNicheId", payload.get("marketNicheId"));
        appendCaseData(builder, "nicheName", payload.get("nicheName"));
        appendCaseData(builder, "nicheDescription", payload.get("nicheDescription"));
        appendCaseData(builder, "demandVolume", payload.get("demandVolume"));
        appendCaseData(builder, "promises", payload.get("promises"));
        appendCaseData(builder, "offers", payload.get("offers"));
        appendCaseData(builder, "baseSegmentation", payload.get("baseSegmentation"));
        appendCaseData(builder, "interests", payload.get("interests"));
        appendCaseData(builder, "demographicFilters", payload.get("demographicFilters"));
        appendCaseData(builder, "extraTips", payload.get("extraTips"));
        appendCaseData(builder, "painModelResponse", payload.get("painModelResponse"));
        appendCaseData(builder, "resultModelResponse", payload.get("resultModelResponse"));
        appendCaseData(builder, "mechanismModelResponse", payload.get("mechanismModelResponse"));
        appendCaseData(builder, "proofModelResponse", payload.get("proofModelResponse"));
        builder.append("[CASE_DATA_END]");
        return builder.toString();
    }

    /** Acrescenta uma chave do contexto no bloco CASE_DATA com renderização segura. */
    private void appendCaseData(StringBuilder builder, String key, Object value) {
        builder.append(key).append(": ").append(toJsonOrText(value).trim()).append('\n');
    }

    /** Extrai a oferta OpenAI bruto armazenado nas métricas da oferta da etapa. */
    private OpenAiResult<String> openAiResult(StageResult<HypothesisOfferOutput> result) {
        Object value = result.metrics().get("openAiResult");
        if (value instanceof OpenAiResult<?> raw) {
            return new OpenAiResult<>(raw.openAiJobId(), raw.rawResponse(), raw.modelResponse(), raw.modelResponse(), raw.inputTokens(), raw.outputTokens(), raw.costUsd());
        }
        throw new IllegalStateException("Resultado da etapa Oferta sem métrica openAiResult");
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
            org.slf4j.LoggerFactory.getLogger(HypothesisOfferBackendClient.class).warn(
                    "Falha ao serializar valor do prompt da etapa Oferta; usando fallback textual. valueType={}",
                    value.getClass().getName(),
                    error);
            return value.toString();
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

    /** Monta a URL base dos endpoints internos de execução da etapa Oferta. */
    private String stageExecutionBaseUrl() {
        return joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/hypothesis-pipeline/offer/stage-executions");
    }

    /** Registra o payload enviado de volta para o backend com o jobId operacional. */
    private void logBackendPayload(String idJob, Map<String, Object> body) {
        try {
            org.slf4j.LoggerFactory.getLogger(HypothesisOfferBackendClient.class).info(
                    "Enviando payload para backend na etapa Oferta. jobId={} payload={}",
                    idJob,
                    objectMapper.writeValueAsString(body));
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger(HypothesisOfferBackendClient.class).warn(
                    "Falha ao serializar payload de log da etapa Oferta. jobId={}",
                    idJob,
                    error);
        }
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
