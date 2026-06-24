package com.marketinghub.worker.creative;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;

/**
 * Responsabilidade: gerar textos de criativos via OpenAI em modo Flex e registrar auditoria da geração.
 */
@Component
public class CreativeChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final AiGenerationRecorder generationRecorder;
    private final Duration batchPollInterval;
    private final Duration batchTimeout;
    private static final Logger log = LoggerFactory.getLogger(CreativeChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String DOMAIN = "CREATIVE_COPY";
    private static final String RESPONSES_ENDPOINT = "/responses";
    private static final String SERVICE_TIER_FLEX = "flex";
    private static final Duration DEFAULT_BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration DEFAULT_BATCH_TIMEOUT = Duration.ofMinutes(5);
    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "expired", "cancelled");

    public CreativeChatGptClient(WebClient.Builder builder,
                                 ObjectMapper objectMapper,
                                 @Value("${openai.api-key:}") String apiKey,
                                 @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                 @Value("${openai.model:gpt-3.5-turbo}") String model,
                                 @Value("${openai.batch-poll-interval:PT0.5S}") Duration batchPollInterval,
                                 @Value("${openai.batch-timeout:PT5M}") Duration batchTimeout,
                                 AiGenerationRecorder generationRecorder) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(REQUEST_TIMEOUT)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int) REQUEST_TIMEOUT.getSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler((int) REQUEST_TIMEOUT.getSeconds())));
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            if (OpenAiRequestUtils.requiresReasoning(model)) {
                clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
            }
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.generationRecorder = generationRecorder;
        this.batchPollInterval = normalizeDuration(batchPollInterval, DEFAULT_BATCH_POLL_INTERVAL);
        this.batchTimeout = normalizeDuration(batchTimeout, DEFAULT_BATCH_TIMEOUT);
        if (!enabled) {
            log.warn("OpenAI API key not configured; creative generation will be skipped");
        }
    }

    /** Gera criativos textuais para um experimento usando chamada direta na Responses API em modo Flex. */
    public Generation generateCreatives(Experiment experiment, int quantity) {
        if (!enabled) {
            log.warn("Skipping creative generation for experiment {} because OpenAI API key is missing", experiment != null ? experiment.getId() : "unknown");
            return Generation.empty();
        }
        String prompt = buildPrompt(experiment, quantity);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em marketing."),
                OpenAiRequestUtils.message("user", prompt)
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        payload.put("service_tier", SERVICE_TIER_FLEX);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        String customId = buildCustomId(experiment);
        Map<String, RequestContext> contexts = new LinkedHashMap<>();
        contexts.put(customId, new RequestContext(experiment, prompt, payload, model));

        log.info("Executando prompt de criativos {} no modo Flex da OpenAI", experiment != null ? experiment.getId() : "sem-id");
        Map<String, OpenAiResponse> responses = executeFlexRequests(contexts);
        OpenAiResponse response = responses.get(customId);
        if (response == null) {
            log.warn("OpenAI Flex returned no response for experiment {}", experiment != null ? experiment.getId() : "unknown");
            return Generation.empty();
        }
        if (response.hasError()) {
            throw new RuntimeException("OpenAI error: " + response.errorMessage());
        }
        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                experiment != null ? String.valueOf(experiment.getId()) : null,
                prompt,
                content,
                model,
                response.usage());
        BigDecimal totalCostUsd = OpenAiCostEstimator.estimateUsd(model, response.usage());
        if (!hasText(content)) {
            log.warn("ChatGPT returned empty content for experiment {}", experiment != null ? experiment.getId() : "unknown");
            return Generation.empty(totalCostUsd);
        }
        log.info("ChatGPT content: {}", content);
        try {
            return parseWithCost(content, totalCostUsd);
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", content, e);
            try {
                String unescaped = content.replace("\\\"", "\"");
                return parseWithCost(unescaped, totalCostUsd);
            } catch (Exception ex) {
                log.error("Failed to parse unescaped ChatGPT response: {}", content, ex);
                throw new RuntimeException("Failed to parse ChatGPT response", ex);
            }
        }
    }

    private Generation parseWithCost(String content, BigDecimal totalCostUsd) throws Exception {
        List<CreateCreativeRequest> parsed = parseContent(content);
        BigDecimal costPerCreative = calculateCostPerCreative(totalCostUsd, parsed.size());
        if (costPerCreative != null) {
            parsed.forEach(req -> applyCostUsd(req, costPerCreative));
        }
        log.info("Parsed creatives: {}", parsed);
        return new Generation(parsed, totalCostUsd, costPerCreative);
    }

    private String buildCustomId(Experiment experiment) {
        if (experiment != null && experiment.getId() != null) {
            return "experiment-" + experiment.getId();
        }
        return "experiment-" + System.nanoTime();
    }

    /** Executa os contextos de criativo diretamente na Responses API com service_tier flex. */
    private Map<String, OpenAiResponse> executeFlexRequests(Map<String, RequestContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return Map.of();
        }
        Map<String, OpenAiResponse> responses = new LinkedHashMap<>();
        for (Map.Entry<String, RequestContext> entry : contexts.entrySet()) {
            OpenAiResponse response = webClient.post()
                    .uri(RESPONSES_ENDPOINT)
                    .bodyValue(entry.getValue().payload())
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response != null) {
                responses.put(entry.getKey(), response);
            }
        }
        return responses;
    }

    private BigDecimal calculateCostPerCreative(BigDecimal totalCostUsd, int totalCreatives) {
        if (totalCostUsd == null || totalCreatives <= 0) {
            return null;
        }
        return totalCostUsd.divide(BigDecimal.valueOf(totalCreatives), 4, RoundingMode.HALF_UP);
    }

    private void applyCostUsd(CreateCreativeRequest request, BigDecimal costPerCreative) {
        if (costPerCreative == null || request == null) {
            return;
        }
        try {
            Method method = request.getClass().getMethod("setCostUsd", BigDecimal.class);
            method.invoke(request, costPerCreative);
        } catch (NoSuchMethodException e) {
            log.debug("CreateCreativeRequest does not expose setCostUsd; skipping cost attribution");
        } catch (ReflectiveOperationException e) {
            log.warn("Failed to set costUsd on CreateCreativeRequest", e);
        }
    }

    private String buildPrompt(Experiment experiment, int quantity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere ").append(quantity).append(" criativos em formato JSON. ");
        String customPrompt = experiment != null ? experiment.getCreativeTextPrompt() : null;
        if (hasText(customPrompt)) {
            sb.append(applyTextPromptTemplate(experiment, quantity)).append(' ');
        } else {
            Hypothesis h = experiment != null ? experiment.getHypothesisRef() : null;
            if (h != null) {
                sb.append("Use a seguinte hipótese como contexto:\n");
                if (hasText(h.getTitle())) sb.append("Título: ").append(h.getTitle()).append("\n");
                if (hasText(h.getPromise())) sb.append("Promessa: ").append(h.getPromise()).append("\n");
                if (hasText(h.getProblem())) sb.append("Problema: ").append(h.getProblem()).append("\n");
                if (hasText(h.getPersona())) sb.append("Persona: ").append(h.getPersona()).append("\n");
                if (hasText(h.getMechanism())) sb.append("Mecanismo: ").append(h.getMechanism()).append("\n");
                if (hasText(h.getUniqueMechanism())) sb.append("Mecanismo único: ").append(h.getUniqueMechanism()).append("\n");
                if (hasText(h.getEntrega())) sb.append("Entrega: ").append(h.getEntrega()).append("\n");
                if (hasText(h.getSuccessRule())) sb.append("Regra de sucesso: ").append(h.getSuccessRule()).append("\n");
                if (h.getOfferType() != null) sb.append("Tipo de oferta: ").append(h.getOfferType()).append("\n");
                if (h.getPrice() != null) sb.append("Preço: ").append(h.getPrice()).append("\n");
            }
        }
        sb.append("Cada objeto deve conter as chaves: \"headline\" (máximo 40 caracteres), ");
        sb.append("\"primaryText\" (máximo 125 caracteres e até 30 hashtags). ");
        sb.append("Retorne apenas um array JSON com esses objetos, sem texto adicional.");
        return sb.toString();
    }

    private String applyTextPromptTemplate(Experiment experiment, int quantity) {
        Map<String, String> placeholders = buildCommonPlaceholders(experiment);
        placeholders.put("quantity", String.valueOf(quantity));
        return replacePlaceholders(experiment != null ? experiment.getCreativeTextPrompt() : null, placeholders);
    }

    private Map<String, String> buildCommonPlaceholders(Experiment experiment) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("experimentId", experiment != null && experiment.getId() != null ? String.valueOf(experiment.getId()) : "");
        placeholders.put("experimentName", experiment != null ? safe(experiment.getName()) : "");
        Hypothesis h = experiment != null ? experiment.getHypothesisRef() : null;
        placeholders.put("hypothesisId", h != null && h.getId() != null ? h.getId().toString() : "");
        placeholders.put("hypothesisTitle", h != null ? safe(h.getTitle()) : "");
        placeholders.put("persona", h != null ? safe(h.getPersona()) : "");
        placeholders.put("problem", h != null ? safe(h.getProblem()) : "");
        placeholders.put("promise", h != null ? safe(h.getPromise()) : "");
        placeholders.put("mechanism", h != null ? safe(h.getMechanism()) : "");
        placeholders.put("uniqueMechanism", h != null ? safe(h.getUniqueMechanism()) : "");
        placeholders.put("entrega", h != null ? safe(h.getEntrega()) : "");
        placeholders.put("successRule", h != null ? safe(h.getSuccessRule()) : "");
        placeholders.put("offerType", h != null && h.getOfferType() != null ? h.getOfferType().name() : "");
        placeholders.put("price", h != null && h.getPrice() != null ? h.getPrice().toPlainString() : "");
        return placeholders;
    }

    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            result = result.replace(token, entry.getValue());
        }
        return result;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Duration normalizeDuration(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }

    private List<CreateCreativeRequest> parseContent(String content) throws Exception {
        CreateCreativeRequest[] arr = objectMapper.readValue(content, CreateCreativeRequest[].class);
        for (CreateCreativeRequest req : arr) {
            if (req.getStatus() == null) {
                req.setStatus(CreativeStatus.DRAFT);
            }
        }
        return Arrays.asList(arr);
    }

    private record RequestContext(Experiment experiment,
                                  String prompt,
                                  Map<String, Object> payload,
                                  String model) {}

    public record Generation(List<CreateCreativeRequest> creatives,
                             BigDecimal totalCostUsd,
                             BigDecimal costPerCreativeUsd) {
        public Generation {
            creatives = creatives == null ? List.of() : List.copyOf(creatives);
        }

        public static Generation empty() {
            return new Generation(List.of(), null, null);
        }

        public static Generation empty(BigDecimal totalCostUsd) {
            return new Generation(List.of(), totalCostUsd, null);
        }
    }
}
