package com.marketinghub.worker.targetingrequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class TargetingRequestChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestChatGptClient.class);
    private static final String DOMAIN = "TARGETING_REQUEST";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiGenerationRecorder generationRecorder;
    private final String defaultModel;

    public TargetingRequestChatGptClient(WebClient.Builder builder,
                                         ObjectMapper objectMapper,
                                         AiGenerationRecorder generationRecorder,
                                         @Value("${openai.api-key:}") String apiKey,
                                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${openai.model:gpt-3.5-turbo}") String defaultModel) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.defaultModel = defaultModel;
    }

    public List<TargetingCandidateSuggestion> generateCandidates(TargetingRequestDto request) {
        String prompt = buildPrompt(request);
        Map<String, Object> payload = Map.of(
                "model", defaultModel,
                "messages", List.of(
                        OpenAiRequestUtils.message("system", "Você é um planejador de mídia especialista em Meta Ads."),
                        OpenAiRequestUtils.message("user", prompt)
                ),
                "temperature", 0.4
        );

        OpenAiResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();
        if (response == null) {
            log.warn("OpenAI returned null response for targeting request {}", request.id());
            return List.of();
        }
        if (response.hasError()) {
            throw new IllegalStateException("OpenAI error: " + response.errorMessage());
        }
        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                request.id() != null ? request.id().toString() : null,
                prompt,
                content,
                defaultModel,
                response.usage());
        try {
            return parseContent(content);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response for request {}: {}", request.id(), content, e);
            throw new IllegalStateException("Unable to parse OpenAI response", e);
        }
    }

    private String buildPrompt(TargetingRequestDto request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere listas de candidatos de targeting do Facebook (interests, behaviors, work_position) para a descrição a seguir.\n");
        sb.append("Respeite o idioma preferencial ").append(request.localeOrDefault()).append(" e país ").append(request.countryOrDefault()).append(".\n");
        sb.append("Tipo de público: ").append(request.audienceOrDefault()).append(".\n");
        sb.append("Crie no máximo 30 itens por tipo, priorizando diversidade sem variações triviais.\n");
        sb.append("Cada item precisa ter: texto_sugerido, tipo (interest|behavior|work_position), rationale, score (0-1), intent_tag (awareness|consideration|decision) e idioma.\n");
        sb.append("Bloqueie PII, termos proibidos da Meta e garanta que os termos existam na API de targeting search.\n");
        sb.append("Responda apenas em JSON seguindo o formato {\"candidates\":[{...}]}.\n");
        sb.append("Descrição: ").append(request.descricao());
        return sb.toString();
    }

    private List<TargetingCandidateSuggestion> parseContent(String content) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        JsonNode candidatesNode = root.get("candidates");
        if (candidatesNode == null) {
            candidatesNode = root;
        }
        List<TargetingCandidateSuggestion> result = new ArrayList<>();
        if (candidatesNode == null) {
            return result;
        }
        Iterator<JsonNode> iterator = candidatesNode.isArray() ? candidatesNode.iterator() : List.of(candidatesNode).iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String texto = readText(node, "texto_sugerido");
            if (!StringUtils.hasText(texto)) {
                continue;
            }
            TargetingCandidateType tipo = TargetingCandidateType.from(readText(node, "tipo"));
            BigDecimal score = readDecimal(node, "score");
            String rationale = readText(node, "rationale");
            String idioma = readText(node, "idioma");
            String intent = readText(node, "intent_tag");
            result.add(new TargetingCandidateSuggestion(texto.trim(), tipo, "AI", score, rationale, idioma, intent));
        }
        return result;
    }

    private String readText(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private BigDecimal readDecimal(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.get(field);
        if (value != null && value.isNumber()) {
            return value.decimalValue();
        }
        if (value != null && value.isTextual()) {
            try {
                return new BigDecimal(value.asText());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
