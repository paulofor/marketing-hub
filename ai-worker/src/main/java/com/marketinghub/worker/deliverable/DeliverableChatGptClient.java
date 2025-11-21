package com.marketinghub.worker.deliverable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiApiKeyProvider;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Wrapper around the OpenAI Responses API focused on deliverable generation.
 */
@Component
public class DeliverableChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(DeliverableChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String DOMAIN = "EXPERIMENT_DELIVERABLE";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final AiGenerationRecorder generationRecorder;

    public DeliverableChatGptClient(WebClient.Builder builder,
                                    ObjectMapper objectMapper,
                                    OpenAiApiKeyProvider apiKeyProvider,
                                    @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                    @Value("${openai.model:gpt-3.5-turbo}") String model,
                                    AiGenerationRecorder generationRecorder) {
        this.enabled = apiKeyProvider.isConfigured();
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
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKeyProvider.getApiKey());
            if (OpenAiRequestUtils.requiresReasoning(model)) {
                clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
            }
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.generationRecorder = generationRecorder;
        if (!enabled) {
            log.warn("OpenAI API key not configured; deliverable generation will be skipped");
        }
    }

    public List<CreateDeliverableRequest> generateDeliverables(Experiment experiment, int quantity) {
        if (!enabled) {
            log.warn("Skipping deliverable generation for experiment {} because OpenAI API key is missing",
                    experiment != null ? experiment.getId() : null);
            return List.of();
        }
        String prompt = buildPrompt(experiment, quantity);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um estrategista de marketing."),
                OpenAiRequestUtils.message("user", prompt)
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        Long experimentId = experiment != null ? experiment.getId() : null;
        log.info("Sending prompt to ChatGPT for experiment {}: {}", experimentId, prompt);
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
            log.error("ChatGPT request failed for experiment {} after {} seconds", experimentId,
                    REQUEST_TIMEOUT.getSeconds(), ex);
            throw new RuntimeException("Failed to call ChatGPT", ex);
        }

        log.info("ChatGPT raw response: {}", response);

        if (response == null) {
            log.warn("ChatGPT returned no choices for experiment {}", experimentId);
            return List.of();
        }
        if (response.hasError()) {
            throw new RuntimeException("OpenAI error: " + response.errorMessage());
        }
        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                experimentId != null ? String.valueOf(experimentId) : null,
                prompt,
                content,
                model,
                response.usage());
        if (!StringUtils.hasText(content)) {
            log.warn("ChatGPT returned empty content for experiment {}", experimentId);
            return List.of();
        }
        log.info("ChatGPT content: {}", content);
        String sanitized = stripCodeFence(content);
        try {
            List<CreateDeliverableRequest> parsed = parseContent(sanitized, prompt);
            log.info("Parsed deliverables: {}", parsed);
            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", sanitized, e);
            try {
                String unescaped = sanitized.replace("\\\"", "\"");
                List<CreateDeliverableRequest> parsed = parseContent(unescaped, prompt);
                log.info("Parsed deliverables after unescaping: {}", parsed);
                return parsed;
            } catch (Exception ex) {
                log.error("Failed to parse unescaped ChatGPT response: {}", sanitized, ex);
                throw new RuntimeException("Failed to parse ChatGPT response", ex);
            }
        }
    }

    private String buildPrompt(Experiment experiment, int quantity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere ").append(quantity).append(" entregáveis em formato JSON que funcionem como iscas digitais. ");
        sb.append("Cada entregável deve ser um brinde digital oferecido ao lead por ter compartilhado dados, entregando valor rápido e ajudando o time a executar o experimento descrito. ");
        if (experiment != null) {
            sb.append("\nContexto do experimento:\n");
            if (StringUtils.hasText(experiment.getName())) {
                sb.append("Nome: ").append(experiment.getName()).append("\n");
            } else if (experiment.getId() != null) {
                sb.append("ID: ").append(experiment.getId()).append("\n");
            }
            if (StringUtils.hasText(experiment.getHypothesis())) {
                sb.append("Resumo: ").append(experiment.getHypothesis()).append("\n");
            }
            if (experiment.getPlatform() != null) {
                sb.append("Plataforma: ").append(experiment.getPlatform()).append("\n");
            }
            if (experiment.getStatus() != null) {
                sb.append("Status: ").append(experiment.getStatus()).append("\n");
            }
        }
        MarketNiche niche = experiment != null ? experiment.getNiche() : null;
        if (niche != null) {
            sb.append("\nNicho:\n");
            if (StringUtils.hasText(niche.getName())) {
                sb.append("Nome: ").append(niche.getName()).append("\n");
            }
            if (StringUtils.hasText(niche.getDescription())) {
                sb.append("Descrição: ").append(niche.getDescription()).append("\n");
            }
            if (StringUtils.hasText(niche.getBaseSegmentation())) {
                sb.append("Segmentação base: ").append(niche.getBaseSegmentation()).append("\n");
            }
            if (StringUtils.hasText(niche.getInterests())) {
                sb.append("Interesses: ").append(niche.getInterests()).append("\n");
            }
            if (StringUtils.hasText(niche.getDemographicFilters())) {
                sb.append("Filtros demográficos: ").append(niche.getDemographicFilters()).append("\n");
            }
            if (StringUtils.hasText(niche.getExtraTips())) {
                sb.append("Dicas extras: ").append(niche.getExtraTips()).append("\n");
            }
        }
        Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
        if (hypothesis != null) {
            sb.append("\nHipótese:\n");
            if (StringUtils.hasText(hypothesis.getTitle())) {
                sb.append("Título: ").append(hypothesis.getTitle()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getPromise())) {
                sb.append("Promessa: ").append(hypothesis.getPromise()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getProblem())) {
                sb.append("Problema: ").append(hypothesis.getProblem()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getPersona())) {
                sb.append("Persona: ").append(hypothesis.getPersona()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getMechanism())) {
                sb.append("Mecanismo: ").append(hypothesis.getMechanism()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getUniqueMechanism())) {
                sb.append("Mecanismo único: ").append(hypothesis.getUniqueMechanism()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getEntrega())) {
                sb.append("Entrega: ").append(hypothesis.getEntrega()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getSuccessRule())) {
                sb.append("Regra de sucesso: ").append(hypothesis.getSuccessRule()).append("\n");
            }
            if (hypothesis.getOfferType() != null) {
                sb.append("Tipo de oferta: ").append(hypothesis.getOfferType()).append("\n");
            }
            if (hypothesis.getPrice() != null) {
                sb.append("Preço: ").append(hypothesis.getPrice()).append("\n");
            }
        }
        sb.append("\nCada objeto deve conter as chaves: \"title\", \"description\", \"content\". ");
        sb.append("A descrição deve resumir o objetivo em até 200 caracteres. ");
        sb.append("O campo \"content\" deve trazer instruções detalhadas, listas e passos separados por \\n. ");
        sb.append("Se fizer sentido, indique canais, formatos, scripts ou modelos que possam ser aplicados imediatamente. ");
        sb.append("Retorne apenas um array JSON com esses objetos, sem texto adicional.");
        return sb.toString();
    }

    private List<CreateDeliverableRequest> parseContent(String content, String prompt) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        if (!root.isArray()) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        for (JsonNode node : root) {
            if (node instanceof ObjectNode objectNode) {
                normaliseText(objectNode, "title", " ");
                normaliseText(objectNode, "description", " ");
                normaliseText(objectNode, "content", "\n");
            }
        }
        CreateDeliverableRequest[] arr = objectMapper.treeToValue(root, CreateDeliverableRequest[].class);
        for (CreateDeliverableRequest req : arr) {
            req.setPrompt(prompt);
            req.setModel(model);
        }
        return Arrays.asList(arr);
    }

    private void normaliseText(ObjectNode node, String field, String delimiter) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        String text;
        if (value.isArray()) {
            text = StreamSupport.stream(value.spliterator(), false)
                    .map(JsonNode::asText)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(delimiter));
        } else {
            text = value.asText();
        }
        if (!StringUtils.hasText(text)) {
            node.putNull(field);
        } else {
            node.put(field, text.trim());
        }
    }

    private String stripCodeFence(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak >= 0) {
                trimmed = trimmed.substring(firstLineBreak + 1);
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
