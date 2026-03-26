package com.marketinghub.hypothesis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkGenerationRequest;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkSection;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.openai.OpenAiBatchClient;
import com.marketinghub.openai.OpenAiCostEstimator;
import com.marketinghub.openai.OpenAiProperties;
import com.marketinghub.openai.OpenAiResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HypothesisFrameworkGenerationService {
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final HypothesisRepository repository;
    private final HypothesisMapper mapper;
    private final HypothesisFrameworkMapperSupport frameworkSupport;
    private final OpenAiBatchClient openAiBatchClient;
    private final OpenAiProperties openAiProperties;
    private final AiWorkerGenerationService generationService;
    private final ObjectMapper objectMapper;

    public HypothesisFrameworkGenerationService(HypothesisRepository repository,
                                                HypothesisMapper mapper,
                                                HypothesisFrameworkMapperSupport frameworkSupport,
                                                OpenAiBatchClient openAiBatchClient,
                                                OpenAiProperties openAiProperties,
                                                AiWorkerGenerationService generationService,
                                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.frameworkSupport = frameworkSupport;
        this.openAiBatchClient = openAiBatchClient;
        this.openAiProperties = openAiProperties;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public HypothesisDto generate(UUID hypothesisId,
                                  HypothesisFrameworkSection section,
                                  HypothesisFrameworkGenerationRequest request) {
        if (!openAiProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "OpenAI API não configurada");
        }
        Hypothesis hypothesis = repository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hipótese não encontrada"));
        HypothesisFrameworkDto snapshot = frameworkSupport.resolve(hypothesis);
        String model = StringUtils.hasText(request.getModel()) ? request.getModel().trim() : DEFAULT_MODEL;
        String userPrompt = buildUserPrompt(hypothesis, snapshot, section, request.getCustomInstructions());
        Map<String, Object> body = buildRequestBody(model, userPrompt, section);
        OpenAiResponse response;
        try {
            response = openAiBatchClient.executeSingle(body, "hypothesis-framework-" + hypothesisId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao chamar OpenAI", ex);
        }
        String content = response.firstText();
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Modelo não retornou conteúdo");
        }
        applyGeneratedSection(hypothesis, snapshot, section, content);
        recordGeneration(hypothesis, section, model, userPrompt, content, response);
        return mapper.toDto(hypothesis);
    }

    private void applyGeneratedSection(Hypothesis hypothesis,
                                       HypothesisFrameworkDto snapshot,
                                       HypothesisFrameworkSection section,
                                       String jsonContent) {
        HypothesisFrameworkDto partial = new HypothesisFrameworkDto();
        try {
            switch (section) {
                case PAIN -> {
                    HypothesisFrameworkDto.Pain generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Pain.class);
                    snapshot.setPain(generated);
                    partial.setPain(generated);
                }
                case RESULT -> {
                    HypothesisFrameworkDto.Result generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Result.class);
                    snapshot.setResult(generated);
                    partial.setResult(generated);
                }
                case MECHANISM -> {
                    HypothesisFrameworkDto.Mechanism generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Mechanism.class);
                    snapshot.setMechanism(generated);
                    partial.setMechanism(generated);
                }
                case PROOF -> {
                    HypothesisFrameworkDto.Proof generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Proof.class);
                    snapshot.setProof(generated);
                    partial.setProof(generated);
                }
                case OFFER -> {
                    HypothesisFrameworkDto.Offer generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Offer.class);
                    snapshot.setOffer(generated);
                    partial.setOffer(generated);
                }
                default -> throw new IllegalStateException("Unsupported section: " + section);
            }
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível interpretar a resposta da IA", e);
        }
        frameworkSupport.storeSnapshot(hypothesis, snapshot, partial);
    }

    private void recordGeneration(Hypothesis hypothesis,
                                  HypothesisFrameworkSection section,
                                  String model,
                                  String prompt,
                                  String rawResponse,
                                  OpenAiResponse response) {
        Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
        Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
        BigDecimal costUsd = response.usage() != null
                ? OpenAiCostEstimator.estimateUsd(model, response.usage())
                : null;
        generationService.recordGeneration(AiWorkerGenerationRequest.builder()
                .domain("hypothesis.framework." + section.path())
                .referenceId(hypothesis.getId().toString())
                .prompt(prompt)
                .rawResponse(rawResponse)
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .costUsd(costUsd)
                .build());
    }

    private Map<String, Object> buildRequestBody(String model,
                                                 String prompt,
                                                 HypothesisFrameworkSection section) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.35);
        body.put("max_output_tokens", 900);
        body.put("input", List.of(
                message("system", buildSystemPrompt(section)),
                message("user", prompt)
        ));
        body.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "hypothesis_framework_" + section.path(),
                        "schema", buildSchema(section)
                )
        ));
        return body;
    }

    private Map<String, Object> buildSchema(HypothesisFrameworkSection section) {
        return switch (section) {
            case PAIN -> schema(Map.of(
                    "surface", stringField("Resumo da dor de superfície descrita pelo nicho"),
                    "root", stringField("Dor raiz que impede o resultado"),
                    "emotional", stringField("Dor emocional associada"),
                    "social", stringField("Dor social/status associado"),
                    "cost", stringField("Custo da situação atual em tempo, dinheiro ou oportunidade")
            ), List.of("surface", "root"));
            case RESULT -> schema(Map.of(
                    "desiredResult", stringField("Resultado desejado em linguagem simples"),
                    "desiredIdentity", stringField("Identidade/como o cliente quer ser percebido"),
                    "businessOutcome", stringField("Impacto concreto no negócio"),
                    "successSignal", stringField("Sinal mensurável de sucesso")
            ), List.of("desiredResult", "businessOutcome"));
            case MECHANISM -> schema(Map.of(
                    "core", stringField("Resumo direto do mecanismo principal"),
                    "unique", stringField("Diferencial/por que funciona melhor"),
                    "visible", stringField("O que o lead consegue ver/testar antes"),
                    "believability", stringField("Porque o mecanismo é crível")
            ), List.of("core"));
            case PROOF -> schema(Map.of(
                    "type", stringField("Tipo de prova (ex.: diagnóstico, amostra personalizada, antes/depois)"),
                    "asset", stringField("Formato ou ativo sugerido"),
                    "message", stringField("Mensagem principal da prova"),
                    "deliveryStage", stringField("Momento do funil onde a prova entra")
            ), List.of("type", "message"));
            case OFFER -> schema(Map.of(
                    "name", stringField("Nome curto da oferta"),
                    "corePromise", stringField("Promessa principal"),
                    "deliverables", stringField("Principais entregáveis"),
                    "riskReversal", stringField("Como reduz risco/percepção de risco"),
                    "priceLogic", stringField("Narrativa de preço/valor"),
                    "cta", stringField("Chamada para ação final"),
                    "priceAmount", Map.of("type", "number", "description", "Preço sugerido em BRL"),
                    "offerType", stringField("Tipo de oferta (LEAD ou TRIPWIRE)")
            ), List.of("corePromise", "deliverables"));
        };
    }

    private Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("additionalProperties", Boolean.FALSE);
        schema.put("required", required);
        return schema;
    }

    private Map<String, Object> stringField(String description) {
        return Map.of("type", "string", "description", description);
    }

    private Map<String, Object> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    private String buildSystemPrompt(HypothesisFrameworkSection section) {
        return switch (section) {
            case PAIN -> "Você é um estrategista focado em mapear a dor de um nicho para campanhas de aquisição."
                    + " Resuma as dores reais que travam o resultado, deixando explícitos impactos emocionais e financeiros.";
            case RESULT -> "Você descreve transformações desejadas conectando resultado emocional e outcome de negócio."
                    + " Foque em algo mensurável e desejado pelo decisor.";
            case MECHANISM -> "Você traduz mecanismos de oferta em linguagem simples, conectando o que será entregue ao porquê funciona.";
            case PROOF -> "Você define qual prova reduz ceticismo para a hipótese e como entregá-la durante o funil.";
            case OFFER -> "Você empacota a oferta com entregáveis, promessa central, lógica de preço e CTA para conversão.";
        };
    }

    private String buildUserPrompt(Hypothesis hypothesis,
                                   HypothesisFrameworkDto snapshot,
                                   HypothesisFrameworkSection section,
                                   String customInstructions) {
        StringBuilder builder = new StringBuilder();
        builder.append("Contexto do nicho: ")
                .append(hypothesis.getMarketNiche() != null ? hypothesis.getMarketNiche().getName() : "N/A")
                .append('\n');
        builder.append("Hipótese: ").append(nonNull(hypothesis.getTitle())).append('\n');
        builder.append("Persona: ").append(nonNull(hypothesis.getPersona())).append('\n');
        builder.append("Promessa atual: ").append(nonNull(hypothesis.getPromise())).append('\n');
        builder.append("Problema atual: ").append(nonNull(hypothesis.getProblem())).append('\n');
        builder.append("Dados recentes da seção: ").append(sectionSnapshot(snapshot, section)).append('\n');
        builder.append("Formato esperado: JSON válido seguindo o schema informado. Não inclua comentários.");
        if (StringUtils.hasText(customInstructions)) {
            builder.append("\nInstruções extras do usuário: ").append(customInstructions.trim());
        }
        return builder.toString();
    }

    private String sectionSnapshot(HypothesisFrameworkDto snapshot, HypothesisFrameworkSection section) {
        return switch (section) {
            case PAIN -> formatPain(snapshot.getPain());
            case RESULT -> formatResult(snapshot.getResult());
            case MECHANISM -> formatMechanism(snapshot.getMechanism());
            case PROOF -> formatProof(snapshot.getProof());
            case OFFER -> formatOffer(snapshot.getOffer());
        };
    }

    private String formatPain(HypothesisFrameworkDto.Pain pain) {
        if (pain == null) return "-";
        return String.format(Locale.ROOT,
                "surface=%s | root=%s | emotional=%s | social=%s | cost=%s",
                nonNull(pain.getSurface()),
                nonNull(pain.getRoot()),
                nonNull(pain.getEmotional()),
                nonNull(pain.getSocial()),
                nonNull(pain.getCost()));
    }

    private String formatResult(HypothesisFrameworkDto.Result result) {
        if (result == null) return "-";
        return String.format(Locale.ROOT,
                "desired=%s | identity=%s | business=%s | signal=%s",
                nonNull(result.getDesiredResult()),
                nonNull(result.getDesiredIdentity()),
                nonNull(result.getBusinessOutcome()),
                nonNull(result.getSuccessSignal()));
    }

    private String formatMechanism(HypothesisFrameworkDto.Mechanism mechanism) {
        if (mechanism == null) return "-";
        return String.format(Locale.ROOT,
                "core=%s | unique=%s | visible=%s | believable=%s",
                nonNull(mechanism.getCore()),
                nonNull(mechanism.getUnique()),
                nonNull(mechanism.getVisible()),
                nonNull(mechanism.getBelievability()));
    }

    private String formatProof(HypothesisFrameworkDto.Proof proof) {
        if (proof == null) return "-";
        return String.format(Locale.ROOT,
                "type=%s | asset=%s | message=%s | stage=%s",
                nonNull(proof.getType()),
                nonNull(proof.getAsset()),
                nonNull(proof.getMessage()),
                nonNull(proof.getDeliveryStage()));
    }

    private String formatOffer(HypothesisFrameworkDto.Offer offer) {
        if (offer == null) return "-";
        String price = offer.getPriceAmount() != null
                ? offer.getPriceAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "-";
        return String.format(Locale.ROOT,
                "name=%s | promise=%s | deliverables=%s | risk=%s | price=%s | type=%s",
                nonNull(offer.getName()),
                nonNull(offer.getCorePromise()),
                nonNull(offer.getDeliverables()),
                nonNull(offer.getRiskReversal()),
                price,
                nonNull(offer.getOfferType()));
    }

    private String nonNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }
}
