package com.marketinghub.worker.targetingrequest;

import com.fasterxml.jackson.core.json.JsonReadFeature;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class TargetingRequestChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestChatGptClient.class);
    private static final String DOMAIN = "TARGETING_REQUEST";
    private static final int MAX_SEED_WORDS = 4;
    private static final int MAX_VARIANTS = 6;
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern LOCATION_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[\\p{L}\\s]{2,}$");
    private static final Pattern STATE_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[A-Z]{2}$");
    private static final Pattern PARENTHESIS_SUFFIX_PATTERN = Pattern.compile("\\s*\\([^)]*\\)$");

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
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(defaultModel)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.defaultModel = defaultModel;
    }

    public List<TargetingCandidateSuggestion> generateCandidates(TargetingRequestDto request) {
        String prompt = buildPrompt(request);
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("model", defaultModel);
        payload.put("input", List.of(
                OpenAiRequestUtils.message("system", "Você é um planejador de mídia especialista em Meta Ads."),
                OpenAiRequestUtils.message("user", prompt)
        ));
        if (OpenAiRequestUtils.supportsTemperature(defaultModel)) {
            payload.put("temperature", 0.4);
        }
        OpenAiRequestUtils.maybeAddReasoning(payload, defaultModel);

        log.info("Sending OpenAI targeting request {} with model {} and payload: {}",
                request.id(),
                defaultModel,
                safeJson(payload));

        OpenAiResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("OpenAI request failed for targeting request {}. Status: {}, Body: {}, Payload: {}",
                    request.id(),
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    safeJson(payload),
                    ex);
            throw ex;
        }
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
        sb.append("Com base na descrição abaixo, gere candidatos de targeting do Facebook seguindo o fluxo seed-first.\n");
        sb.append("Cada candidato deve conter: seed (1 a 4 palavras, sem localidade), seed_variants (variações singular/plural, sem acentos e versões em inglês quando fizer sentido), tipo (interest|behavior|work_position), rationale, score (0-1), intent_tag e idioma_hint.\n");
        sb.append("Remova qualquer menção geográfica do seed; restrições de país devem ir em constraints.country.\n");
        sb.append("Use o idioma preferencial ").append(request.localeOrDefault()).append(" e país alvo ").append(request.countryOrDefault()).append(".\n");
        sb.append("Evite termos proibidos pela Meta ou qualquer PII. \n");
        sb.append("Retorne JSON puro no formato {\"candidates\":[{...}]} sem comentários ou markdown.\n");
        sb.append("Descrição: ").append(request.descricao());
        return sb.toString();
    }

    private List<TargetingCandidateSuggestion> parseContent(String content) throws Exception {
        String sanitizedContent = sanitizeContent(content);
        ObjectMapper lenientMapper = objectMapper.copy()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature());
        JsonNode root = lenientMapper.readTree(sanitizedContent);
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
            String seed = sanitizeSeed(readText(node, "seed"));
            if (!StringUtils.hasText(seed)) {
                seed = sanitizeSeed(readText(node, "texto_sugerido"));
            }
            if (!StringUtils.hasText(seed)) {
                continue;
            }
            List<String> variants = extractVariants(node.path("seed_variants"), seed);
            TargetingCandidateType tipo = TargetingCandidateType.from(readText(node, "tipo"));
            BigDecimal score = readDecimal(node, "score");
            String rationale = readText(node, "rationale");
            String idiomaHint = firstNonBlank(readText(node, "idioma_hint"), readText(node, "idioma"));
            String intent = readText(node, "intent_tag");
            String country = null;
            JsonNode constraints = node.get("constraints");
            if (constraints != null) {
                country = readText(constraints, "country");
            }
            result.add(new TargetingCandidateSuggestion(
                    seed,
                    variants,
                    tipo != null ? tipo : TargetingCandidateType.INTEREST,
                    "AI",
                    score,
                    rationale,
                    idiomaHint,
                    intent,
                    country
            ));
        }
        return result;
    }

    private List<String> extractVariants(JsonNode node, String seed) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        if (StringUtils.hasText(seed)) {
            variants.add(seed);
            variants.add(removeAccents(seed));
        }
        if (node != null && node.isArray()) {
            node.forEach(element -> {
                String value = sanitizeSeed(element.asText());
                if (StringUtils.hasText(value)) {
                    variants.add(value);
                    variants.add(removeAccents(value));
                }
            });
        }
        variants.removeIf(value -> !StringUtils.hasText(value));
        return variants.stream().limit(MAX_VARIANTS).toList();
    }

    private String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.contains("\n")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String readText(JsonNode node, String field) {
        if (node == null) return null;
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

    private String removeAccents(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String sanitizeSeed(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String collapsed = MULTIPLE_SPACES.matcher(raw).replaceAll(" ").trim();
        collapsed = PARENTHESIS_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = LOCATION_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = STATE_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = collapsed.replaceAll("[\\p{Punct}]+$", "");
        collapsed = collapsed.trim();
        if (!StringUtils.hasText(collapsed)) {
            return null;
        }
        return limitWords(collapsed, MAX_SEED_WORDS);
    }

    private String limitWords(String value, int maxWords) {
        String[] tokens = MULTIPLE_SPACES.matcher(value).replaceAll(" ").trim().split(" ");
        if (tokens.length <= maxWords) {
            return String.join(" ", tokens);
        }
        return String.join(" ", Arrays.copyOf(tokens, maxWords));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String safeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }
}
