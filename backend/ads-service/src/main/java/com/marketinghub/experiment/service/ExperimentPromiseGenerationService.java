package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.experiment.service.generatepromise.ExperimentPromiseOptionDto;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsRequest;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsResponse;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.openai.OpenAiBatchClient;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerar opções de contrato de promessa única para novos experimentos com apoio da IA. */
@Service
public class ExperimentPromiseGenerationService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPromiseGenerationService.class);
    private static final String MODEL = "gpt-5.2";

    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;
    private final OpenAiBatchClient openAiBatchClient;
    private final ObjectMapper objectMapper;
    private final AiWorkerGenerationService generationService;
    private final OpenAiPricingService pricingService;

    /** Inicializa o serviço com repositórios, cliente OpenAI e auditoria de gerações. */
    public ExperimentPromiseGenerationService(MarketNicheRepository nicheRepository,
                                              HypothesisRepository hypothesisRepository,
                                              OpenAiBatchClient openAiBatchClient,
                                              ObjectMapper objectMapper,
                                              AiWorkerGenerationService generationService,
                                              OpenAiPricingService pricingService) {
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.openAiBatchClient = openAiBatchClient;
        this.objectMapper = objectMapper;
        this.generationService = generationService;
        this.pricingService = pricingService;
    }

    /** Gera três opções completas para preencher o contrato de promessa única do experimento. */
    @Transactional(readOnly = true)
    public GenerateExperimentPromiseOptionsResponse generate(GenerateExperimentPromiseOptionsRequest request) {
        if (request == null || request.nicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um nicho antes de gerar com IA.");
        }
        MarketNiche niche = nicheRepository.findById(request.nicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho não encontrado"));
        Hypothesis hypothesis = resolveHypothesis(request.hypothesisId());
        String prompt = buildPrompt(request, niche, hypothesis);
        Map<String, Object> body = buildOpenAiRequest(prompt);
        try {
            OpenAiResponse response = openAiBatchClient.executeSingle(body, "experiment-promise-" + UUID.randomUUID());
            String content = response.firstText();
            if (!StringUtils.hasText(content)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A IA não retornou opções de promessa.");
            }
            GenerateExperimentPromiseOptionsResponse parsed = sanitize(objectMapper.readValue(content, GenerateExperimentPromiseOptionsResponse.class));
            recordGeneration(request, prompt, content, response.usage());
            return parsed;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Falha ao gerar contrato de promessa do experimento; operation=experiment-promise-generate nicheId={} hypothesisId={}", request.nicheId(), request.hypothesisId(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível gerar opções com IA.", ex);
        } catch (Exception ex) {
            log.error("Falha ao interpretar contrato de promessa do experimento; operation=experiment-promise-parse nicheId={} hypothesisId={}", request.nicheId(), request.hypothesisId(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível interpretar as opções da IA.", ex);
        }
    }

    /** Localiza a hipótese opcional usada como contexto da geração. */
    private Hypothesis resolveHypothesis(UUID hypothesisId) {
        if (hypothesisId == null) {
            return null;
        }
        return hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hipótese não encontrada"));
    }

    /** Monta o payload da Responses API com schema estruturado para evitar texto livre. */
    private Map<String, Object> buildOpenAiRequest(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("input", List.of(message("system", systemPrompt()), message("user", prompt)));
        body.put("max_output_tokens", 1400);
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", jsonSchemaFormat());
        body.put("text", text);
        return body;
    }

    /** Cria uma mensagem no formato aceito pela Responses API. */
    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /** Define as regras comerciais que o modelo deve respeitar na geração. */
    private String systemPrompt() {
        return "Você é especialista em marketing de resposta direta para produtos digitais. "
                + "Gere contratos de promessa única claros, simples e focados em venda via geração de leads. "
                + "Cada opção deve ter uma única dor, uma recompensa gratuita específica, promessa do funil e CTA coerentes. "
                + "Não prometa resultados impossíveis, não sugira objetivo de tráfego e mantenha tudo pronto para campanha de Leads.";
    }

    /** Monta o contexto funcional do nicho, hipótese e campos já digitados na tela. */
    private String buildPrompt(GenerateExperimentPromiseOptionsRequest request, MarketNiche niche, Hypothesis hypothesis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere exatamente 3 opções diferentes de contrato de promessa única para um novo experimento.\n");
        sb.append("Nicho: ").append(niche.getName()).append('\n');
        appendIfPresent(sb, "Descrição do nicho", niche.getDescription());
        if (hypothesis != null) {
            appendIfPresent(sb, "Hipótese selecionada", hypothesis.getTitle());
            appendIfPresent(sb, "Problema da hipótese", hypothesis.getProblem());
            appendIfPresent(sb, "Promessa da hipótese", hypothesis.getPromise());
            appendIfPresent(sb, "Mecanismo", hypothesis.getMechanism());
            appendIfPresent(sb, "Entrega", hypothesis.getEntrega());
        } else {
            appendIfPresent(sb, "Hipótese digitada", request.hypothesis());
        }
        appendIfPresent(sb, "Dor atual digitada", request.currentSinglePain());
        appendIfPresent(sb, "Recompensa atual digitada", request.currentFreeReward());
        appendIfPresent(sb, "Promessa atual digitada", request.currentFunnelPromise());
        appendIfPresent(sb, "CTA atual digitado", request.currentPrimaryCta());
        sb.append("\nAs três opções devem ser distintas: uma direta, uma emocional e uma operacional/prática.");
        return sb.toString();
    }

    /** Adiciona ao prompt apenas campos com conteúdo útil. */
    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    /** Declara o schema JSON esperado do modelo para três opções completas. */
    private Map<String, Object> jsonSchemaFormat() {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("type", "object");
        option.put("additionalProperties", false);
        option.put("required", List.of("singlePain", "freeReward", "funnelPromise", "primaryCta", "reason"));
        option.put("properties", Map.of(
                "singlePain", Map.of("type", "string"),
                "freeReward", Map.of("type", "string"),
                "funnelPromise", Map.of("type", "string"),
                "primaryCta", Map.of("type", "string"),
                "reason", Map.of("type", "string")));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("options"));
        schema.put("properties", Map.of("options", Map.of("type", "array", "minItems", 3, "maxItems", 3, "items", option)));
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "experiment_promise_options");
        format.put("schema", schema);
        format.put("json_schema", Map.of("name", "experiment_promise_options", "schema", schema));
        return format;
    }

    /** Normaliza a resposta para garantir exatamente três opções utilizáveis pela tela. */
    private GenerateExperimentPromiseOptionsResponse sanitize(GenerateExperimentPromiseOptionsResponse response) {
        List<ExperimentPromiseOptionDto> options = response == null || response.options() == null
                ? List.of()
                : response.options().stream()
                        .filter(option -> StringUtils.hasText(option.singlePain())
                                && StringUtils.hasText(option.freeReward())
                                && StringUtils.hasText(option.funnelPromise())
                                && StringUtils.hasText(option.primaryCta()))
                        .limit(3)
                        .toList();
        if (options.size() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A IA não retornou três opções completas.");
        }
        return new GenerateExperimentPromiseOptionsResponse(options);
    }

    /** Registra a geração para auditoria operacional e acompanhamento de custo. */
    private void recordGeneration(GenerateExperimentPromiseOptionsRequest request, String prompt, String content, OpenAiResponse.OpenAiUsage usage) {
        BigDecimal cost = estimateCostSafely(usage);
        generationService.recordGeneration(AiWorkerGenerationRequest.builder()
                .domain("experiment.promise-options")
                .referenceId(String.valueOf(request.nicheId()))
                .prompt(prompt)
                .rawResponse(content)
                .model(MODEL)
                .inputTokens(usage != null ? usage.effectiveInputTokens() : null)
                .outputTokens(usage != null ? usage.effectiveOutputTokens() : null)
                .costUsd(cost)
                .build());
    }

    /** Calcula o custo sem bloquear a entrega das opções quando o catálogo financeiro estiver incompleto. */
    private BigDecimal estimateCostSafely(OpenAiResponse.OpenAiUsage usage) {
        try {
            return pricingService.estimateStandardCost(MODEL, usage);
        } catch (RuntimeException ex) {
            log.error("Falha ao calcular custo da geração de promessa; operation=experiment-promise-cost model={}", MODEL, ex);
            return BigDecimal.ZERO;
        }
    }
}
