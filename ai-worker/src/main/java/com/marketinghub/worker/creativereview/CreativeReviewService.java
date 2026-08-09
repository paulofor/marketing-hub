package com.marketinghub.worker.creativereview;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: processar a etapa independente de revisão e reportar uma decisão por anúncio. */
@Service
public class CreativeReviewService {
    private static final Logger log = LoggerFactory.getLogger(CreativeReviewService.class);
    private final CreativeReviewBackendClient backendClient;
    private final CreativeReviewOpenAiClient openAiClient;

    /** Inicializa a etapa com suas portas de backend e modelo multimodal. */
    public CreativeReviewService(CreativeReviewBackendClient backendClient, CreativeReviewOpenAiClient openAiClient) {
        this.backendClient = backendClient;
        this.openAiClient = openAiClient;
    }

    /** Processa um lote e isola falhas por criativo para manter o gate fechado. */
    public Summary processPending(int limit) {
        int success = 0;
        int failed = 0;
        var pending = backendClient.listPending(limit);
        for (Map<String, Object> creative : pending) {
            Long id = Long.valueOf(creative.get("creativeId").toString());
            try {
                var execution = openAiClient.review(creative);
                backendClient.report(id, resultPayload(execution));
                success++;
            } catch (RuntimeException ex) {
                failed++;
                log.error("Falha na revisão do anúncio. creativeId={}", id, ex);
                backendClient.report(id, Map.of("decision", "FAILED", "error", rootMessage(ex)));
            }
        }
        return new Summary(pending.size(), success, failed);
    }

    /** Converte o JSON validado em contrato de callback com auditoria técnica separada. */
    private Map<String, Object> resultPayload(CreativeReviewOpenAiClient.ReviewExecution execution) {
        JsonNode result = execution.result();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("decision", result.path("decision").asText());
        payload.put("attentionScore", result.path("attentionScore").asInt());
        payload.put("clarityScore", result.path("clarityScore").asInt());
        payload.put("desireScore", result.path("desireScore").asInt());
        payload.put("credibilityScore", result.path("credibilityScore").asInt());
        payload.put("actionScore", result.path("actionScore").asInt());
        payload.put("summary", result.path("summary").asText());
        payload.put("issuesJson", result.path("issues").toString());
        payload.put("recommendationsJson", result.path("recommendations").toString());
        payload.put("revisedHeadline", result.path("revisedHeadline").asText());
        payload.put("revisedPrimaryText", result.path("revisedPrimaryText").asText());
        payload.put("revisedDescription", result.path("revisedDescription").asText());
        payload.put("revisedCta", result.path("revisedCta").asText());
        payload.put("revisedImagePrompt", result.path("revisedImagePrompt").asText());
        payload.put("mandatoryVisualRequirements", jsonArray(result.path("mandatoryVisualRequirements")));
        payload.put("forbiddenVisualElements", jsonArray(result.path("forbiddenVisualElements")));
        payload.put("visualAcceptanceCriteria", jsonArray(result.path("visualAcceptanceCriteria")));
        payload.put("model", execution.model());
        payload.put("requestJson", execution.requestJson());
        payload.put("responseJson", execution.responseJson());
        payload.put("inputTokens", execution.inputTokens());
        payload.put("outputTokens", execution.outputTokens());
        payload.put("costUsd", execution.costUsd());
        return payload;
    }

    /** Converte arrays validados pelo schema em valores JSON nativos do callback. */
    private Object jsonArray(JsonNode node) {
        return node.isArray() ? node : com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    }

    /** Extrai a causa mais específica sem perder o stack trace já registrado. */
    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    /** Resumo operacional do ciclo da etapa. */
    public record Summary(int total, int success, int failed) {}
}
