package com.marketinghub.worker.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.journey.model.Journey;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente responsável por sugerir copy e estrutura dos e-mails da jornada.
 */
@Component
public class ExperimentEmailChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentEmailChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;

    public ExperimentEmailChatGptClient(WebClient.Builder builder,
                                        ObjectMapper objectMapper,
                                        @Value("${openai.api-key:}") String apiKey,
                                        @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                        @Value("${openai.model:gpt-3.5-turbo}") String model) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.enabled = StringUtils.hasText(apiKey);
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
        } else {
            log.warn("OpenAI API key não configurada; geração de e-mails será ignorada");
        }
        this.webClient = clientBuilder.build();
    }

    public Generation generateEmails(Experiment experiment,
                                     Journey journey,
                                     int quantity,
                                     List<StepContext> stepContexts) {
        if (!enabled) {
            return Generation.disabled(model);
        }
        String prompt = buildPrompt(experiment, journey, quantity, stepContexts);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um copywriter sênior de lifecycle marketing e CRM."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Enviando prompt de e-mails para experimento {}: {}", experiment.getId(), prompt);
        OpenAiResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block(REQUEST_TIMEOUT);
        } catch (Exception ex) {
            log.error("Falha ao consultar OpenAI para e-mails do experimento {}", experiment.getId(), ex);
            throw new RuntimeException("Falha ao consultar ChatGPT para e-mails", ex);
        }
        if (response == null) {
            log.warn("OpenAI retornou resposta nula para experimento {}", experiment.getId());
            return Generation.empty(model, prompt, null);
        }
        if (response.hasError()) {
            throw new RuntimeException("Erro OpenAI: " + response.errorMessage());
        }
        String content = response.firstText();
        log.info("Resposta OpenAI para e-mails: {}", content);
        List<EmailPlan> plans = parseContent(content);
        return new Generation(plans, prompt, content, model);
    }

    private List<EmailPlan> parseContent(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        try {
            EmailPlan[] array = objectMapper.readValue(content, EmailPlan[].class);
            List<EmailPlan> result = new ArrayList<>();
            for (EmailPlan plan : array) {
                if (plan != null && plan.stepId() != null && StringUtils.hasText(plan.subject())) {
                    result.add(plan);
                }
            }
            return result;
        } catch (Exception ex) {
            log.error("Falha ao interpretar resposta de e-mails: {}", content, ex);
            try {
                String unescaped = content.replace("\\\"", "\"");
                EmailPlan[] array = objectMapper.readValue(unescaped, EmailPlan[].class);
                List<EmailPlan> result = new ArrayList<>();
                for (EmailPlan plan : array) {
                    if (plan != null && plan.stepId() != null && StringUtils.hasText(plan.subject())) {
                        result.add(plan);
                    }
                }
                return result;
            } catch (Exception retry) {
                log.error("Falha ao interpretar resposta de e-mails após normalização: {}", content, retry);
                throw new RuntimeException("Não foi possível interpretar a resposta do ChatGPT para e-mails", retry);
            }
        }
    }

    private String buildPrompt(Experiment experiment,
                               Journey journey,
                               int quantity,
                               List<StepContext> stepContexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere até ").append(quantity).append(" e-mails em português no formato JSON. Cada objeto deve conter as chaves: ")
                .append("\"stepId\" (ID numérico da etapa), \"subject\", \"templateId\" (slug simples), \"status\" (draft, review ou approved), \"notes\" com resumo da abordagem, \"callToAction\" e \"preheader\" opcional. ")
                .append("Retorne apenas um array JSON, sem texto adicional.\n\n");
        if (experiment != null) {
            if (StringUtils.hasText(experiment.getName())) {
                sb.append("Experimento: ").append(experiment.getName()).append("\n");
            }
            if (StringUtils.hasText(experiment.getHypothesis())) {
                sb.append("Resumo do experimento: ").append(experiment.getHypothesis()).append("\n");
            }
        }
        Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
        if (hypothesis != null) {
            if (StringUtils.hasText(hypothesis.getTitle())) {
                sb.append("Hipótese: ").append(hypothesis.getTitle()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getPersona())) {
                sb.append("Persona: ").append(hypothesis.getPersona()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getProblem())) {
                sb.append("Problema: ").append(hypothesis.getProblem()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getPromise())) {
                sb.append("Promessa: ").append(hypothesis.getPromise()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getMechanism())) {
                sb.append("Mecanismo: ").append(hypothesis.getMechanism()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getUniqueMechanism())) {
                sb.append("Mecanismo único: ").append(hypothesis.getUniqueMechanism()).append("\n");
            }
        }
        if (journey != null) {
            if (StringUtils.hasText(journey.getName())) {
                sb.append("\nJornada: ").append(journey.getName()).append("\n");
            }
            if (journey.getMetadata() != null && !journey.getMetadata().isEmpty()) {
                sb.append("Metadados da jornada: ");
                journey.getMetadata().forEach((key, value) -> {
                    if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                        sb.append(key).append(": ").append(value).append("; ");
                    }
                });
                sb.append("\n");
            }
        }
        if (stepContexts != null && !stepContexts.isEmpty()) {
            sb.append("\nEtapas elegíveis para e-mails:\n");
            for (StepContext ctx : stepContexts) {
                sb.append("- Passo ID ").append(ctx.id())
                        .append(" posição ").append(ctx.position() != null ? ctx.position() : "?")
                        .append(": ").append(StringUtils.hasText(ctx.name()) ? ctx.name() : "Sem nome");
                if (StringUtils.hasText(ctx.description())) {
                    sb.append(" — ").append(ctx.description());
                }
                if (!ctx.metadata().isEmpty()) {
                    sb.append(". Metadados: ");
                    ctx.metadata().forEach((key, value) -> sb.append(key).append(": ").append(value).append("; "));
                }
                sb.append("\n");
            }
        }
        sb.append("\nFoque em entregar assunto com curiosidade clara, CTA objetivo e próximos passos práticos para o lead.");
        return sb.toString();
    }

    public record Generation(List<EmailPlan> plans, String prompt, String rawResponse, String model) {
        public Generation {
            plans = plans != null ? List.copyOf(plans) : List.of();
        }

        public static Generation disabled(String model) {
            return new Generation(List.of(), "", null, model);
        }

        public static Generation empty(String model, String prompt, String rawResponse) {
            return new Generation(List.of(), prompt, rawResponse, model);
        }

        public String auditTrail() {
            StringBuilder sb = new StringBuilder();
            if (prompt != null && !prompt.isBlank()) {
                sb.append("PROMPT:\n").append(prompt);
            }
            if (rawResponse != null && !rawResponse.isBlank()) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append("RESPOSTA:\n").append(rawResponse);
            }
            return sb.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailPlan(
            Long stepId,
            String subject,
            String templateId,
            String status,
            String notes,
            String callToAction,
            String preheader) {
    }

    public record StepContext(Long id, Integer position, String name, String description, Map<String, String> metadata) {
        public StepContext {
            if (metadata == null || metadata.isEmpty()) {
                metadata = Map.of();
            } else {
                Map<String, String> sanitized = new LinkedHashMap<>();
                metadata.forEach((key, value) -> {
                    if (key != null && value != null) {
                        sanitized.put(key, value);
                    }
                });
                metadata = Map.copyOf(sanitized);
            }
        }
    }
}
