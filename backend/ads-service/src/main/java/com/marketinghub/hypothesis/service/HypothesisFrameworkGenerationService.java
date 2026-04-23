package com.marketinghub.hypothesis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJob;
import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJobStage;
import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJobStatus;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkGenerationRequest;
import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobCompletionRequest;
import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobDto;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkSection;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.hypothesis.repository.HypothesisFrameworkGenerationJobRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HypothesisFrameworkGenerationService {
    private static final String DEFAULT_MODEL = "gpt-5.2";
    private static final Duration STALE_PENDING_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration STALE_PROCESSING_TIMEOUT = Duration.ofMinutes(20);
    private static final String SUMMARY_PROMPTS_ROOT = "prompts/hypothesis-framework/summary";
    private static final String SUMMARY_SYSTEM_TEMPLATE = SUMMARY_PROMPTS_ROOT + "/shared/system.md";
    private static final String RESEARCH_DIRECTIVE = "Sempre que possível, pesquise em sites especializados do nicho"
            + " usando a ferramenta de web_search antes de responder. Use os achados para justificar cada campo do JSON"
            + " e cite as principais referências consultadas.";

    private final HypothesisRepository repository;
    private final HypothesisFrameworkGenerationJobRepository jobRepository;
    private final HypothesisMapper mapper;
    private final HypothesisFrameworkMapperSupport frameworkSupport;
    private final AiWorkerGenerationService generationService;
    private final ObjectMapper objectMapper;
    private final OpenAiPricingService openAiPricingService;

    public HypothesisFrameworkGenerationService(HypothesisRepository repository,
                                                HypothesisFrameworkGenerationJobRepository jobRepository,
                                                HypothesisMapper mapper,
                                                HypothesisFrameworkMapperSupport frameworkSupport,
                                                AiWorkerGenerationService generationService,
                                                ObjectMapper objectMapper,
                                                OpenAiPricingService openAiPricingService) {
        this.repository = repository;
        this.jobRepository = jobRepository;
        this.mapper = mapper;
        this.frameworkSupport = frameworkSupport;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
        this.openAiPricingService = openAiPricingService;
    }

    @Transactional
    public HypothesisDto generate(UUID hypothesisId,
                                  HypothesisFrameworkSection section,
                                  HypothesisFrameworkGenerationRequest request,
                                  boolean summaryOnly) {
        Hypothesis hypothesis = repository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hipótese não encontrada"));

        List<HypothesisFrameworkGenerationJob> activeJobs = jobRepository.findByHypothesisIdAndSectionAndStatusInOrderByCreatedAtDesc(
                hypothesisId,
                section,
                Set.of(HypothesisFrameworkGenerationJobStatus.PENDING, HypothesisFrameworkGenerationJobStatus.PROCESSING));
        boolean hasActiveJob = markStaleJobsAsFailed(activeJobs);
        if (!hasActiveJob) {
            enqueueJob(hypothesis, section, request, summaryOnly);
        }
        return mapper.toDto(hypothesis);
    }

    private boolean markStaleJobsAsFailed(List<HypothesisFrameworkGenerationJob> activeJobs) {
        Instant now = Instant.now();
        boolean hasActiveJob = false;
        for (HypothesisFrameworkGenerationJob activeJob : activeJobs) {
            if (isStale(activeJob, now)) {
                activeJob.setStatus(HypothesisFrameworkGenerationJobStatus.FAILED);
                activeJob.setStage(HypothesisFrameworkGenerationJobStage.FAILED);
                activeJob.setErrorMessage("Job anterior expirou aguardando processamento. Uma nova solicitação foi liberada.");
                activeJob.setFinishedAt(now);
                continue;
            }
            hasActiveJob = true;
        }
        return hasActiveJob;
    }

    private boolean isStale(HypothesisFrameworkGenerationJob job, Instant now) {
        Duration timeout = job.getStatus() == HypothesisFrameworkGenerationJobStatus.PROCESSING
                ? STALE_PROCESSING_TIMEOUT
                : STALE_PENDING_TIMEOUT;
        Instant reference = job.getStartedAt() != null ? job.getStartedAt() : job.getCreatedAt();
        if (reference == null) {
            return false;
        }
        return reference.plus(timeout).isBefore(now);
    }

    @Transactional(readOnly = true)
    public List<HypothesisFrameworkGenerationJobDto> listPendingJobs(int limit) {
        return jobRepository.findByStatusOrderByCreatedAtAsc(HypothesisFrameworkGenerationJobStatus.PENDING,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HypothesisFrameworkGenerationJobDto> listJobs(UUID hypothesisId, int limit) {
        return jobRepository.findByHypothesisIdOrderByCreatedAtDesc(
                        hypothesisId,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public HypothesisFrameworkGenerationJobDto claimJob(UUID jobId, String workerId) {
        HypothesisFrameworkGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() != HypothesisFrameworkGenerationJobStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job não está pendente");
        }
        job.setStatus(HypothesisFrameworkGenerationJobStatus.PROCESSING);
        job.setStage(HypothesisFrameworkGenerationJobStage.SENT_TO_OPENAI);
        job.setWorkerId(StringUtils.hasText(workerId) ? workerId.trim() : "unknown-worker");
        job.setStartedAt(Instant.now());
        return toDto(job);
    }

    @Transactional
    public void updateJobStage(UUID jobId, HypothesisFrameworkGenerationJobStage stage) {
        HypothesisFrameworkGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == HypothesisFrameworkGenerationJobStatus.COMPLETED
                || job.getStatus() == HypothesisFrameworkGenerationJobStatus.FAILED) {
            return;
        }
        job.setStage(stage != null ? stage : job.getStage());
    }

    @Transactional
    public void completeJob(UUID jobId, HypothesisFrameworkGenerationJobCompletionRequest request) {
        HypothesisFrameworkGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == HypothesisFrameworkGenerationJobStatus.COMPLETED) {
            return;
        }
        if (job.getStatus() != HypothesisFrameworkGenerationJobStatus.PROCESSING
                && job.getStatus() != HypothesisFrameworkGenerationJobStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job não pode ser finalizado");
        }

        Hypothesis hypothesis = job.getHypothesis();
        HypothesisFrameworkDto snapshot = frameworkSupport.resolve(hypothesis);
        boolean summaryOnly = isSummaryJob(job);
        applyGeneratedSection(hypothesis, snapshot, job.getSection(), request.responseContent(), summaryOnly);

        OpenAiResponse.OpenAiUsage usage = new OpenAiResponse.OpenAiUsage(
                request.inputTokens(),
                request.outputTokens(),
                request.inputTokens(),
                request.outputTokens(),
                totalTokens(request.inputTokens(), request.outputTokens()));
        BigDecimal estimatedCost = request.costUsd() != null
                ? request.costUsd()
                : openAiPricingService.estimateStandardCost(job.getModel(), usage);

        generationService.recordGeneration(AiWorkerGenerationRequest.builder()
                .domain("hypothesis.framework." + job.getSection().path() + (summaryOnly ? ".summary" : ""))
                .referenceId(hypothesis.getId().toString())
                .prompt(job.getPrompt())
                .rawResponse(request.rawResponse())
                .model(job.getModel())
                .inputTokens(request.inputTokens())
                .outputTokens(request.outputTokens())
                .costUsd(estimatedCost)
                .build());

        job.setStatus(HypothesisFrameworkGenerationJobStatus.COMPLETED);
        job.setStage(HypothesisFrameworkGenerationJobStage.COMPLETED);
        if (StringUtils.hasText(request.requestBodyJson())) {
            job.setRequestBodyJson(request.requestBodyJson().trim());
        }
        job.setResponseContent(request.responseContent());
        job.setRawResponse(request.rawResponse());
        job.setInputTokens(request.inputTokens());
        job.setOutputTokens(request.outputTokens());
        job.setCostUsd(estimatedCost);
        job.setErrorMessage(null);
        job.setFinishedAt(Instant.now());
    }

    @Transactional
    public void failJob(UUID jobId, String errorMessage) {
        HypothesisFrameworkGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == HypothesisFrameworkGenerationJobStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job já finalizado");
        }
        job.setStatus(HypothesisFrameworkGenerationJobStatus.FAILED);
        job.setStage(HypothesisFrameworkGenerationJobStage.FAILED);
        job.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage.trim() : "Falha desconhecida");
        job.setFinishedAt(Instant.now());
    }

    private void enqueueJob(Hypothesis hypothesis,
                            HypothesisFrameworkSection section,
                            HypothesisFrameworkGenerationRequest request,
                            boolean summaryOnly) {
        HypothesisFrameworkDto snapshot = frameworkSupport.resolve(hypothesis);
        String model = StringUtils.hasText(request.getModel()) ? request.getModel().trim() : DEFAULT_MODEL;
        String userPrompt = buildUserPrompt(hypothesis, snapshot, section, request.getCustomInstructions(), summaryOnly);
        Map<String, Object> requestBody = buildRequestBody(model, userPrompt, section, summaryOnly);
        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao serializar job de geração", ex);
        }

        HypothesisFrameworkGenerationJob job = HypothesisFrameworkGenerationJob.builder()
                .hypothesis(hypothesis)
                .section(section)
                .status(HypothesisFrameworkGenerationJobStatus.PENDING)
                .stage(HypothesisFrameworkGenerationJobStage.WAITING_AI_WORKER)
                .model(model)
                .customInstructions(request.getCustomInstructions())
                .prompt(userPrompt)
                .requestBodyJson(requestBodyJson)
                .build();
        jobRepository.save(job);
    }

    private HypothesisFrameworkGenerationJobDto toDto(HypothesisFrameworkGenerationJob job) {
        return HypothesisFrameworkGenerationJobDto.builder()
                .id(job.getId())
                .hypothesisId(job.getHypothesis().getId())
                .section(job.getSection())
                .status(job.getStatus().name())
                .stage(job.getStage().name())
                .customInstructions(job.getCustomInstructions())
                .errorMessage(job.getErrorMessage())
                .model(job.getModel())
                .prompt(job.getPrompt())
                .requestBodyJson(job.getRequestBodyJson())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    private Integer totalTokens(Integer input, Integer output) {
        if (input == null && output == null) {
            return null;
        }
        return (input != null ? input : 0) + (output != null ? output : 0);
    }

    private void applyGeneratedSection(Hypothesis hypothesis,
                                       HypothesisFrameworkDto snapshot,
                                       HypothesisFrameworkSection section,
                                       String jsonContent,
                                       boolean summaryOnly) {
        HypothesisFrameworkDto partial = new HypothesisFrameworkDto();
        try {
            switch (section) {
                case PAIN -> {
                    if (summaryOnly) {
                        HypothesisFrameworkDto.Pain generated = new HypothesisFrameworkDto.Pain();
                        generated.setSummary(parseSummaryValue(section, jsonContent));
                        snapshot.setPain(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().pain(generated).build()).getPain());
                        partial.setPain(generated);
                    } else {
                        HypothesisFrameworkDto.Pain generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Pain.class);
                        snapshot.setPain(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().pain(generated).build()).getPain());
                        partial.setPain(generated);
                    }
                }
                case RESULT -> {
                    if (summaryOnly) {
                        HypothesisFrameworkDto.Result generated = new HypothesisFrameworkDto.Result();
                        generated.setSummary(parseSummaryValue(section, jsonContent));
                        snapshot.setResult(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().result(generated).build()).getResult());
                        partial.setResult(generated);
                    } else {
                        HypothesisFrameworkDto.Result generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Result.class);
                        snapshot.setResult(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().result(generated).build()).getResult());
                        partial.setResult(generated);
                    }
                }
                case MECHANISM -> {
                    if (summaryOnly) {
                        HypothesisFrameworkDto.Mechanism generated = new HypothesisFrameworkDto.Mechanism();
                        generated.setSummary(parseSummaryValue(section, jsonContent));
                        snapshot.setMechanism(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().mechanism(generated).build())
                                .getMechanism());
                        partial.setMechanism(generated);
                    } else {
                        HypothesisFrameworkDto.Mechanism generated = parseMechanism(jsonContent);
                        snapshot.setMechanism(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().mechanism(generated).build())
                                .getMechanism());
                        partial.setMechanism(generated);
                    }
                }
                case PROOF -> {
                    if (summaryOnly) {
                        HypothesisFrameworkDto.Proof generated = new HypothesisFrameworkDto.Proof();
                        generated.setSummary(parseSummaryValue(section, jsonContent));
                        snapshot.setProof(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().proof(generated).build()).getProof());
                        partial.setProof(generated);
                    } else {
                        HypothesisFrameworkDto.Proof generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Proof.class);
                        snapshot.setProof(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().proof(generated).build()).getProof());
                        partial.setProof(generated);
                    }
                }
                case OFFER -> {
                    if (summaryOnly) {
                        HypothesisFrameworkDto.Offer generated = new HypothesisFrameworkDto.Offer();
                        generated.setSummary(parseSummaryValue(section, jsonContent));
                        snapshot.setOffer(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().offer(generated).build()).getOffer());
                        partial.setOffer(generated);
                    } else {
                        HypothesisFrameworkDto.Offer generated = objectMapper.readValue(jsonContent, HypothesisFrameworkDto.Offer.class);
                        snapshot.setOffer(frameworkSupport.merge(snapshot, HypothesisFrameworkDto.builder().offer(generated).build()).getOffer());
                        partial.setOffer(generated);
                    }
                }
                default -> throw new IllegalStateException("Unsupported section: " + section);
            }
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível interpretar a resposta da IA", e);
        }
        frameworkSupport.storeSnapshot(hypothesis, snapshot, partial);
    }

    private Map<String, Object> buildRequestBody(String model,
                                                 String prompt,
                                                 HypothesisFrameworkSection section,
                                                 boolean summaryOnly) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.35);
        body.put("max_output_tokens", 900);
        body.put("input", List.of(
                message("system", buildSystemPrompt(section, summaryOnly)),
                message("user", prompt)
        ));
        Map<String, Object> textConfig = new LinkedHashMap<>();
        textConfig.put("format", jsonSchemaFormat(
                "hypothesis_framework_" + section.path() + (summaryOnly ? "_summary" : ""),
                buildSchema(section, summaryOnly)
        ));
        body.put("text", textConfig);
        body.put("tools", List.of(Map.of("type", "web_search")));
        return body;
    }

    private Map<String, Object> jsonSchemaFormat(String name,
                                                 Map<String, Object> schema) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", name);
        format.put("schema", schema);

        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("name", name);
        jsonSchema.put("schema", schema);
        format.put("json_schema", jsonSchema);
        return format;
    }

    private Map<String, Object> buildSchema(HypothesisFrameworkSection section, boolean summaryOnly) {
        if (summaryOnly) {
            return switch (section) {
                case PAIN -> schema(Map.of(
                        "coreProblem", stringField("Problema central da seção PAIN em linguagem comercial objetiva"),
                        "commercialConsequence", stringField("Principal consequência comercial da dor"),
                        "urgency", stringField("Urgência percebida para agir agora")
                ), List.of("coreProblem", "commercialConsequence", "urgency"));
                case RESULT -> schema(Map.of(
                        "desiredTransformation", stringField("Transformação final desejada pela persona"),
                        "businessOutcome", stringField("Impacto de negócio esperado"),
                        "successMarker", stringField("Marcador simples e objetivo de sucesso")
                ), List.of("desiredTransformation", "businessOutcome", "successMarker"));
                case MECHANISM -> schema(Map.of(
                        "coreMechanism", stringField("Mecanismo central em linguagem simples"),
                        "visibleBeforePurchase", stringField("Elemento visível/validável antes da compra"),
                        "reasonToBelieve", stringField("Principal razão para acreditar que funciona")
                ), List.of("coreMechanism", "visibleBeforePurchase", "reasonToBelieve"));
                case PROOF -> schema(Map.of(
                        "proofType", stringField("Tipo de prova que mais reduz ceticismo"),
                        "proofAsset", stringField("Ativo/formato de prova recomendado"),
                        "proofMessage", stringField("Mensagem principal da prova")
                ), List.of("proofType", "proofAsset", "proofMessage"));
                case OFFER -> schema(Map.of(
                        "primaryAction", stringField("Ação principal esperada do lead"),
                        "proofAsset", stringField("Ativo de prova associado à oferta"),
                        "topDeliverables", stringField("Entregáveis prioritários em linguagem curta"),
                        "preferredCta", stringField("CTA preferencial da oferta")
                ), List.of("primaryAction", "proofAsset", "topDeliverables", "preferredCta"));
            };
        }
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
                    "promise", stringField("Promessa principal"),
                    "deliverables", stringField("Principais entregáveis"),
                    "riskReversal", stringField("Como reduz risco/percepção de risco"),
                    "priceNarrative", stringField("Narrativa de preço/valor"),
                    "cta", stringField("Chamada para ação final")
            ), List.of("name", "promise", "deliverables", "riskReversal", "priceNarrative", "cta"));
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

    private String buildSystemPrompt(HypothesisFrameworkSection section, boolean summaryOnly) {
        if (summaryOnly) {
            return loadPromptTemplate(SUMMARY_SYSTEM_TEMPLATE);
        }
        return switch (section) {
            case PAIN -> "Você é um estrategista focado em mapear a dor de um nicho para campanhas de aquisição."
                    + " Resuma as dores reais que travam o resultado, deixando explícitos impactos emocionais e financeiros. "
                    + RESEARCH_DIRECTIVE;
            case RESULT -> "Você descreve transformações desejadas conectando resultado emocional e outcome de negócio."
                    + " Foque em algo mensurável e desejado pelo decisor. "
                    + RESEARCH_DIRECTIVE;
            case MECHANISM -> "Você traduz mecanismos de oferta em linguagem simples, conectando o que será entregue ao porquê funciona. "
                    + RESEARCH_DIRECTIVE;
            case PROOF -> "Você define qual prova reduz ceticismo para a hipótese e como entregá-la durante o funil. "
                    + RESEARCH_DIRECTIVE;
            case OFFER -> "Você empacota a oferta com entregáveis, promessa central, lógica de preço e CTA para conversão. "
                    + RESEARCH_DIRECTIVE;
        };
    }

    private String buildUserPrompt(Hypothesis hypothesis,
                                   HypothesisFrameworkDto snapshot,
                                   HypothesisFrameworkSection section,
                                   String customInstructions,
                                   boolean summaryOnly) {
        if (summaryOnly) {
            return buildSummaryPrompt(hypothesis, snapshot, section, customInstructions);
        }
        StringBuilder builder = new StringBuilder();
        String niche = hypothesis.getMarketNiche() != null ? hypothesis.getMarketNiche().getName() : "N/A";
        if (section == HypothesisFrameworkSection.PAIN) {
            builder.append("Contexto do nicho: ")
                    .append(niche)
                    .append("\n\nPreencha os campos de DOR com foco em clareza comercial.\n\n")
                    .append("Regras obrigatórias desta seção:\n")
                    .append("1. A dor de superfície deve ser escrita como a persona sentiria e falaria.\n")
                    .append("2. A dor raiz deve explicar a causa real sem usar linguagem técnica demais.\n")
                    .append("3. A dor emocional deve mostrar o peso psicológico do problema no dia a dia.\n")
                    .append("4. A dor social deve mostrar impacto em imagem, autoridade ou comparação com outros profissionais.\n")
                    .append("5. O custo deve mostrar perda de dinheiro, tempo, previsibilidade ou oportunidade.\n")
                    .append("6. Não suba a sofisticação da dor além do que a persona média entenderia.\n")
                    .append("7. Se houver duas versões possíveis, escolha a que fica mais fácil de usar em anúncio, landing page e venda.\n")
                    .append("8. Evite transformar a dor raiz em uma aula de marketing.\n\n")
                    .append("Regra adicional:\n")
                    .append("Se a dor puder ser resolvida por diferentes tipos de solução, descreva-a de forma que também possa ser atacada por materiais digitais, comunicação e ativos gerados por IA.\n\n")
                    .append("Formato esperado:\n")
                    .append("JSON válido com as chaves:\n")
                    .append("surface, root, emotional, social, cost\n")
                    .append("Não inclua comentários.\n");
        } else if (section == HypothesisFrameworkSection.RESULT) {
            builder.append("Contexto do nicho: ")
                    .append(niche)
                    .append("\n\nDor consolidada da seção anterior:\n- Superfície: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSurface() : null))
                    .append("\n- Raiz: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getRoot() : null))
                    .append("\n- Emocional: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getEmotional() : null))
                    .append("\n- Social: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSocial() : null))
                    .append("\n- Custo: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getCost() : null))
                    .append("\n\nPreencha os campos de RESULTADO com foco em transformação percebida.\n\n")
                    .append("Regras obrigatórias:\n")
                    .append("1. O resultado desejado deve ser a transformação da dor acima, não o mecanismo.\n")
                    .append("2. O resultado principal deve expressar ganho final percebido.\n")
                    .append("3. A identidade deve mostrar como a persona quer ser vista.\n")
                    .append("4. O impacto de negócio deve traduzir o resultado em consequência prática.\n")
                    .append("5. O sinal de sucesso deve ser objetivo e simples.\n")
                    .append("6. Não transforme entregável ou ferramenta em resultado.\n")
                    .append("7. Use linguagem compatível com a maturidade média da persona.\n\n")
                    .append("Formato esperado:\n")
                    .append("JSON válido com as chaves:\n")
                    .append("desiredResult, desiredIdentity, businessOutcome, successSignal\n")
                    .append("Não inclua comentários.\n");
        } else if (section == HypothesisFrameworkSection.MECHANISM) {
            builder.append("Contexto do nicho: ")
                    .append(niche)
                    .append("\n\nNível da persona: público geral do nicho, com baixa a moderada maturidade em marketing e tráfego.\n\n")
                    .append("\nDor consolidada da seção anterior:\n- Superfície: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSurface() : null))
                    .append("\n- Raiz: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getRoot() : null))
                    .append("\n- Emocional: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getEmotional() : null))
                    .append("\n- Social: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSocial() : null))
                    .append("\n- Custo: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getCost() : null))
                    .append("\n\nResultado consolidado da seção anterior:\n- Resultado desejado: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getDesiredResult() : null))
                    .append("\n- Identidade desejada: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getDesiredIdentity() : null))
                    .append("\n- Impacto no negócio: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getBusinessOutcome() : null))
                    .append("\n- Sinal de sucesso: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getSuccessSignal() : null))
                    .append("\n\nDados recentes da seção:\n")
                    .append(formatMechanism(snapshot.getMechanism()))
                    .append("\n\nEnvelope real do produto:\n")
                    .append("- Este produto precisa ser altamente escalável e majoritariamente automatizado por IA.\n")
                    .append("- O produto pode entregar:\n")
                    .append("  - imagens personalizadas\n")
                    .append("  - kits criativos\n")
                    .append("  - copies prontas\n")
                    .append("  - e-books / PDFs / playbooks\n")
                    .append("  - amostras com marca d’água\n")
                    .append("  - materiais digitais de comunicação, captação e onboarding\n")
                    .append("- O produto NÃO pode entregar:\n")
                    .append("  - consultoria 1:1\n")
                    .append("  - calls\n")
                    .append("  - diagnóstico manual humano\n")
                    .append("  - acompanhamento recorrente\n")
                    .append("  - revisão humana individual\n")
                    .append("  - implementação manual contínua\n")
                    .append("  - gestão de tráfego\n")
                    .append("\nEntregáveis permitidos para compor o mecanismo:\n")
                    .append("- kit de imagens personalizadas\n")
                    .append("- kit de criativos feed/story\n")
                    .append("- bio/capa/destaques\n")
                    .append("- copies para anúncio e WhatsApp\n")
                    .append("- e-book curto / mini-guia\n")
                    .append("- playbook PDF\n")
                    .append("- mapa visual de progresso\n")
                    .append("- sequência de boas-vindas em texto\n")
                    .append("- amostra com marca d’água\n")
                    .append("- regeneração automática do material\n")
                    .append("\n\nPreencha os campos de MECANISMO com foco em clareza, plausibilidade e valor percebido.\n\n")
                    .append("Regras obrigatórias desta seção:\n")
                    .append("1. O mecanismo deve ser a ponte entre a DOR e o RESULTADO.\n")
                    .append("2. O mecanismo deve explicar como ativos digitais gerados por IA ajudam a sair da dor e se aproximar do resultado.\n")
                    .append("3. O mecanismo não pode depender de consultoria, call, diagnóstico manual ou acompanhamento humano.\n")
                    .append("4. O mecanismo central deve ser escrito em linguagem simples e comercial.\n")
                    .append("5. O mecanismo único deve mostrar o diferencial do produto sem parecer serviço humano.\n")
                    .append("6. O campo visible deve mostrar o que o cliente consegue ver, receber, testar ou validar antes da compra.\n")
                    .append("7. O campo believability deve explicar por que isso parece plausível para essa persona, sem exagero.\n")
                    .append("8. Não transforme mecanismo em promessa.\n")
                    .append("9. Não descreva operação comercial humana como se fosse o mecanismo do produto.\n")
                    .append("10. Se houver dúvida entre um mecanismo “mais completo” e um mecanismo “mais escalável”, escolha o mais escalável.\n")
                    .append("11. Se a melhor resposta depender de trabalho humano recorrente, descarte e gere uma alternativa automatizável.\n\n")
                    .append("O que privilegiar:\n")
                    .append("- sistema de geração de ativos\n")
                    .append("- clareza visual e textual\n")
                    .append("- facilidade de implementação\n")
                    .append("- personalização por briefing simples\n")
                    .append("- uso imediato do material\n\n")
                    .append("O que evitar:\n")
                    .append("- “consultoria”\n")
                    .append("- “sessão”\n")
                    .append("- “diagnóstico manual”\n")
                    .append("- “ajuste com você”\n")
                    .append("- “acompanhamento”\n")
                    .append("- “análise individual”\n")
                    .append("- “gestão”\n\n")
                    .append("Formato esperado:\n")
                    .append("JSON válido com as chaves:\n")
                    .append("core, unique, visible, believability\n")
                    .append("Não inclua comentários.\n");
        } else if (section == HypothesisFrameworkSection.PROOF) {
            builder.append("Contexto do nicho: ")
                    .append(niche)
                    .append("\n\nDor consolidada da seção anterior:\n- Superfície: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSurface() : null))
                    .append("\n- Raiz: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getRoot() : null))
                    .append("\n- Emocional: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getEmotional() : null))
                    .append("\n- Social: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSocial() : null))
                    .append("\n- Custo: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getCost() : null))
                    .append("\n\nResultado consolidado da seção anterior:\n- Resultado desejado: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getDesiredResult() : null))
                    .append("\n- Identidade desejada: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getDesiredIdentity() : null))
                    .append("\n- Impacto no negócio: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getBusinessOutcome() : null))
                    .append("\n- Sinal de sucesso: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getSuccessSignal() : null))
                    .append("\n\nMecanismo consolidado da seção anterior:\n- Mecanismo central: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getCore() : null))
                    .append(" \n- Mecanismo único: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getUnique() : null))
                    .append(" \n- O que é visível: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getVisible() : null))
                    .append("  \n- Por que acreditar: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getBelievability() : null))
                    .append("\n\nDados recentes da seção: ")
                    .append(formatProof(snapshot.getProof()))
                    .append("\n\nPreencha os campos de PROVA com foco em redução de ceticismo e facilidade de implementação.\n\n")
                    .append("Regras obrigatórias desta seção:\n")
                    .append("1. A prova deve reduzir o medo de “isso é só template” ou “isso não vai servir para mim”.\n")
                    .append("2. A prova deve ser automatizável.\n")
                    .append("3. A prova deve poder ser entregue sem interação humana 1:1.\n")
                    .append("4. Prefira:\n")
                    .append("   - amostra visual personalizada\n")
                    .append("   - mini-kit com marca d’água\n")
                    .append("   - página de comparação antes/depois\n")
                    .append("   - mini e-book ou guia adaptado ao nicho\n")
                    .append("   - prévia do material final\n")
                    .append("5. Evite:\n")
                    .append("   - diagnóstico ao vivo\n")
                    .append("   - análise manual de WhatsApp\n")
                    .append("   - call\n")
                    .append("   - revisão humana personalizada\n")
                    .append("   - stress test real operado manualmente\n")
                    .append("6. Se a prova depender de trabalho humano contínuo, simplifique até caber em automação.\n")
                    .append("7. Não torne a prova mais complexa que a oferta.\n")
                    .append("8. O tipo de prova deve ser claro e comercialmente utilizável.\n")
                    .append("9. O ativo de prova deve ser realista para operação.\n")
                    .append("10. A mensagem da prova deve conectar:\n")
                    .append("   - personalização\n")
                    .append("   - diferença percebida\n")
                    .append("   - menor risco\n")
                    .append("11. O estágio deve refletir onde essa prova melhor converte.\n")
                    .append("12. Se houver duas opções, escolha a prova com menor atrito e maior clareza.\n\n")
                    .append("Formato esperado:\n")
                    .append("JSON válido com as chaves:\n")
                    .append("type, asset, message, deliveryStage\n")
                    .append("Não inclua comentários.\n");
        } else if (section == HypothesisFrameworkSection.OFFER) {
            builder.append("Contexto do nicho: ")
                    .append(niche)
                    .append("\nHipótese: ")
                    .append(nonNull(hypothesis.getTitle()))
                    .append("\n\nNível da persona: público geral do nicho, com baixa a moderada maturidade em marketing e tráfego.\n")
                    .append("\n\nDor consolidada da seção anterior:\n- Superfície: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSurface() : null))
                    .append("\n- Raiz: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getRoot() : null))
                    .append("\n- Emocional: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getEmotional() : null))
                    .append("\n- Social: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getSocial() : null))
                    .append("\n- Custo: ")
                    .append(nonNull(snapshot.getPain() != null ? snapshot.getPain().getCost() : null))
                    .append("\n\nResultado consolidado da seção anterior:\n- Resultado desejado: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getDesiredResult() : null))
                    .append("\n- Identidade desejada: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getDesiredIdentity() : null))
                    .append("\n- Impacto no negócio: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getBusinessOutcome() : null))
                    .append("\n- Sinal de sucesso: ")
                    .append(nonNull(snapshot.getResult() != null ? snapshot.getResult().getSuccessSignal() : null))
                    .append("\n\nMecanismo consolidado da seção anterior:\n- Mecanismo central: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getCore() : null))
                    .append(" \n- Mecanismo único: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getUnique() : null))
                    .append(" \n- O que é visível: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getVisible() : null))
                    .append("  \n- Por que acreditar: ")
                    .append(nonNull(snapshot.getMechanism() != null ? snapshot.getMechanism().getBelievability() : null))
                    .append("\n\nProva consolidada da seção anterior:\n- Tipo: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getType() : null))
                    .append(" \n- Ativo: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getAsset() : null))
                    .append("  \n- Mensagem: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getMessage() : null))
                    .append("  \n- Estágio: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getDeliveryStage() : null))
                    .append("\n\nDados recentes da seção: ")
                    .append(formatOffer(snapshot.getOffer()))
                    .append("\n\nEnvelope real do produto:\n")
                    .append("- Este produto deve ser altamente escalável e majoritariamente automatizado por IA.\n")
                    .append("- O produto pode entregar apenas ativos digitais gerados automaticamente, como:\n")
                    .append("  - imagens personalizadas\n")
                    .append("  - kits criativos\n")
                    .append("  - copies prontas\n")
                    .append("  - e-books / mini-guias / playbooks em PDF\n")
                    .append("  - bio/capa/destaques\n")
                    .append("  - sequências de mensagens prontas\n")
                    .append("  - mapas visuais de progresso\n")
                    .append("  - amostras com marca d’água\n")
                    .append("  - regeneração automática de material\n")
                    .append("- O produto NÃO pode incluir:\n")
                    .append("  - consultoria 1:1\n")
                    .append("  - calls\n")
                    .append("  - sessões extras\n")
                    .append("  - revisão humana recorrente\n")
                    .append("  - acompanhamento individual\n")
                    .append("  - implementação manual\n")
                    .append("  - diagnóstico ao vivo\n")
                    .append("  - gestão de tráfego\n")
                    .append("\nEntregáveis permitidos:\n")
                    .append("- amostra em PDF com marca d’água\n")
                    .append("- kit visual feed/story\n")
                    .append("- copies para anúncio\n")
                    .append("- copies para WhatsApp\n")
                    .append("- bio/capa/destaques\n")
                    .append("- mini e-book / playbook / guia\n")
                    .append("- mapa de progresso em PDF\n")
                    .append("- sequência de boas-vindas pronta\n")
                    .append("- check-ins em texto prontos\n")
                    .append("- versão/regeneração automática do kit\n")
                    .append("\nPreencha os campos de OFERTA com foco em fechamento comercial.\n\n")
                    .append("Regras obrigatórias desta seção:\n")
                    .append("1. A oferta deve empacotar a transformação, não apenas o volume de entregáveis.\n")
                    .append("2. A oferta deve caber 100% no envelope do produto.\n")
                    .append("3. Não proponha consultoria, call, sessão, revisão humana, diagnóstico manual ou acompanhamento.\n")
                    .append("4. Os entregáveis devem ser exclusivamente digitais, finitos, replicáveis e escaláveis.\n")
                    .append("5. O nome deve ser simples, memorável e comercial.\n")
                    .append("6. A promessa deve falar primeiro de resultado percebido e só depois do tipo de material.\n")
                    .append("7. O risco reverso deve ser compatível com automação, por exemplo:\n")
                    .append("   - nova geração automática\n")
                    .append("   - nova versão da amostra\n")
                    .append("   - crédito para regeneração\n")
                    .append("   - garantia de adequação ao briefing\n")
                    .append("8. A narrativa de preço deve justificar valor com base em:\n")
                    .append("   - economia de tempo\n")
                    .append("   - personalização\n")
                    .append("   - velocidade\n")
                    .append("   - reaproveitamento do material\n")
                    .append("   - potencial comercial\n")
                    .append("9. O CTA deve levar para uma ação escalável:\n")
                    .append("   - preencher briefing\n")
                    .append("   - gerar amostra\n")
                    .append("   - desbloquear kit\n")
                    .append("   - receber prévia\n")
                    .append("10. Se a melhor resposta exigir trabalho humano recorrente, descarte e gere uma alternativa compatível com IA.\n")
                    .append(
                            "11. Se houver dúvida entre uma oferta “mais sofisticada” e uma “mais vendável e escalável”, escolha a mais vendável e escalável.\n")
                    .append("12. Use somente entregáveis permitidos.\n\n")
                    .append("O que privilegiar:\n")
                    .append("- kit\n")
                    .append("- pack\n")
                    .append("- playbook\n")
                    .append("- amostra\n")
                    .append("- PDF\n")
                    .append("- criativos\n")
                    .append("- material pronto\n")
                    .append("- personalização por formulário\n")
                    .append("- geração rápida\n")
                    .append("\nO que evitar:\n")
                    .append("- consultoria\n")
                    .append("- análise manual\n")
                    .append("- sessão estratégica\n")
                    .append("- call diagnóstica\n")
                    .append("- acompanhamento\n")
                    .append("- implementação personalizada\n\n")
                    .append("Formato esperado:\n")
                    .append("JSON válido com as chaves:\n")
                    .append("name, promise, deliverables, riskReversal, priceNarrative, cta\n")
                    .append("Não inclua comentários.\n");
        } else {
            builder.append("Contexto do nicho: ")
                    .append(niche)
                    .append('\n');
        }
        builder.append("Hipótese: ").append(nonNull(hypothesis.getTitle())).append('\n');
        builder.append("Persona: ").append(nonNull(hypothesis.getPersona())).append('\n');
        builder.append("Promessa atual: ").append(nonNull(hypothesis.getPromise())).append('\n');
        builder.append("Problema atual: ").append(nonNull(hypothesis.getProblem())).append('\n');
        if (section != HypothesisFrameworkSection.MECHANISM
                && section != HypothesisFrameworkSection.PROOF) {
            builder.append("Dados recentes da seção: ").append(sectionSnapshot(snapshot, section)).append('\n');
        }
        if (section != HypothesisFrameworkSection.PAIN
                && section != HypothesisFrameworkSection.RESULT
                && section != HypothesisFrameworkSection.MECHANISM) {
            builder.append("Formato esperado: JSON válido seguindo o schema informado. Não inclua comentários.");
        }
        if (StringUtils.hasText(customInstructions)) {
            builder.append("\nInstruções extras do usuário: ").append(customInstructions.trim());
        }
        return builder.toString();
    }

    private String buildSummaryPrompt(Hypothesis hypothesis,
                                      HypothesisFrameworkDto snapshot,
                                      HypothesisFrameworkSection section,
                                      String customInstructions) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("NICHE_NAME", hypothesis.getMarketNiche() != null ? hypothesis.getMarketNiche().getName() : "N/A");
        placeholders.put("HYPOTHESIS_TITLE", nonNull(hypothesis.getTitle()));
        placeholders.put("SECTION_NAME", section.name());
        placeholders.put("SECTION_SNAPSHOT", sectionSnapshot(snapshot, section));
        placeholders.put("CUSTOM_INSTRUCTIONS", StringUtils.hasText(customInstructions) ? customInstructions.trim() : "-");
        placeholders.put("PAIN_JSON", toJson(snapshot.getPain()));
        placeholders.put("RESULT_JSON", toJson(snapshot.getResult()));
        placeholders.put("MECHANISM_JSON", toJson(snapshot.getMechanism()));
        placeholders.put("PROOF_JSON", toJson(snapshot.getProof()));
        placeholders.put("OFFER_JSON", toJson(snapshot.getOffer()));
        String sectionTemplate = loadPromptTemplate(summaryUserTemplatePath(section));
        return renderTemplate(sectionTemplate, placeholders);
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
                "name=%s | offer_promise=%s | deliverables=%s | risk=%s | price_logic=%s | cta=%s",
                nonNull(offer.getName()),
                nonNull(offer.getCorePromise()),
                nonNull(offer.getDeliverables()),
                nonNull(offer.getRiskReversal()),
                nonNull(offer.getPriceLogic() != null ? offer.getPriceLogic() : price),
                nonNull(offer.getCta()));
    }

    private String nonNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private boolean isSummaryJob(HypothesisFrameworkGenerationJob job) {
        return StringUtils.hasText(job.getRequestBodyJson())
                && job.getRequestBodyJson().contains("_summary");
    }

    private String parseSummaryValue(HypothesisFrameworkSection section, String jsonContent) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(jsonContent);
        String fallback = firstText(root, "item resumido", "itemResumido", "item_resumido", "resumo", "summary");
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        List<String> chunks = new ArrayList<>();
        switch (section) {
            case PAIN -> {
                appendSummaryChunk(chunks, firstText(root, "coreProblem"));
                appendSummaryChunk(chunks, firstText(root, "commercialConsequence"));
                appendSummaryChunk(chunks, firstText(root, "urgency"));
            }
            case RESULT -> {
                appendSummaryChunk(chunks, firstText(root, "desiredTransformation"));
                appendSummaryChunk(chunks, firstText(root, "businessOutcome"));
                appendSummaryChunk(chunks, firstText(root, "successMarker"));
            }
            case MECHANISM -> {
                appendSummaryChunk(chunks, firstText(root, "coreMechanism"));
                appendSummaryChunk(chunks, firstText(root, "visibleBeforePurchase"));
                appendSummaryChunk(chunks, firstText(root, "reasonToBelieve"));
            }
            case PROOF -> {
                appendSummaryChunk(chunks, firstText(root, "proofType"));
                appendSummaryChunk(chunks, firstText(root, "proofAsset"));
                appendSummaryChunk(chunks, firstText(root, "proofMessage"));
            }
            case OFFER -> {
                appendSummaryChunk(chunks, firstText(root, "primaryAction"));
                appendSummaryChunk(chunks, firstText(root, "proofAsset"));
                appendSummaryChunk(chunks, firstText(root, "topDeliverables"));
                appendSummaryChunk(chunks, firstText(root, "preferredCta"));
            }
            default -> {
                return null;
            }
        }
        return chunks.isEmpty() ? null : String.join(" | ", chunks);
    }

    private void appendSummaryChunk(List<String> chunks, String value) {
        if (StringUtils.hasText(value)) {
            chunks.add(value.trim());
        }
    }

    private String summaryUserTemplatePath(HypothesisFrameworkSection section) {
        return SUMMARY_PROMPTS_ROOT + "/" + section.path().toLowerCase(Locale.ROOT) + "/user.md";
    }

    private String loadPromptTemplate(String classpathLocation) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            if (stream == null) {
                throw new IllegalStateException("Template de prompt não encontrado: " + classpathLocation);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao carregar template de prompt: " + classpathLocation, ex);
        }
    }

    private String renderTemplate(String template, Map<String, String> placeholders) {
        String rendered = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", nonNull(entry.getValue()));
        }
        return rendered;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "-";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "-";
        }
    }

    private HypothesisFrameworkDto.Mechanism parseMechanism(String jsonContent) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(jsonContent);
        JsonNode source = root;
        if (root != null && root.has("mechanism") && root.get("mechanism").isObject()) {
            source = root.get("mechanism");
        }

        HypothesisFrameworkDto.Mechanism mechanism = new HypothesisFrameworkDto.Mechanism();
        mechanism.setCore(firstText(source, "core"));
        mechanism.setVisible(firstText(source, "visible"));
        mechanism.setUnique(firstText(source, "unique", "differential", "uniqueMechanism"));
        mechanism.setBelievability(firstText(source, "believability", "believable", "whyBelieve", "reasonToBelieve"));
        return mechanism;
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }
}
