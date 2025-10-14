package com.marketinghub.worker.instantform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.worker.openai.AiGenerationRecorder;
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
 * Cliente responsável por gerar instant forms planejados para hipóteses.
 */
@Component
public class ExperimentInstantFormChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentInstantFormChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String DOMAIN = "EXPERIMENT_INSTANT_FORM";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final AiGenerationRecorder generationRecorder;

    public ExperimentInstantFormChatGptClient(WebClient.Builder builder,
                                              ObjectMapper objectMapper,
                                              @Value("${openai.api-key:}") String apiKey,
                                              @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                              @Value("${openai.model:gpt-3.5-turbo}") String model,
                                              AiGenerationRecorder generationRecorder) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.enabled = StringUtils.hasText(apiKey);
        this.generationRecorder = generationRecorder;

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
            log.warn("OpenAI API key não configurada; geração de instant forms será ignorada");
        }
        this.webClient = clientBuilder.build();
    }

    public Generation generateInstantForms(Experiment experiment,
                                           Journey journey,
                                           int quantity,
                                           List<StepContext> stepContexts) {
        if (!enabled) {
            return Generation.disabled(model);
        }
        String prompt = buildPrompt(experiment, journey, quantity, stepContexts);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em Meta Ads focado em formulários instantâneos."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Enviando prompt de instant forms para experimento {}: {}", experiment != null ? experiment.getId() : null, prompt);
        OpenAiResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block(REQUEST_TIMEOUT);
        } catch (Exception ex) {
            log.error("Falha ao consultar OpenAI para instant forms do experimento {}", experiment != null ? experiment.getId() : null, ex);
            throw new RuntimeException("Falha ao consultar ChatGPT para instant forms", ex);
        }
        if (response == null) {
            log.warn("OpenAI retornou resposta nula para experimento {}", experiment != null ? experiment.getId() : null);
            return Generation.empty(model, prompt, null);
        }
        if (response.hasError()) {
            throw new RuntimeException("Erro OpenAI: " + response.errorMessage());
        }
        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                experiment != null ? String.valueOf(experiment.getId()) : null,
                prompt,
                content,
                model,
                response.usage());
        log.info("Resposta OpenAI para instant forms: {}", content);
        List<InstantFormPlan> plans = parseContent(content);
        return new Generation(plans, prompt, content, model);
    }

    private List<InstantFormPlan> parseContent(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        try {
            InstantFormPlan[] array = objectMapper.readValue(content, InstantFormPlan[].class);
            List<InstantFormPlan> result = new ArrayList<>();
            for (InstantFormPlan plan : array) {
                if (plan != null && StringUtils.hasText(plan.formId()) && StringUtils.hasText(plan.name())) {
                    result.add(plan);
                }
            }
            return result;
        } catch (Exception ex) {
            log.error("Falha ao interpretar resposta de instant forms: {}", content, ex);
            try {
                String unescaped = content.replace("\\\"", "\"");
                InstantFormPlan[] array = objectMapper.readValue(unescaped, InstantFormPlan[].class);
                List<InstantFormPlan> result = new ArrayList<>();
                for (InstantFormPlan plan : array) {
                    if (plan != null && StringUtils.hasText(plan.formId()) && StringUtils.hasText(plan.name())) {
                        result.add(plan);
                    }
                }
                return result;
            } catch (Exception retry) {
                log.error("Falha ao interpretar resposta de instant forms após normalização: {}", content, retry);
                throw new RuntimeException("Não foi possível interpretar a resposta do ChatGPT para instant forms", retry);
            }
        }
    }

    private String buildPrompt(Experiment experiment,
                               Journey journey,
                               int quantity,
                               List<StepContext> stepContexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere até ").append(quantity).append(" instant forms em português no formato JSON. ");
        sb.append("Cada objeto deve conter as chaves \"formId\" (slug minúsculo com prefixo ai-form-), \"name\", \"status\" (draft, review ou approved), \"locale\" (pt_BR), \"followUpActionUrl\" e \"privacyPolicyUrl\". ");
        sb.append("Retorne apenas um array JSON, sem texto adicional.\n\n");

        if (experiment != null) {
            if (StringUtils.hasText(experiment.getName())) {
                sb.append("Experimento: ").append(experiment.getName()).append("\n");
            }
            if (StringUtils.hasText(experiment.getHypothesis())) {
                sb.append("Resumo do experimento: ").append(experiment.getHypothesis()).append("\n");
            }
            FacebookPage page = experiment.getFacebookPage();
            if (page != null) {
                if (StringUtils.hasText(page.getName())) {
                    sb.append("Página Meta: ").append(page.getName()).append("\n");
                }
                if (StringUtils.hasText(page.getPageId())) {
                    sb.append("ID da página: ").append(page.getPageId()).append("\n");
                }
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
            if (StringUtils.hasText(journey.getDescription())) {
                sb.append("Descrição da jornada: ").append(journey.getDescription()).append("\n");
            }
            if (journey.getMetadata() != null && !journey.getMetadata().isEmpty()) {
                sb.append("Metadados da jornada:\n");
                journey.getMetadata().forEach((key, value) -> {
                    if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                        sb.append("- ").append(key).append(": ").append(value).append("\n");
                    }
                });
            }
        }

        if (stepContexts != null && !stepContexts.isEmpty()) {
            sb.append("\nEtapas que exigem instant form:\n");
            for (StepContext context : stepContexts) {
                sb.append("- Etapa ").append(context.position() != null ? context.position() : context.id()).append(": ");
                if (StringUtils.hasText(context.name())) {
                    sb.append(context.name());
                } else {
                    sb.append("Sem nome");
                }
                sb.append("\n");
                if (StringUtils.hasText(context.description())) {
                    sb.append("  Descrição: ").append(context.description()).append("\n");
                }
                if (!context.metadata().isEmpty()) {
                    sb.append("  Metadados:\n");
                    context.metadata().forEach((key, value) -> {
                        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                            sb.append("    - ").append(key).append(": ").append(value).append("\n");
                        }
                    });
                }
            }
        }

        sb.append("\nProjete formulários que coletem consentimento explícito, dados de contato e perguntas de qualificação alinhadas aos objetivos de cada etapa. Garanta coerência com a promessa e persona descritas.\n");
        sb.append("Respeite o limite de caracteres e utilize URLs completas iniciando com https://.\n");
        return sb.toString();
    }

    public record Generation(List<InstantFormPlan> plans, String prompt, String rawResponse, String model) {
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
    public record InstantFormPlan(
            String formId,
            String name,
            String status,
            String locale,
            String followUpActionUrl,
            String privacyPolicyUrl) {
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
