package com.marketinghub.worker.openai.core.copy;

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

/** Responsabilidade: integrar a etapa copy do core OpenAI aos endpoints internos do backend. */
public class CopyBackendClient implements StageBackendPort<CopyInput, CopyOutput> {

    private static final String STATUS_STARTED = "INICIADO";

    private final WebClient webClient;
    private final CopyWorkerProperties properties;
    private final ObjectMapper objectMapper;

    /** Inicializa o cliente com WebClient, propriedades de copy e ObjectMapper para normalizar artefatos. */
    public CopyBackendClient(
            WebClient.Builder builder,
            CopyWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        this.webClient = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Busca no backend os jobs de copy iniciados e aptos para processamento pela OpenAI. */
    @Override
    public List<StageExecution<CopyInput>> listPending(int limit) {
        int effectiveLimit = Math.max(1, limit);
        String uri = joinPath(
                properties.backendBaseUrl(),
                properties.apiPrefix(),
                "/internal/geralanding/copy/stage-executions/pending"
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

    /** Envia ao backend o prompt renderizado, o schema e o request cru despachados para a OpenAI. */
    @Override
    public void markDispatched(StageExecution<CopyInput> execution, OpenAiDispatch dispatch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", dispatch.prompt());
        body.put("promptMarkdownContent", dispatch.promptMarkdownContent());
        body.put("schemaJson", dispatch.schemaJson());
        body.put("requestBodyJson", dispatch.requestBodyJson());
        body.put("jobidopenai", dispatch.openAiJobId());

        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-prompt", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia ao backend a resposta validada da OpenAI para concluir a etapa copy. */
    @Override
    public void markCompleted(StageExecution<CopyInput> execution, OpenAiResult<CopyOutput> result) {
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

    /** Envia ao backend os dados de falha quando a etapa copy não é concluída. */
    @Override
    public void markFailed(StageExecution<CopyInput> execution, Throwable error) {
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
    private StageExecution<CopyInput> toStageExecution(Map<String, Object> item) {
        Long experimentId = asLong(item.get("experimentId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(item.get("jobid"));

        CopyInput input = new CopyInput(
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

    /** Monta os dados do prompt com os artefatos e informações de framework disponíveis no backend. */
    private Map<String, Object> buildPromptDataFromPending(Map<String, Object> pending) {
        Map<String, Object> experiment = asMap(pending.get("experiment"));
        Map<String, Object> hypothesis = asMap(pending.get("hypothesis"));
        Map<String, Object> framework = asMap(hypothesis.get("framework"));

        Map<String, Object> payload = new LinkedHashMap<>();
        Object campaignAngle = normalizeJsonArtifact(experiment.get("campaignAngle"));
        Object adCopy = normalizeJsonArtifact(experiment.get("adCopy"));
        Object adImageBriefing = normalizeJsonArtifact(experiment.get("adImageBriefing"));
        Object landingPageWireframe = normalizeJsonArtifact(experiment.get("landingPageWireframe"));
        Object pain = framework.getOrDefault("pain", Map.of());
        Object result = framework.getOrDefault("result", Map.of());
        Object mechanism = framework.getOrDefault("mechanism", Map.of());
        Object proof = framework.getOrDefault("proof", Map.of());
        Object offer = framework.getOrDefault("offer", Map.of());
        String nicheName = firstText(experiment.get("nicheName"), experiment.get("niche"), experiment.get("name"));

        payload.put("singlePain", experiment.get("singlePain"));
        payload.put("freeReward", experiment.get("freeReward"));
        payload.put("funnelPromise", experiment.get("funnelPromise"));
        payload.put("primaryCta", experiment.get("primaryCta"));
        payload.put("campaignObjective", experiment.get("campaignObjective"));
        payload.put("campaignAngle", campaignAngle);
        payload.put("adCopy", adCopy);
        payload.put("adImageBriefing", adImageBriefing);
        payload.put("landingPageWireframe", landingPageWireframe);
        payload.put("NICHE_NAME", nicheName);
        payload.put("PAIN_JSON", pain);
        payload.put("RESULT_JSON", result);
        payload.put("MECHANISM_JSON", mechanism);
        payload.put("PROOF_JSON", proof);
        payload.put("OFFER_JSON", offer);
        payload.put("CASE_DATA_BLOCK", buildCaseDataBlock(payload));

        return payload;
    }

    /** Monta o bloco textual de contexto estratégico exigido pelo prompt da etapa copy. */
    private String buildCaseDataBlock(Map<String, Object> payload) {
        StringBuilder builder = new StringBuilder("[CASE_DATA_BEGIN]\n");
        appendCaseData(builder, "NICHE_NAME", payload.get("NICHE_NAME"));
        appendCaseData(builder, "singlePain", payload.get("singlePain"));
        appendCaseData(builder, "freeReward", payload.get("freeReward"));
        appendCaseData(builder, "funnelPromise", payload.get("funnelPromise"));
        appendCaseData(builder, "primaryCta", payload.get("primaryCta"));
        appendCaseData(builder, "campaignObjective", payload.get("campaignObjective"));
        appendCaseData(builder, "PAIN_JSON", payload.get("PAIN_JSON"));
        appendCaseData(builder, "RESULT_JSON", payload.get("RESULT_JSON"));
        appendCaseData(builder, "MECHANISM_JSON", payload.get("MECHANISM_JSON"));
        appendCaseData(builder, "PROOF_JSON", payload.get("PROOF_JSON"));
        appendCaseData(builder, "OFFER_JSON", payload.get("OFFER_JSON"));
        appendCaseData(builder, "campaignAngle", payload.get("campaignAngle"));
        appendCaseData(builder, "adCopy", payload.get("adCopy"));
        appendCaseData(builder, "adImageBriefing", payload.get("adImageBriefing"));
        builder.append("[CASE_DATA_END]");
        return builder.toString();
    }

    /** Acrescenta uma chave do contexto no bloco CASE_DATA com renderização segura. */
    private void appendCaseData(StringBuilder builder, String key, Object value) {
        builder.append(key).append(": ").append(toJsonOrText(value).trim()).append('\n');
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

    /** Retorna o primeiro valor textual não vazio entre as opções informadas. */
    private String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }

    /** Monta a URL base dos endpoints internos de execução da etapa copy. */
    private String stageExecutionBaseUrl() {
        return joinPath(
                properties.backendBaseUrl(),
                properties.apiPrefix(),
                "/internal/geralanding/copy/stage-executions"
        );
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
