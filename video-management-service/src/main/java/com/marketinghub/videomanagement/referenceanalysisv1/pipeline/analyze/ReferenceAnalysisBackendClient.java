package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageResult;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/** Integra a etapa analyze exclusivamente aos contratos pending e callbacks do backend. */
@Component
public class ReferenceAnalysisBackendClient {
    private static final Logger log = LoggerFactory.getLogger(ReferenceAnalysisBackendClient.class);
    private static final String BASE_PATH = "/api/internal/sales-videos/reference-analysis/v1/analyze/stage-executions";
    private static final ParameterizedTypeReference<List<ReferenceAnalysisStageContext>> PENDING_TYPE =
            new ParameterizedTypeReference<>() { };
    private final VideoManagementProperties properties;
    private final WebClient backend;

    /** Configura o cliente com o backend canônico, sem acesso direto ao banco. */
    public ReferenceAnalysisBackendClient(VideoManagementProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.backend = builder.baseUrl(properties.getBackendBaseUrl().toString()).build();
    }

    /** Busca no máximo uma execução pelo endpoint pending canônico. */
    public List<ReferenceAnalysisStageContext> pending() {
        String url = BASE_PATH + "/pending";
        try {
            log.info("Request backend análise de referência; url={} workerId={}", url,
                    properties.getReferenceAnalysis().getWorkerId());
            List<ReferenceAnalysisStageContext> response = authorized(backend.get()
                    .uri(uri -> uri.path(url)
                            .queryParam("workerId", properties.getReferenceAnalysis().getWorkerId())
                            .queryParam("budgetLimitUsd",
                                    properties.getReferenceAnalysis().getBudgetLimitUsd())
                            .queryParam("reservationUsd",
                                    properties.getReferenceAnalysis().getReservationUsd())
                            .build()))
                    .retrieve().bodyToMono(PENDING_TYPE).block();
            log.info("Response backend análise de referência; url={} quantidade={}", url,
                    response == null ? 0 : response.size());
            return response == null ? List.of() : response;
        } catch (RuntimeException ex) {
            log.error("Falha ao buscar análise de referência; url={} workerId={}", url,
                    properties.getReferenceAnalysis().getWorkerId(), ex);
            throw ex;
        }
    }

    /** Reporta sucesso com resultado, artefatos e auditoria da interação de IA. */
    public void complete(ReferenceAnalysisStageContext context, ReferenceAnalysisStageResult result) {
        String url = BASE_PATH + "/" + context.executionId() + "/complete";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("producerExecutionId", context.producerExecutionId());
        payload.put("summaryMarkdown", result.summaryMarkdown());
        payload.put("output", result.output());
        payload.put("artifacts", result.artifacts());
        payload.put("rawRequest", result.rawRequest());
        payload.put("rawResponse", result.rawResponse());
        payload.put("model", result.model());
        payload.put("inputTokens", result.inputTokens());
        payload.put("cachedInputTokens", result.cachedInputTokens());
        payload.put("outputTokens", result.outputTokens());
        payload.put("costUsd", result.costUsd());
        payload.put("decision", result.decision());
        post(url, payload, context.executionId(), "concluir");
    }

    /** Reporta falha completa antes de liberar a execução para retry explícito. */
    public void fail(ReferenceAnalysisStageContext context, RuntimeException error) {
        String url = BASE_PATH + "/" + context.executionId() + "/fail";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("producerExecutionId", context.producerExecutionId());
        payload.put("error", safeError(error));
        if (error instanceof ReferenceAnalysisFailureException failure) {
            payload.put("artifacts", failure.artifacts());
            payload.put("rawRequest", failure.rawRequest());
            payload.put("rawResponse", failure.rawResponse());
            payload.put("model", failure.model());
        }
        post(url, payload, context.executionId(), "falhar");
    }

    /** Envia callback auditável e registra URL, execução e resposta sem expor credenciais. */
    private void post(String url, Object payload, Long executionId, String operation) {
        try {
            log.info("Request backend análise de referência; operação={} executionId={} url={}",
                    operation, executionId, url);
            JsonNode response = authorized(backend.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)).retrieve().bodyToMono(JsonNode.class).block();
            log.info("Response backend análise de referência; operação={} executionId={} url={} response={}",
                    operation, executionId, url, response);
        } catch (RuntimeException ex) {
            log.error("Falha no callback da análise de referência; operação={} executionId={} url={}",
                    operation, executionId, url, ex);
            throw ex;
        }
    }

    /** Aplica autenticação interna quando configurada para o executor. */
    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        if (StringUtils.hasText(properties.getAuthToken())) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthToken());
        }
        return request;
    }

    /** Limita a mensagem funcional sem perder a stack trace registrada localmente. */
    private String safeError(RuntimeException error) {
        String message = error.getMessage();
        if (!StringUtils.hasText(message)) {
            return error.getClass().getSimpleName();
        }
        return message.length() > 4000 ? message.substring(0, 4000) : message;
    }
}
