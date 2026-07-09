package com.marketinghub.worker.openai.core.qualityreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageBackendPort;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: integrar a revisão visual do core OpenAI aos endpoints internos do backend. */
public class QualityReviewBackendClient implements StageBackendPort<QualityReviewInput, QualityReviewOutput> {

    private static final String STATUS_STARTED = "INICIADO";

    private final WebClient webClient;
    private final QualityReviewWorkerProperties properties;
    /** Inicializa o cliente com WebClient, propriedades da revisão visual e ObjectMapper mantido por compatibilidade de configuração. */
    public QualityReviewBackendClient(WebClient.Builder builder, QualityReviewWorkerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.webClient = builder.clone()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(properties.maxInMemorySizeBytes()))
                .build();
    }

    /** Busca no backend os jobs de revisão visual iniciados e aptos para processamento pela OpenAI. */
    @Override
    public List<StageExecution<QualityReviewInput>> listPending(int limit) {
        List<Map<String, Object>> payload = webClient.get()
                .uri(stageExecutionBaseUrl() + "/pending")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block(properties.timeout());
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        return payload.stream()
                .map(this::toStageExecution)
                .filter(item -> item.aggregateId() != null && item.idJob() != null)
                .limit(Math.max(1, limit))
                .toList();
    }

    /** Envia ao backend o prompt renderizado, o schema e o request multimodal despachados para a OpenAI. */
    @Override
    public void markDispatched(StageExecution<QualityReviewInput> execution, OpenAiDispatch dispatch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", dispatch.prompt());
        body.put("promptMarkdownContent", dispatch.promptMarkdownContent());
        body.put("schemaJson", dispatch.schemaJson());
        body.put("requestBodyJson", dispatch.requestBodyJson());
        body.put("jobidopenai", dispatch.openAiJobId());
        body.put("qualityReviewAudit", dispatch.metadata().get("qualityReviewAudit"));
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-prompt", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia ao backend a resposta validada da OpenAI para concluir a revisão visual. */
    @Override
    public void markCompleted(StageExecution<QualityReviewInput> execution, OpenAiResult<QualityReviewOutput> result) {
        postResponse(execution, result.modelResponse(), result.inputTokens(), result.outputTokens(), result.costUsd(), result.openAiJobId(), null, null);
    }

    /** Envia ao backend os dados de falha quando a revisão visual não é concluída. */
    @Override
    public void markFailed(StageExecution<QualityReviewInput> execution, Throwable error) {
        postResponse(execution, null, null, null, null, null, error != null ? error.getMessage() : "Unknown error", error != null ? stackTraceSummary(error) : null);
    }

    /** Converte o payload pendente do backend para o modelo interno de execução da etapa. */
    private StageExecution<QualityReviewInput> toStageExecution(Map<String, Object> item) {
        Long experimentId = asLong(item.get("experimentId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(item.get("jobid"));
        Map<String, Object> promptData = buildPromptDataFromPending(item);
        QualityReviewInput input = new QualityReviewInput(experimentId, stageCode, idJob, promptData, resolveLandingHtml(promptData));
        return new StageExecution<>(idJob, experimentId, stageCode, STATUS_STARTED, asInstant(item.get("executionRequestedAt")), input);
    }

    /** Monta o contexto textual enxuto com somente o HTML final canônico necessário ao Quality Review. */
    private Map<String, Object> buildPromptDataFromPending(Map<String, Object> pending) {
        Map<String, Object> experiment = asMap(pending.get("experiment"));
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "singlePain", firstPresent(pending.get("singlePain"), experiment.get("singlePain")));
        putIfPresent(payload, "freeReward", firstPresent(pending.get("freeReward"), experiment.get("freeReward")));
        putIfPresent(payload, "funnelPromise", firstPresent(pending.get("funnelPromise"), experiment.get("funnelPromise")));
        putIfPresent(payload, "primaryCta", firstPresent(pending.get("primaryCta"), experiment.get("primaryCta")));
        putIfPresent(payload, "campaignObjective", firstPresent(pending.get("campaignObjective"), experiment.get("campaignObjective")));
        putIfPresent(payload, "htmlGeraLanding", resolveHtmlGeraLanding(pending, experiment));
        return payload;
    }


    /** Retorna o primeiro valor não nulo entre origem principal e fallback. */
    private Object firstPresent(Object primary, Object fallback) {
        return primary != null ? primary : fallback;
    }

    /** Resolve exclusivamente o htmlGeraLanding usado como fonte visual canônica do Quality Review. */
    private String resolveLandingHtml(Map<String, Object> promptData) {
        return asString(promptData.get("htmlGeraLanding"));
    }

    /** Resolve o campo canônico htmlGeraLanding recebido do backend sem usar fallback legado de landingPageHtml. */
    private String resolveHtmlGeraLanding(Map<String, Object> pending, Map<String, Object> experiment) {
        String html = asString(pending.get("htmlGeraLanding"));
        return html != null ? html : asString(experiment.get("htmlGeraLanding"));
    }

    /** Adiciona ao payload somente campos com valor real para preservar compatibilidade com Map.copyOf. */
    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    /** Envia o callback de resposta ou falha da revisão visual ao backend. */
    private void postResponse(StageExecution<QualityReviewInput> execution, String modelResponse, Integer inputTokens, Integer outputTokens, Object costUsd, String openAiJobId, String errorMessage, String errorDetail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", modelResponse);
        body.put("inputTokens", inputTokens);
        body.put("outputTokens", outputTokens);
        body.put("costUsd", costUsd);
        body.put("openAiJobId", openAiJobId);
        body.put("errorMessage", errorMessage);
        body.put("errorDetail", errorDetail);
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
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
            return Long.parseLong(text);
        }
        return null;
    }

    /** Converte um valor genérico para String preservando nulos. */
    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    /** Converte timestamps textuais ISO-8601 para Instant quando presentes. */
    private Instant asInstant(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return Instant.parse(text);
        }
        return null;
    }

    /** Resolve a URL base dos endpoints internos da etapa quality-review. */
    private String stageExecutionBaseUrl() {
        return joinPath(properties.backendBaseUrl(), properties.apiPrefix(), "/internal/geralanding/quality-review/stage-executions");
    }

    /** Une partes de URL sem duplicar barras entre segmentos. */
    private String joinPath(String base, String prefix, String path) {
        return trimRight(base) + "/" + trimSlashes(prefix) + "/" + trimSlashes(path);
    }

    /** Remove barras finais de um trecho de URL. */
    private String trimRight(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    /** Remove barras iniciais e finais de um trecho de URL. */
    private String trimSlashes(String value) {
        return value == null ? "" : value.replaceAll("^/+|/+$", "");
    }

    /** Resume o stack trace para persistência no backend sem perder o ponto da falha. */
    private String stackTraceSummary(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
