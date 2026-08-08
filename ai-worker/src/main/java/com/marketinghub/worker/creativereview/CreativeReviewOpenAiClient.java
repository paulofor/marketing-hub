package com.marketinghub.worker.creativereview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: executar a avaliação multimodal versionada dos anúncios na OpenAI. */
@Component
public class CreativeReviewOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(CreativeReviewOpenAiClient.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String prompt;
    private final Object schema;

    /** Inicializa o cliente, carregando integralmente prompt e schema versionados do classpath. */
    public CreativeReviewOpenAiClient(WebClient.Builder builder, ObjectMapper objectMapper,
                                      @Value("${openai.api-key:}") String apiKey,
                                      @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                      @Value("${creative-review.worker.model:gpt-5.5}") String model) {
        this.webClient = builder.baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.prompt = load("prompts/creative-review/ad-specialist-v1.md");
        try {
            this.schema = objectMapper.readValue(load("prompts/creative-review/ad-specialist-v1-schema.json"), Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Schema do agente de anúncios é inválido", ex);
        }
    }

    /** Avalia visual e contexto comercial e devolve payload, resposta bruta e parecer validado. */
    public ReviewExecution review(Map<String, Object> creative) {
        String mediaUrl = text(creative.get("mediaUrl"));
        if (mediaUrl.isBlank()) {
            throw new IllegalArgumentException("Criativo sem mídia pública para revisão multimodal");
        }
        String renderedPrompt = prompt.replace("{{context}}", toJson(creative));
        Map<String, Object> format = Map.of("type", "json_schema", "name", "creative_ad_review_v1", "schema", schema, "strict", true);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("service_tier", "flex");
        payload.put("input", List.of(Map.of("role", "user", "content", List.of(
                Map.of("type", "input_text", "text", renderedPrompt),
                Map.of("type", "input_image", "image_url", mediaUrl, "detail", "high")))));
        payload.put("text", Map.of("format", format));
        OpenAiRequestUtils.maybeAddReasoning(payload, model);
        String requestJson = toJson(payload);
        log.info("Enviando revisão multimodal de anúncio. creativeId={} request={}", creative.get("creativeId"), requestJson);
        OpenAiResponse response = webClient.post().uri("/responses").bodyValue(payload).retrieve()
                .bodyToMono(OpenAiResponse.class).block();
        String responseJson = toJson(response);
        log.info("Resposta bruta da revisão multimodal. creativeId={} response={}", creative.get("creativeId"), responseJson);
        if (response == null || response.firstText() == null) {
            throw new IllegalStateException("OpenAI não retornou parecer do anúncio");
        }
        try {
            JsonNode result = objectMapper.readTree(response.firstText());
            return new ReviewExecution(model, requestJson, responseJson, result,
                    response.usage() != null ? response.usage().effectiveInputTokens() : null,
                    response.usage() != null ? response.usage().effectiveOutputTokens() : null,
                    OpenAiCostEstimator.estimateUsd(model, response.usage()));
        } catch (JsonProcessingException ex) {
            log.error("Falha ao interpretar parecer do anúncio. creativeId={}", creative.get("creativeId"), ex);
            throw new IllegalStateException("Parecer do anúncio não respeitou o schema", ex);
        }
    }

    /** Converte qualquer contexto ou auditoria em JSON. */
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Falha ao serializar revisão do anúncio", ex); }
    }

    /** Normaliza um campo textual opcional. */
    private String text(Object value) { return value == null ? "" : value.toString().trim(); }

    /** Carrega um recurso versionado do classpath. */
    private String load(String path) {
        try { return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8); }
        catch (IOException ex) { throw new IllegalStateException("Falha ao carregar recurso " + path, ex); }
    }

    /** Resultado técnico e funcional preservado para auditoria no backend. */
    public record ReviewExecution(String model, String requestJson, String responseJson, JsonNode result,
                                  Integer inputTokens, Integer outputTokens, java.math.BigDecimal costUsd) {}
}
