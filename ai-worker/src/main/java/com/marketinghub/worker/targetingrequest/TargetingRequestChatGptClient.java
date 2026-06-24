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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cliente responsável por gerar candidatos iniciais de targeting via OpenAI para posterior validação oficial na Meta.
 */
@Component
public class TargetingRequestChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestChatGptClient.class);
    private static final String DOMAIN = "TARGETING_REQUEST";
    private static final String RESPONSES_ENDPOINT = "/responses";
    private static final String SERVICE_TIER_FLEX = "flex";
    private static final String PROMPT_PATH = "prompts/targetingrequest/targeting-request-v1.md";
    private static final int MAX_SEED_WORDS = 4;
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern LOCATION_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[\\p{L}\\s]{2,}$");
    private static final Pattern STATE_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[A-Z]{2}$");
    private static final Pattern PARENTHESIS_SUFFIX_PATTERN = Pattern.compile("\\s*\\([^)]*\\)$");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiGenerationRecorder generationRecorder;
    private final String targetingModel;
    private final Duration batchTimeout;
    private final Duration batchPollInterval;

    public TargetingRequestChatGptClient(WebClient.Builder builder,
                                         ObjectMapper objectMapper,
                                         AiGenerationRecorder generationRecorder,
                                         @Value("${openai.api-key:}") String apiKey,
                                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${openai.targeting-request-model:gpt-5.5}") String targetingModel,
                                         @Value("${openai.batch-timeout:PT5M}") Duration batchTimeout,
                                         @Value("${openai.batch-poll-interval:PT0.5S}") Duration batchPollInterval) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(targetingModel)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.targetingModel = targetingModel;
        this.batchTimeout = batchTimeout != null ? batchTimeout : Duration.ofMinutes(5);
        this.batchPollInterval = batchPollInterval != null ? batchPollInterval : Duration.ofMillis(500);
    }

    /**
     * Gera candidatos de targeting a partir da solicitação recebida do backend.
     */
    public List<TargetingCandidateSuggestion> generateCandidates(TargetingRequestDto request) {
        if (request == null) {
            return List.of();
        }
        String prompt = buildPrompt(request);
        RequestContext context = buildContext(request, prompt);
        Map<String, RequestContext> contexts = Map.of(context.customId(), context);
        Map<String, OpenAiResponse> responses = executeFlexRequests(contexts);
        OpenAiResponse response = responses.get(context.customId());
        if (response == null) {
            log.warn("OpenAI Flex returned no response for targeting request {}", request.id());
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
                targetingModel,
                response.usage());
        try {
            return parseContent(content);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response for request {}: {}", request.id(), content, e);
            throw new IllegalStateException("Unable to parse OpenAI response", e);
        }
    }

    /**
     * Monta o contexto de batch com payload Responses API em modo flex.
     */
    private RequestContext buildContext(TargetingRequestDto request, String prompt) {
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um planejador de mídia especialista em Meta Ads."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", targetingModel);
        payload.put("input", input);
        payload.put("service_tier", SERVICE_TIER_FLEX);
        if (OpenAiRequestUtils.supportsTemperature(targetingModel)) {
            payload.put("temperature", 0.4);
        }
        OpenAiRequestUtils.maybeAddReasoning(payload, targetingModel);
        String customId = request.id() != null ? "targeting-request-" + request.id() : "targeting-request-" + UUID.randomUUID();
        log.info("Executando targeting request {} no modo Flex com modelo {}", customId, targetingModel);
        return new RequestContext(customId, request, prompt, payload, targetingModel);
    }

    /**
     * Executa chamadas diretas na Responses API em modo Flex e indexa por customId.
     */
    private Map<String, OpenAiResponse> executeFlexRequests(Map<String, RequestContext> contexts) {
        if (contexts.isEmpty()) {
            return Map.of();
        }
        Map<String, OpenAiResponse> responses = new LinkedHashMap<>();
        for (Map.Entry<String, RequestContext> entry : contexts.entrySet()) {
            OpenAiResponse response = webClient.post()
                    .uri(RESPONSES_ENDPOINT)
                    .bodyValue(entry.getValue().payload())
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block();
            if (response != null) {
                responses.put(entry.getKey(), response);
            }
        }
        return responses;
    }

    /**
     * Carrega o prompt versionado e injeta os dados operacionais da solicitação.
     */
    private String buildPrompt(TargetingRequestDto request) {
        String template = loadPromptTemplate();
        return template
                .replace("{{locale}}", request.localeOrDefault())
                .replace("{{country}}", request.countryOrDefault())
                .replace("{{descricao}}", request.descricao() != null ? request.descricao() : "");
    }

    /**
     * Lê o prompt de targeting do classpath para evitar instruções longas hardcoded.
     */
    private String loadPromptTemplate() {
        ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar prompt de targeting no caminho {}", PROMPT_PATH, ex);
            throw new IllegalStateException("Não foi possível carregar o prompt de targeting", ex);
        }
    }

    /**
     * Interpreta o JSON retornado pela OpenAI e monta sugestões normalizadas.
     */
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
            TargetingCandidateType tipo = TargetingCandidateType.from(readText(node, "tipo"));
            BigDecimal score = readDecimal(node, "score");
            if (score != null && score.compareTo(BigDecimal.valueOf(0.75)) < 0) {
                log.info("Descartando seed {} por baixa aderência comercial de IA: {}", seed, score);
                continue;
            }
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
                    List.of(seed),
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

    /**
     * Remove cercas de markdown para permitir leitura do JSON retornado.
     */
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

    /**
     * Lê campo textual de um nó JSON quando ele existe.
     */
    private String readText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    /**
     * Lê campo decimal de um nó JSON aceitando número ou texto numérico.
     */
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

    /**
     * Normaliza o seed removendo localidade, sufixos e excesso de palavras.
     */
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

    /**
     * Limita um texto à quantidade máxima de palavras permitida para seed.
     */
    private String limitWords(String value, int maxWords) {
        String[] tokens = MULTIPLE_SPACES.matcher(value).replaceAll(" ").trim().split(" ");
        if (tokens.length <= maxWords) {
            return String.join(" ", tokens);
        }
        return String.join(" ", Arrays.copyOf(tokens, maxWords));
    }

    /**
     * Retorna o primeiro texto preenchido entre os valores recebidos.
     */
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

    private record RequestContext(String customId,
                                  TargetingRequestDto request,
                                  String prompt,
                                  Map<String, Object> payload,
                                  String model) {
    }

}
