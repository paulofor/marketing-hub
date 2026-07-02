package com.marketinghub.worker.pipeline.hypothesispain;

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

/** Responsabilidade: integrar a etapa Dor do pipeline de hipótese aos endpoints internos do backend. */
public class HypothesisPainBackendClient implements StageBackendPort<HypothesisPainInput, HypothesisPainOutput> {
    private final WebClient webClient;
    private final HypothesisPainWorkerProperties properties;
    private final ObjectMapper objectMapper;

    /** Inicializa o cliente com WebClient, propriedades da etapa Dor e ObjectMapper para normalizar contexto. */
    public HypothesisPainBackendClient(WebClient.Builder builder, HypothesisPainWorkerProperties properties, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Busca no backend os jobs da etapa Dor iniciados e aptos para processamento. */
    @Override
    public List<StageExecution<HypothesisPainInput>> listPending(int limit) {
        int effectiveLimit = Math.max(1, limit);
        String uri = joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/hypothesis-pipeline/pain/stage-executions/pending");
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
    public void markRunning(StageExecution<HypothesisPainInput> execution) {
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/running", execution.idJob())
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Persiste no backend o prompt renderizado, schema e request bruto enviados para OpenAI. */
    public void saveOpenAiRequest(StageExecution<HypothesisPainInput> execution, OpenAiRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
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

    /** Envia ao backend a resposta validada da OpenAI para concluir a etapa Dor. */
    @Override
    public void markCompleted(StageExecution<HypothesisPainInput> execution, StageResult<HypothesisPainOutput> result) {
        OpenAiResult<String> openAiResult = openAiResult(result);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("marketNicheId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", openAiResult.modelResponse());
        body.put("rawResponse", openAiResult.rawResponse());
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

    /** Envia ao backend os dados de falha quando a etapa Dor não é concluída. */
    @Override
    public void markFailed(StageExecution<HypothesisPainInput> execution, Throwable error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("marketNicheId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", null);
        body.put("rawResponse", null);
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
    private StageExecution<HypothesisPainInput> toStageExecution(Map<String, Object> item) {
        Long marketNicheId = asLong(item.get("marketNicheId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(firstPresent(item.get("jobid"), item.get("idJob")));
        HypothesisPainInput input = new HypothesisPainInput(marketNicheId, stageCode, idJob, buildPromptDataFromPending(item));
        String status = asString(firstPresent(item.get("status"), "INICIADO"));
        return new StageExecution<>(idJob, marketNicheId, stageCode, status, asInstant(item.get("executionRequestedAt")), input, Map.of());
    }

    /** Monta os dados do prompt com as informações do nicho e contexto comercial disponível. */
    Map<String, Object> buildPromptDataFromPending(Map<String, Object> pending) {
        Map<String, Object> niche = asMap(pending.get("niche"));
        Map<String, Object> enrichmentProfile = asMap(pending.get("enrichmentProfile"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("marketNicheId", pending.get("marketNicheId"));
        payload.put("__promptTemplate", asMap(pending.get("promptTemplate")));
        payload.put("nicheName", optionalText(niche.get("name")));
        payload.put("nicheDescription", optionalText(niche.get("description")));
        payload.put("demandVolume", optionalText(niche.get("demandVolume")));
        payload.put("promises", optionalText(niche.get("promises")));
        payload.put("offers", optionalText(niche.get("offers")));
        payload.put("baseSegmentation", optionalText(niche.get("baseSegmentation")));
        payload.put("interests", optionalText(niche.get("interests")));
        payload.put("demographicFilters", optionalText(niche.get("demographicFilters")));
        payload.put("extraTips", optionalText(niche.get("extraTips")));
        payload.put("existingHypothesesSummary", summarizeExistingHypotheses(asList(pending.get("existingHypotheses"))));
        putEnrichedNicheContext(payload, enrichmentProfile);
        payload.put("CASE_DATA_BLOCK", buildCaseDataBlock(payload));
        return payload;
    }

    /** Resume hipóteses já existentes para orientar o modelo a criar uma dor diferente no mesmo nicho. */
    private String summarizeExistingHypotheses(List<Object> existingHypotheses) {
        if (existingHypotheses.isEmpty()) {
            return "Nenhuma hipótese anterior registrada para este nicho.";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Object item : existingHypotheses) {
            Map<String, Object> hypothesis = asMap(item);
            builder.append(index++).append(") ");
            appendSummaryField(builder, "código", hypothesis.get("title"));
            appendSummaryField(builder, "dor", hypothesis.get("problem"));
            appendSummaryField(builder, "promessa", hypothesis.get("promise"));
            appendSummaryField(builder, "persona", hypothesis.get("persona"));
            appendSummaryField(builder, "mecanismo", hypothesis.get("mechanism"));
            appendSummaryField(builder, "entrega", hypothesis.get("entrega"));
            appendSummaryField(builder, "status", hypothesis.get("status"));
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    /** Acrescenta um campo textual ao resumo quando houver conteúdo útil. */
    private void appendSummaryField(StringBuilder builder, String label, Object value) {
        String text = optionalText(value).trim();
        if (!text.isEmpty()) {
            builder.append(label).append(": ").append(text).append("; ");
        }
    }

    /** Copia o perfil enriquecido do OPRM para o contexto estratégico consumido pela etapa. */
    private void putEnrichedNicheContext(Map<String, Object> payload, Map<String, Object> enrichmentProfile) {
        payload.put("enrichedRoutineSummary", optionalText(enrichmentProfile.get("routineSummary")));
        payload.put("enrichedPainsSummary", optionalText(enrichmentProfile.get("painsSummary")));
        payload.put("enrichedResultsSummary", optionalText(enrichmentProfile.get("resultsSummary")));
        payload.put("enrichedMechanismOpportunitiesSummary", optionalText(enrichmentProfile.get("mechanismOpportunitiesSummary")));
        payload.put("enrichedEvidenceSummary", optionalText(enrichmentProfile.get("evidenceSummary")));
        payload.put("enrichedSourceDomains", optionalText(enrichmentProfile.get("sourceDomains")));
        payload.put("enrichedPersonaSummary", optionalText(enrichmentProfile.get("personaSummary")));
        payload.put("enrichedLanguagePatterns", optionalText(enrichmentProfile.get("languagePatterns")));
        payload.put("enrichedCommercialTriggers", optionalText(enrichmentProfile.get("commercialTriggers")));
        payload.put("enrichedObjections", optionalText(enrichmentProfile.get("objections")));
        payload.put("enrichedQualityStatus", optionalText(enrichmentProfile.get("qualityStatus")));
        payload.put("enrichedConfidenceScore", optionalText(enrichmentProfile.get("confidenceScore")));
        payload.put("enrichedDifficultyEvidenceScore", optionalText(enrichmentProfile.get("difficultyEvidenceScore")));
        payload.put("enrichedSourceDiversityScore", optionalText(enrichmentProfile.get("sourceDiversityScore")));
    }

    /** Normaliza campos textuais opcionais do nicho para evitar nulos no contexto do prompt. */
    private String optionalText(Object value) {
        return value != null ? value.toString() : "";
    }

    /** Monta o bloco textual de contexto estratégico exigido pelo prompt da etapa Dor. */
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
        appendCaseData(builder, "existingHypothesesSummary", payload.get("existingHypothesesSummary"));
        appendEnrichedNicheContext(builder, payload);
        builder.append("[CASE_DATA_END]");
        return builder.toString();
    }

    /** Acrescenta o contexto enriquecido do nicho sem misturar oferta pronta no insumo de hipótese. */
    private void appendEnrichedNicheContext(StringBuilder builder, Map<String, Object> payload) {
        appendCaseData(builder, "enrichedRoutineSummary", payload.get("enrichedRoutineSummary"));
        appendCaseData(builder, "enrichedPainsSummary", payload.get("enrichedPainsSummary"));
        appendCaseData(builder, "enrichedResultsSummary", payload.get("enrichedResultsSummary"));
        appendCaseData(builder, "enrichedMechanismOpportunitiesSummary", payload.get("enrichedMechanismOpportunitiesSummary"));
        appendCaseData(builder, "enrichedEvidenceSummary", payload.get("enrichedEvidenceSummary"));
        appendCaseData(builder, "enrichedSourceDomains", payload.get("enrichedSourceDomains"));
        appendCaseData(builder, "enrichedPersonaSummary", payload.get("enrichedPersonaSummary"));
        appendCaseData(builder, "enrichedLanguagePatterns", payload.get("enrichedLanguagePatterns"));
        appendCaseData(builder, "enrichedCommercialTriggers", payload.get("enrichedCommercialTriggers"));
        appendCaseData(builder, "enrichedObjections", payload.get("enrichedObjections"));
        appendCaseData(builder, "enrichedQualityStatus", payload.get("enrichedQualityStatus"));
        appendCaseData(builder, "enrichedConfidenceScore", payload.get("enrichedConfidenceScore"));
        appendCaseData(builder, "enrichedDifficultyEvidenceScore", payload.get("enrichedDifficultyEvidenceScore"));
        appendCaseData(builder, "enrichedSourceDiversityScore", payload.get("enrichedSourceDiversityScore"));
    }

    /** Acrescenta uma chave do contexto no bloco CASE_DATA com renderização segura. */
    private void appendCaseData(StringBuilder builder, String key, Object value) {
        builder.append(key).append(": ").append(toJsonOrText(value).trim()).append('\n');
    }

    /** Extrai o resultado OpenAI bruto armazenado nas métricas do resultado da etapa. */
    private OpenAiResult<String> openAiResult(StageResult<HypothesisPainOutput> result) {
        Object value = result.metrics().get("openAiResult");
        if (value instanceof OpenAiResult<?> raw) {
            return new OpenAiResult<>(raw.openAiJobId(), raw.rawResponse(), raw.modelResponse(), raw.modelResponse(), raw.inputTokens(), raw.outputTokens(), raw.costUsd());
        }
        throw new IllegalStateException("Resultado da etapa Dor sem métrica openAiResult");
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

    /** Normaliza listas vindas do payload pendente para leitura segura do contrato. */
    private List<Object> asList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(item -> (Object) item).toList();
        }
        return List.of();
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

    /** Monta a URL base dos endpoints internos de execução da etapa Dor. */
    private String stageExecutionBaseUrl() {
        return joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/hypothesis-pipeline/pain/stage-executions");
    }

    /** Registra o payload enviado de volta para o backend com o jobId operacional. */
    private void logBackendPayload(String idJob, Map<String, Object> body) {
        try {
            org.slf4j.LoggerFactory.getLogger(HypothesisPainBackendClient.class).info(
                    "Enviando payload para backend na etapa Dor. jobId={} payload={}",
                    idJob,
                    objectMapper.writeValueAsString(body));
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger(HypothesisPainBackendClient.class).warn(
                    "Falha ao serializar payload de log da etapa Dor. jobId={}",
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
