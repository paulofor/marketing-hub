package com.marketinghub.worker.leadportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalQuestionType;
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
 * ChatGPT client responsável por planejar fluxos do portal do lead.
 */
@Component
public class ExperimentLeadPortalFlowChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentLeadPortalFlowChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String DOMAIN = "LEAD_PORTAL_FLOW";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final AiGenerationRecorder generationRecorder;

    public ExperimentLeadPortalFlowChatGptClient(WebClient.Builder builder,
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
            log.warn("OpenAI API key não configurada; geração de fluxos do portal será ignorada");
        }
        this.webClient = clientBuilder.build();
    }

    public Generation generateFlows(Experiment experiment, int quantity) {
        if (!enabled) {
            log.warn("Ignorando geração de fluxos do portal para experimento {} por falta de API key",
                    experiment != null ? experiment.getId() : null);
            return Generation.disabled(model);
        }
        String prompt = buildPrompt(experiment, quantity);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em onboarding de leads."),
                OpenAiRequestUtils.message("user", prompt)
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        OpenAiResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block(REQUEST_TIMEOUT);
        } catch (Exception ex) {
            log.error("Falha ao consultar ChatGPT para fluxos do portal do lead do experimento {}",
                    experiment != null ? experiment.getId() : null, ex);
            throw new RuntimeException("Falha ao consultar ChatGPT para fluxos do portal do lead", ex);
        }
        if (response == null) {
            log.warn("ChatGPT retornou resposta vazia para experimento {}", experiment != null ? experiment.getId() : null);
            return Generation.empty(model, prompt, null);
        }
        if (response.hasError()) {
            throw new RuntimeException("Erro na resposta do ChatGPT: " + response.errorMessage());
        }

        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                experiment != null ? String.valueOf(experiment.getId()) : null,
                prompt,
                content,
                model,
                response.usage());
        log.info("Resposta do ChatGPT para fluxos do portal: {}", content);

        List<FlowPlan> plans = parseContent(content);
        return new Generation(plans, prompt, content, model);
    }

    private List<FlowPlan> parseContent(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        try {
            FlowPlan[] array = objectMapper.readValue(content, FlowPlan[].class);
            List<FlowPlan> plans = new ArrayList<>();
            for (FlowPlan plan : array) {
                if (plan != null && StringUtils.hasText(plan.name())) {
                    plans.add(plan);
                }
            }
            return plans;
        } catch (Exception ex) {
            log.error("Falha ao interpretar resposta de fluxos do portal: {}", content, ex);
            try {
                String normalized = content.replace("\\\"", "\"");
                FlowPlan[] array = objectMapper.readValue(normalized, FlowPlan[].class);
                List<FlowPlan> plans = new ArrayList<>();
                for (FlowPlan plan : array) {
                    if (plan != null && StringUtils.hasText(plan.name())) {
                        plans.add(plan);
                    }
                }
                return plans;
            } catch (Exception retry) {
                log.error("Falha ao interpretar resposta após normalização: {}", content, retry);
                throw new RuntimeException("Não foi possível interpretar a resposta do ChatGPT para fluxos do portal", retry);
            }
        }
    }

    private String buildPrompt(Experiment experiment, int quantity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere até ").append(quantity).append(" fluxos para portal de leads em português no formato JSON.");
        sb.append(" Cada item deve conter: \"name\" (título amigável), \"slug\" (kebab-case único), \"description\" (objetivo do fluxo) e \"questions\".");
        sb.append(" Em questions informe objetos com as chaves: \"title\", \"dataKey\" (snake case curto), \"type\" (TEXT, TEXTAREA, NUMBER, EMAIL, PHONE, DATE, SINGLE_CHOICE, MULTIPLE_CHOICE ou IMAGE_UPLOAD), \"required\", \"description\", \"placeholder\" e \"options\" (array, usar quando tipo for SINGLE_CHOICE ou MULTIPLE_CHOICE).");
        sb.append(" Solicite perguntas simples que envolvam o lead no diagnóstico da situação e proponha opções de resposta realistas sempre que houver múltipla escolha.");
        sb.append(" Finalize SEMPRE cada fluxo com uma pergunta do tipo IMAGE_UPLOAD solicitando uma foto clara relacionada ao problema do lead.");
        sb.append(" Responda somente com um array JSON válido, sem comentários ou texto extra.\n\n");

        if (experiment != null) {
            if (StringUtils.hasText(experiment.getName())) {
                sb.append("Experimento: ").append(experiment.getName()).append("\n");
            }
            if (StringUtils.hasText(experiment.getHypothesis())) {
                sb.append("Resumo do experimento: ").append(experiment.getHypothesis()).append("\n");
            }
            Hypothesis hypothesis = experiment.getHypothesisRef();
            if (hypothesis != null) {
                if (StringUtils.hasText(hypothesis.getProblem())) {
                    sb.append("Problema do lead: ").append(hypothesis.getProblem()).append("\n");
                }
                if (StringUtils.hasText(hypothesis.getPromise())) {
                    sb.append("Promessa da solução: ").append(hypothesis.getPromise()).append("\n");
                }
                if (StringUtils.hasText(hypothesis.getPersona())) {
                    sb.append("Persona: ").append(hypothesis.getPersona()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public record Generation(List<FlowPlan> plans, String prompt, String rawResponse, String model) {
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
    public record FlowPlan(String name,
                           String slug,
                           String description,
                           List<QuestionPlan> questions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionPlan(String title,
                               @JsonProperty("dataKey") String dataKey,
                               LeadPortalQuestionType type,
                               boolean required,
                               String description,
                               String placeholder,
                               List<String> options) {
    }
}
