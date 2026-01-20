package com.marketinghub.worker.creative;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;

/**
 * Simple wrapper around the OpenAI chat completions API for creative generation.
 */
@Component
public class CreativeChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final AiGenerationRecorder generationRecorder;
    private static final Logger log = LoggerFactory.getLogger(CreativeChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String DOMAIN = "CREATIVE_COPY";

    public CreativeChatGptClient(WebClient.Builder builder,
                                 ObjectMapper objectMapper,
                                 @Value("${openai.api-key:}") String apiKey,
                                 @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                 @Value("${openai.model:gpt-3.5-turbo}") String model,
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
        if (!enabled) {
            log.warn("OpenAI API key not configured; creative generation will be skipped");
        }
    }

    public Generation generateCreatives(Experiment experiment, int quantity) {
        if (!enabled) {
            log.warn("Skipping creative generation for experiment {} because OpenAI API key is missing", experiment.getId());
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
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Sending prompt to ChatGPT for experiment {}: {}", experiment.getId(), prompt);
        log.debug("ChatGPT payload: {}", payload);

        OpenAiResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block(REQUEST_TIMEOUT);
        } catch (Exception ex) {
            log.error("ChatGPT request failed for experiment {} after {} seconds", experiment.getId(),
                    REQUEST_TIMEOUT.getSeconds(), ex);
            throw new RuntimeException("Failed to call ChatGPT", ex);
        }

        log.info("ChatGPT raw response: {}", response);

        if (response == null) {
            log.warn("ChatGPT returned no choices for experiment {}", experiment.getId());
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
        if (content == null || content.isBlank()) {
            log.warn("ChatGPT returned empty content for experiment {}", experiment.getId());
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
        Hypothesis h = experiment.getHypothesisRef();
        if (h != null) {
            sb.append("Use a seguinte hipótese como contexto:\n");
            sb.append("Título: ").append(h.getTitle()).append("\n");
            if (h.getPromise() != null) sb.append("Promessa: ").append(h.getPromise()).append("\n");
            if (h.getProblem() != null) sb.append("Problema: ").append(h.getProblem()).append("\n");
            if (h.getPersona() != null) sb.append("Persona: ").append(h.getPersona()).append("\n");
            if (h.getMechanism() != null) sb.append("Mecanismo: ").append(h.getMechanism()).append("\n");
            if (h.getUniqueMechanism() != null) sb.append("Mecanismo único: ").append(h.getUniqueMechanism()).append("\n");
            if (h.getEntrega() != null) sb.append("Entrega: ").append(h.getEntrega()).append("\n");
            if (h.getSuccessRule() != null) sb.append("Regra de sucesso: ").append(h.getSuccessRule()).append("\n");
            if (h.getOfferType() != null) sb.append("Tipo de oferta: ").append(h.getOfferType()).append("\n");
            if (h.getPrice() != null) sb.append("Preço: ").append(h.getPrice()).append("\n");
        }
        sb.append("Cada objeto deve conter as chaves: \"headline\" (máximo 40 caracteres), ");
        sb.append("\"primaryText\" (máximo 125 caracteres e até 30 hashtags). ");
        sb.append("Retorne apenas um array JSON com esses objetos, sem texto adicional.");
        return sb.toString();
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
