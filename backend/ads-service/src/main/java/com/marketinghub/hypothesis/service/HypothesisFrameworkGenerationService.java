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
import com.marketinghub.openai.OpenAiCostEstimator;
import com.marketinghub.openai.OpenAiResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
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
    private static final String RESEARCH_DIRECTIVE = "Sempre que possível, pesquise em sites especializados do nicho"
            + " usando a ferramenta de web_search antes de responder. Use os achados para justificar cada campo do JSON"
            + " e cite as principais referências consultadas.";

    private final HypothesisRepository repository;
    private final HypothesisFrameworkGenerationJobRepository jobRepository;
    private final HypothesisMapper mapper;
    private final HypothesisFrameworkMapperSupport frameworkSupport;
    private final AiWorkerGenerationService generationService;
    private final ObjectMapper objectMapper;

    public HypothesisFrameworkGenerationService(HypothesisRepository repository,
                                                HypothesisFrameworkGenerationJobRepository jobRepository,
                                                HypothesisMapper mapper,
                                                HypothesisFrameworkMapperSupport frameworkSupport,
                                                AiWorkerGenerationService generationService,
                                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.jobRepository = jobRepository;
        this.mapper = mapper;
        this.frameworkSupport = frameworkSupport;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public HypothesisDto generate(UUID hypothesisId,
                                  HypothesisFrameworkSection section,
                                  HypothesisFrameworkGenerationRequest request) {
        Hypothesis hypothesis = repository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hipótese não encontrada"));

        List<HypothesisFrameworkGenerationJob> activeJobs = jobRepository.findByHypothesisIdAndSectionAndStatusInOrderByCreatedAtDesc(
                hypothesisId,
                section,
                Set.of(HypothesisFrameworkGenerationJobStatus.PENDING, HypothesisFrameworkGenerationJobStatus.PROCESSING));
        boolean hasActiveJob = markStaleJobsAsFailed(activeJobs);
        if (!hasActiveJob) {
            enqueueJob(hypothesis, section, request);
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
        applyGeneratedSection(hypothesis, snapshot, job.getSection(), request.responseContent());

        OpenAiResponse.OpenAiUsage usage = new OpenAiResponse.OpenAiUsage(
                request.inputTokens(),
                request.outputTokens(),
                request.inputTokens(),
                request.outputTokens(),
                totalTokens(request.inputTokens(), request.outputTokens()));
        BigDecimal estimatedCost = request.costUsd() != null
                ? request.costUsd()
                : OpenAiCostEstimator.estimateUsd(job.getModel(), usage);

        generationService.recordGeneration(AiWorkerGenerationRequest.builder()
                .domain("hypothesis.framework." + job.getSection().path())
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
                            HypothesisFrameworkGenerationRequest request) {
        HypothesisFrameworkDto snapshot = frameworkSupport.resolve(hypothesis);
        String model = StringUtils.hasText(request.getModel()) ? request.getModel().trim() : DEFAULT_MODEL;
        String userPrompt = buildUserPrompt(hypothesis, snapshot, section, request.getCustomInstructions());
        Map<String, Object> requestBody = buildRequestBody(model, userPrompt, section);
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
                    HypothesisFrameworkDto.Mechanism generated = parseMechanism(jsonContent);
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
        Map<String, Object> textConfig = new LinkedHashMap<>();
        textConfig.put("format", jsonSchemaFormat(
                "hypothesis_framework_" + section.path(),
                buildSchema(section)
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

    private String buildSystemPrompt(HypothesisFrameworkSection section) {
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
                                   String customInstructions) {
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
                    .append("\n\nDados recentes da seção:\n")
                    .append(formatMechanism(snapshot.getMechanism()))
                    .append("\n\nPreencha os campos de MECANISMO com foco em clareza, plausibilidade e valor percebido.\n\n")
                    .append("Regras obrigatórias desta seção:\n")
                    .append("1. O mecanismo deve ser escrito como a ponte entre a DOR e o RESULTADO.\n")
                    .append("2. O mecanismo central deve explicar, em linguagem simples, como a solução ajuda a sair da dor e chegar ao resultado.\n")
                    .append("3. O mecanismo único deve mostrar o diferencial sem depender de jargão técnico.\n")
                    .append("4. O campo visible deve mostrar o que o cliente consegue ver, receber, testar ou validar antes da compra.\n")
                    .append("5. O campo believability deve responder por que a solução parece plausível para essa persona, sem exagero.\n")
                    .append("6. Não transforme mecanismo em promessa.\n")
                    .append("7. Não descreva entregáveis puros como se fossem mecanismo.\n")
                    .append("8. Se houver dúvida entre um mecanismo “técnico” e um mecanismo “claro e comercial”, escolha o mais claro e comercial.\n")
                    .append("9. O mecanismo deve ser compreensível por alguém do nicho, não por um especialista em tráfego.\n\n")
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
                    .append("2. Prefira provas simples, visuais e fáceis de entregar.\n")
                    .append("3. Não torne a prova mais complexa que a oferta.\n")
                    .append("4. O tipo de prova deve ser claro e comercialmente utilizável.\n")
                    .append("5. O ativo de prova deve ser realista para operação.\n")
                    .append("6. A mensagem da prova deve conectar:\n")
                    .append("   - personalização\n")
                    .append("   - diferença percebida\n")
                    .append("   - menor risco\n")
                    .append("7. O estágio deve refletir onde essa prova melhor converte.\n")
                    .append("8. Se houver duas opções, escolha a prova com menor atrito e maior clareza.\n\n")
                    .append("Formato esperado:\n")
                    .append("JSON válido com as chaves:\n")
                    .append("type, asset, message, deliveryStage\n")
                    .append("Não inclua comentários.\n");
        } else if (section == HypothesisFrameworkSection.OFFER) {
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
                    .append("\n\nProva consolidado da seção anterior:\n- Tipo: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getType() : null))
                    .append(" \n- Ativo: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getAsset() : null))
                    .append("  \n- Mensagem: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getMessage() : null))
                    .append("  \n- Estágio: ")
                    .append(nonNull(snapshot.getProof() != null ? snapshot.getProof().getDeliveryStage() : null))
                    .append("\n\nDados recentes da seção: ")
                    .append(formatOffer(snapshot.getOffer()))
                    .append("\n\nPreencha os campos de OFERTA com foco em fechamento comercial.\n\n")
                    .append("Regras obrigatórias desta seção:\n")
                    .append("1. A oferta deve empacotar a transformação, não apenas o volume de entregáveis.\n")
                    .append("2. O nome deve ser simples, memorável e comercial.\n")
                    .append("3. A promessa da oferta deve falar de resultado percebido antes do mecanismo.\n")
                    .append("4. Os entregáveis devem ser claros e finitos.\n")
                    .append("5. O risco reverso deve reduzir insegurança de compra.\n")
                    .append("6. A narrativa de preço deve justificar valor com base em:\n")
                    .append("   - economia de tempo\n")
                    .append("   - personalização\n")
                    .append("   - velocidade\n")
                    .append("   - potencial comercial\n")
                    .append("7. O CTA deve ser simples e direto.\n")
                    .append("8. Se houver dúvida entre uma oferta “mais técnica” e uma “mais vendável”, escolha a mais vendável.\n\n")
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
