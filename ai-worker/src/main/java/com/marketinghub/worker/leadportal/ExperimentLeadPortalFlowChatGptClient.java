package com.marketinghub.worker.leadportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.prompt.Prompt;
import com.marketinghub.prompt.PromptDomainObjectType;
import com.marketinghub.prompt.PromptDomains;
import com.marketinghub.prompt.service.PromptDomainService;
import com.marketinghub.prompt.service.PromptService;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import com.marketinghub.worker.prompt.NichePromptContext;
import com.marketinghub.worker.prompt.PromptTemplateRenderer;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ChatGPT client responsável por planejar fluxos do portal do lead.
 */
@Component
public class ExperimentLeadPortalFlowChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentLeadPortalFlowChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String DOMAIN = PromptDomains.LEAD_PORTAL_FLOW;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final PromptService promptService;
    private final PromptDomainService promptDomainService;
    private final PromptTemplateRenderer promptTemplateRenderer;
    private final AiGenerationRecorder generationRecorder;

    public ExperimentLeadPortalFlowChatGptClient(WebClient.Builder builder,
                                                 ObjectMapper objectMapper,
                                                 @Value("${openai.api-key:}") String apiKey,
                                                 @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                                 @Value("${openai.model:gpt-3.5-turbo}") String model,
                                                 PromptService promptService,
                                                 PromptDomainService promptDomainService,
                                                 PromptTemplateRenderer promptTemplateRenderer,
                                                 AiGenerationRecorder generationRecorder) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.promptService = promptService;
        this.promptDomainService = promptDomainService;
        this.promptTemplateRenderer = promptTemplateRenderer;
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
        PromptData promptData = buildPrompt(experiment, quantity);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em onboarding de leads."),
                OpenAiRequestUtils.message("user", promptData.prompt())
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
            return Generation.empty(model, promptData.prompt(), null);
        }
        if (response.hasError()) {
            throw new RuntimeException("Erro na resposta do ChatGPT: " + response.errorMessage());
        }

        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                experiment != null ? String.valueOf(experiment.getId()) : null,
                promptData.prompt(),
                content,
                model,
                response.usage());
        log.info("Resposta do ChatGPT para fluxos do portal: {}", content);

        List<FlowPlan> plans = parseContent(content);
        return new Generation(plans, promptData.prompt(), content, model);
    }

    private PromptData buildPrompt(Experiment experiment, int quantity) {
        Prompt promptTemplate = promptService.getActiveByDomainOrThrow(DOMAIN);
        Map<String, Object> context = buildPromptContext(experiment, quantity);
        String rendered = promptTemplateRenderer.render(promptTemplate.getTemplate(), context);
        log.debug("Prompt do portal do lead renderizado (modelo={}): {}", model, rendered);
        return new PromptData(rendered, context);
    }

    private Map<String, Object> buildPromptContext(Experiment experiment, int quantity) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("quantity", Math.max(1, quantity));
        context.put("model", model);

        List<PromptDomainObjectType> configuredObjects = promptDomainService.getObjectTypes(DOMAIN);
        Set<PromptDomainObjectType> objectSet = new HashSet<>(configuredObjects);

        Map<String, Object> hypothesisContext = mapHypothesis(experiment != null ? experiment.getHypothesisRef() : null);
        Map<String, Object> nicheContext = mapNiche(experiment != null ? experiment.getNiche() : null);
        Map<String, Object> experimentContext = mapExperiment(experiment, hypothesisContext, nicheContext);

        // Sempre disponibilizamos o contexto completo do experimento
        context.put("experiment", experimentContext);
        if (objectSet.contains(PromptDomainObjectType.HYPOTHESIS)) {
            context.put("hypothesis", hypothesisContext);
        }
        if (objectSet.contains(PromptDomainObjectType.NICHE)) {
            context.put("niche", nicheContext);
        }
        return context;
    }

    private Map<String, Object> mapExperiment(Experiment experiment,
                                              Map<String, Object> hypothesisContext,
                                              Map<String, Object> nicheContext) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", experiment != null ? experiment.getId() : null);
        map.put("name", textOrDefault(experiment != null ? experiment.getName() : null, "Experimento exemplo"));
        map.put("hypothesisSummary", textOrDefault(experiment != null ? experiment.getHypothesis() : null, "Resumo do experimento"));
        map.put("status", experiment != null && experiment.getStatus() != null ? experiment.getStatus().name() : "DRAFT");
        map.put("platform", experiment != null && experiment.getPlatform() != null ? experiment.getPlatform().name() : "META");
        map.put("leadPortalFlowsToGenerate", experiment != null && experiment.getLeadPortalFlowsToGenerate() != null
                ? experiment.getLeadPortalFlowsToGenerate()
                : 3);
        map.put("followUpActionUrl", textOrDefault(experiment != null ? experiment.getFollowUpActionUrl() : null, "https://exemplo.com/acao"));
        map.put("createdAt", experiment != null ? experiment.getCreatedAt() : Instant.now());
        map.put("updatedAt", experiment != null ? experiment.getUpdatedAt() : Instant.now());
        map.put("hypothesis", hypothesisContext != null ? hypothesisContext : mapHypothesis(null));
        map.put("niche", nicheContext != null ? nicheContext : defaultNicheContext());
        return map;
    }

    private Map<String, Object> mapHypothesis(Hypothesis hypothesis) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", hypothesis != null ? hypothesis.getId() : null);
        map.put("title", textOrDefault(hypothesis != null ? hypothesis.getTitle() : null, "Hipótese exemplo"));
        map.put("promise", textOrDefault(hypothesis != null ? hypothesis.getPromise() : null, "Promessa de valor para o lead"));
        map.put("problem", textOrDefault(hypothesis != null ? hypothesis.getProblem() : null, "Problema recorrente do lead"));
        map.put("persona", textOrDefault(hypothesis != null ? hypothesis.getPersona() : null, "Persona ideal"));
        map.put("mechanism", textOrDefault(hypothesis != null ? hypothesis.getMechanism() : null, "Mecanismo responsável pela transformação"));
        map.put("uniqueMechanism", textOrDefault(hypothesis != null ? hypothesis.getUniqueMechanism() : null, "Elemento exclusivo da solução"));
        map.put("entrega", textOrDefault(hypothesis != null ? hypothesis.getEntrega() : null, "Entrega principal oferecida"));
        map.put("successRule", textOrDefault(hypothesis != null ? hypothesis.getSuccessRule() : null, "Critério utilizado para medir sucesso"));
        map.put("offerType", hypothesis != null && hypothesis.getOfferType() != null ? hypothesis.getOfferType().name() : "LEAD");
        map.put("price", hypothesis != null ? hypothesis.getPrice() : null);
        map.put("model", textOrDefault(hypothesis != null ? hypothesis.getModel() : null, model));
        map.put("generatedAt", hypothesis != null ? hypothesis.getGeneratedAt() : Instant.now());
        map.put("createdAt", hypothesis != null ? hypothesis.getCreatedAt() : Instant.now());
        map.put("updatedAt", hypothesis != null ? hypothesis.getUpdatedAt() : Instant.now());
        return map;
    }

    private Map<String, Object> mapNiche(MarketNiche niche) {
        if (niche == null) {
            return defaultNicheContext();
        }
        NichePromptContext context = NichePromptContext.from(niche);
        if (context != null) {
            return context.asMap();
        }
        return defaultNicheContext();
    }

    private Map<String, Object> defaultNicheContext() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 0L);
        map.put("name", "Nicho exemplo");
        map.put("description", "Descrição resumida do nicho");
        map.put("baseSegmentation", "Segmentação base sugerida");
        map.put("interests", "Interesses relevantes");
        map.put("demographicFilters", "Filtros demográficos recomendados");
        map.put("extraTips", "Dicas adicionais para explorar o público");
        map.put("interestCategory", "Categoria de interesse principal");
        map.put("roleCategory", "Categoria de papel do público");
        map.put("detailedDescriptions", List.of());
        map.put("latestDetailedDescription", null);
        map.put("hypothesisDetailedDescription", null);
        map.put("differentiatedTechnology", Map.of());
        return map;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
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

    private record PromptData(String prompt, Map<String, Object> context) {
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
