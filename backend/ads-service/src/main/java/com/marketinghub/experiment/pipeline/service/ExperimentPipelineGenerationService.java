package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStage;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationJobDetailDto;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationJobSummaryDto;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobCompletionRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto;
import com.marketinghub.experiment.pipeline.repository.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.openai.OpenAiCostEstimator;
import com.marketinghub.openai.OpenAiResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExperimentPipelineGenerationService {
    private static final String DEFAULT_MODEL = "gpt-5.2";
    private static final Duration STALE_PENDING_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration STALE_PROCESSING_TIMEOUT = Duration.ofMinutes(20);
    private static final String COMMON_CAMPAIGN_ASSET_RULES = """
            Você cria ativos de campanha para o Marketing Hub.

            Regras globais:
            1. O anúncio e a landing devem ter a mesma promessa central.
            2. O CTA do anúncio deve combinar com a ação principal da landing.
            3. O material precisa caber no envelope real do produto:
               - pode entregar ativos digitais gerados por IA
               - não pode prometer consultoria, call, gestão humana ou acompanhamento manual
            4. Priorize clareza comercial:
               DOR → RESULTADO → MECANISMO → PROVA → AÇÃO
            5. Não transforme mecanismo em promessa principal.
            6. Não use jargão técnico desnecessário.
            7. O público é geral dentro do nicho, com baixa a moderada maturidade em marketing.
            8. Sempre escreva pensando em alta escala e geração automatizada.
            9. O anúncio deve ser rápido de entender.
            10. A landing deve aprofundar a promessa e reduzir ceticismo.
            """;

    private final ExperimentRepository experimentRepository;
    private final ExperimentPipelineGenerationJobRepository jobRepository;
    private final ExperimentMapper experimentMapper;
    private final AiWorkerGenerationService generationService;
    private final ObjectMapper objectMapper;

    public ExperimentPipelineGenerationService(ExperimentRepository experimentRepository,
                                               ExperimentPipelineGenerationJobRepository jobRepository,
                                               ExperimentMapper experimentMapper,
                                               AiWorkerGenerationService generationService,
                                               ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.jobRepository = jobRepository;
        this.experimentMapper = experimentMapper;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExperimentDto generate(Long experimentId,
                                  ExperimentPipelineSection section,
                                  ExperimentPipelineGenerationRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));

        validatePredecessor(experiment, section);

        List<ExperimentPipelineGenerationJob> activeJobs = jobRepository
                .findByExperimentIdAndSectionAndStatusInOrderByCreatedAtDesc(
                        experimentId,
                        section,
                        Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING));
        boolean hasActiveJob = markStaleJobsAsFailed(activeJobs);
        if (!hasActiveJob) {
            enqueueJob(experiment, section, request);
        }
        return experimentMapper.toDto(experiment);
    }

    @Transactional(readOnly = true)
    public List<ExperimentPipelineGenerationJobDto> listPendingJobs(int limit) {
        return jobRepository.findByStatusOrderByCreatedAtAsc(ExperimentPipelineGenerationJobStatus.PENDING,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExperimentPipelineGenerationJobDto> listJobs(Long experimentId, int limit) {
        return jobRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(this::toDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public Page<ExperimentPipelineGenerationJobSummaryDto> listJobsPage(Long experimentId,
                                                                        ExperimentPipelineSection section,
                                                                        int page,
                                                                        int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ExperimentPipelineGenerationJob> jobs = section == null
                ? jobRepository.findByExperimentId(experimentId, pageable)
                : jobRepository.findByExperimentIdAndSection(experimentId, section, pageable);
        return jobs.map(this::toSummaryDto);
    }

    public BigDecimal totalCostUsd(Long experimentId) {
        ensureExperimentExists(experimentId);
        return jobRepository.sumCostUsdByExperimentId(experimentId);
    }

    private void ensureExperimentExists(Long experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado");
        }
    }

    @Transactional(readOnly = true)
    public ExperimentPipelineGenerationJobDetailDto getJobDetail(Long experimentId, UUID jobId) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (!job.getExperiment().getId().equals(experimentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado para este experimento");
        }
        return toDetailDto(job);
    }

    @Transactional
    public ExperimentPipelineGenerationJobDto claimJob(UUID jobId, String workerId) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() != ExperimentPipelineGenerationJobStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job não está pendente");
        }
        job.setStatus(ExperimentPipelineGenerationJobStatus.PROCESSING);
        job.setStage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI);
        job.setWorkerId(StringUtils.hasText(workerId) ? workerId.trim() : "unknown-worker");
        job.setStartedAt(Instant.now());
        return toDto(job);
    }

    @Transactional
    public void updateJobStage(UUID jobId, ExperimentPipelineGenerationJobStage stage) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == ExperimentPipelineGenerationJobStatus.COMPLETED
                || job.getStatus() == ExperimentPipelineGenerationJobStatus.FAILED) {
            return;
        }
        job.setStage(stage != null ? stage : job.getStage());
    }

    @Transactional
    public void completeJob(UUID jobId, ExperimentPipelineGenerationJobCompletionRequest request) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == ExperimentPipelineGenerationJobStatus.COMPLETED) {
            return;
        }
        Experiment experiment = job.getExperiment();
        applySectionContent(experiment, job.getSection(), request.responseContent());
        if (job.getSection() == ExperimentPipelineSection.AD_COPY
                || job.getSection() == ExperimentPipelineSection.AD_IMAGE_BRIEFING) {
            generationService.deleteByDomainAndReferenceId(
                    "experiment.pipeline." + job.getSection().path(),
                    job.getExperiment().getId().toString());
        }

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
                .domain("experiment.pipeline." + job.getSection().path())
                .referenceId(job.getExperiment().getId().toString())
                .prompt(job.getPrompt())
                .rawResponse(request.rawResponse())
                .model(job.getModel())
                .inputTokens(request.inputTokens())
                .outputTokens(request.outputTokens())
                .costUsd(estimatedCost)
                .build());

        job.setStatus(ExperimentPipelineGenerationJobStatus.COMPLETED);
        job.setStage(ExperimentPipelineGenerationJobStage.COMPLETED);
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
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == ExperimentPipelineGenerationJobStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job já finalizado");
        }
        job.setStatus(ExperimentPipelineGenerationJobStatus.FAILED);
        job.setStage(ExperimentPipelineGenerationJobStage.FAILED);
        job.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage.trim() : "Falha desconhecida");
        job.setFinishedAt(Instant.now());
    }

    private void enqueueJob(Experiment experiment,
                            ExperimentPipelineSection section,
                            ExperimentPipelineGenerationRequest request) {
        String model = StringUtils.hasText(request.getModel()) ? request.getModel().trim() : DEFAULT_MODEL;
        String userPrompt = buildUserPrompt(experiment, section, request.getCustomInstructions());
        Map<String, Object> requestBody = buildRequestBody(model, userPrompt, section);

        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao serializar job de geração", ex);
        }

        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .experiment(experiment)
                .section(section)
                .status(ExperimentPipelineGenerationJobStatus.PENDING)
                .stage(ExperimentPipelineGenerationJobStage.WAITING_AI_WORKER)
                .model(model)
                .customInstructions(request.getCustomInstructions())
                .prompt(userPrompt)
                .requestBodyJson(requestBodyJson)
                .build();
        jobRepository.save(job);
    }

    private void validatePredecessor(Experiment experiment, ExperimentPipelineSection section) {
        ExperimentPipelineSection predecessor = section.predecessor();
        if (predecessor == null) {
            return;
        }
        String predecessorContent = switch (predecessor) {
            case CAMPAIGN_ANGLE -> experiment.getCampaignAngle();
            case AD_COPY -> experiment.getAdCopy();
            case AD_IMAGE_BRIEFING -> experiment.getAdImageBriefing();
            case LANDING_PAGE_COPY -> experiment.getLandingPageCopy();
            case LANDING_PAGE_WIREFRAME -> experiment.getLandingPageWireframe();
        };
        if (!StringUtils.hasText(predecessorContent)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A seção " + section.path() + " depende da seção " + predecessor.path() + " já concluída");
        }
    }

    private boolean markStaleJobsAsFailed(List<ExperimentPipelineGenerationJob> activeJobs) {
        Instant now = Instant.now();
        boolean hasActiveJob = false;
        for (ExperimentPipelineGenerationJob activeJob : activeJobs) {
            if (isStale(activeJob, now)) {
                activeJob.setStatus(ExperimentPipelineGenerationJobStatus.FAILED);
                activeJob.setStage(ExperimentPipelineGenerationJobStage.FAILED);
                activeJob.setErrorMessage("Job anterior expirou aguardando processamento. Uma nova solicitação foi liberada.");
                activeJob.setFinishedAt(now);
                continue;
            }
            hasActiveJob = true;
        }
        return hasActiveJob;
    }

    private boolean isStale(ExperimentPipelineGenerationJob job, Instant now) {
        Duration timeout = job.getStatus() == ExperimentPipelineGenerationJobStatus.PROCESSING
                ? STALE_PROCESSING_TIMEOUT
                : STALE_PENDING_TIMEOUT;
        Instant reference = job.getStartedAt() != null ? job.getStartedAt() : job.getCreatedAt();
        if (reference == null) {
            return false;
        }
        return reference.plus(timeout).isBefore(now);
    }

    private String buildUserPrompt(Experiment experiment,
                                   ExperimentPipelineSection section,
                                   String customInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Experimento #").append(experiment.getId()).append("\n");
        appendIfPresent(sb, "Nome do experimento", experiment.getName());
        appendIfPresent(sb, "Hipótese resumida", experiment.getHypothesis());
        if (experiment.getHypothesisRef() != null) {
            appendIfPresent(sb, "Título da hipótese", experiment.getHypothesisRef().getTitle());
            appendIfPresent(sb, "Problema", experiment.getHypothesisRef().getProblem());
            appendIfPresent(sb, "Promessa", experiment.getHypothesisRef().getPromise());
        }
        sb.append("Metadados obrigatórios do experimento:\n");
        sb.append("- primary_variable: ").append(nonBlank(experiment.getPrimaryVariable())).append("\n");
        sb.append("- variant_id: variant-").append(experiment.getId()).append("\n");
        sb.append("- stage: ").append(experiment.getStage() != null ? experiment.getStage().name() : "").append("\n");
        sb.append("- control_or_treatment: treatment\n");
        sb.append("- asset_role: ").append(section.path()).append("\n");
        sb.append("\nTarefa alvo: ").append(section.path()).append("\n");
        appendPreviousOutputs(sb, experiment, section);
        appendSectionPrompt(sb, experiment, section);
        if (StringUtils.hasText(customInstructions)) {
            sb.append("\nInstruções extras do usuário:\n").append(customInstructions.trim()).append("\n");
        }
        sb.append("\nResponda exclusivamente em JSON válido e siga estritamente o schema da seção.");
        return sb.toString();
    }

    private Map<String, Object> buildRequestBody(String model,
                                                 String userPrompt,
                                                 ExperimentPipelineSection section) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("role", "system", "content", buildSystemPrompt(section)),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", "experiment_pipeline_" + section.path().replace("-", "_"),
                        "strict", true,
                        "schema", sectionSchema(section)
                )
        ));
        return body;
    }

    private String buildSystemPrompt(ExperimentPipelineSection section) {
        return switch (section) {
            case AD_COPY -> "Você é redator especialista em Meta Ads e copy de resposta direta. "
                    + "Escreva em português do Brasil, com clareza comercial e sem jargões técnicos desnecessários. "
                    + "Siga rigorosamente as regras e o formato solicitado no prompt do usuário.";
            default -> "Você é especialista em marketing direto e criação de ativos para performance. "
                    + "Escreva em português do Brasil, de forma clara e vendável. "
                    + "Seção atual: " + section.path() + ". "
                    + "Considere as dependências anteriores já geradas e mantenha consistência entre elas.";
        };
    }

    private void appendSectionPrompt(StringBuilder sb,
                                     Experiment experiment,
                                     ExperimentPipelineSection section) {
        String niche = experiment.getNiche() != null ? nonBlank(experiment.getNiche().getName()) : "";
        String campaignAngle = nonBlank(experiment.getCampaignAngle());
        Map<String, String> campaignAngleFields = extractCampaignAngleFields(campaignAngle);
        String primaryPain = firstNonBlank(
                campaignAngleFields.get("primaryPain"),
                experiment.getHypothesisRef() != null ? nonBlank(experiment.getHypothesisRef().getProblem()) : "");
        String primaryPromise = firstNonBlank(
                campaignAngleFields.get("primaryPromise"),
                experiment.getHypothesisRef() != null ? nonBlank(experiment.getHypothesisRef().getPromise()) : "");
        String mechanismSummary = firstNonBlank(
                campaignAngleFields.get("mechanismSummary"),
                experiment.getHypothesisRef() != null ? nonBlank(experiment.getHypothesisRef().getMechanism()) : "");
        String proofSummary = firstNonBlank(
                campaignAngleFields.get("proofSummary"),
                campaignAngleFields.get("proofUsed"),
                experiment.getHypothesisRef() != null ? nonBlank(experiment.getHypothesisRef().getEntrega()) : "");
        String singleMindedPromise = campaignAngleFields.get("singleMindedPromise");
        String primaryCta = firstNonBlank(campaignAngleFields.get("primaryCTA"), campaignAngleFields.get("cta"));
        String landingMatchLine = campaignAngleFields.get("landingMatchLine");
        List<String> adHeadlines = extractAdCopyHeadlines(experiment.getAdCopy());
        List<String> adCtas = extractAdCopyCtas(experiment.getAdCopy());
        String primaryAdHeadline = !adHeadlines.isEmpty() ? adHeadlines.get(0) : firstNonBlank(singleMindedPromise, primaryPromise);
        String primaryAdCta = !adCtas.isEmpty() ? adCtas.get(0) : primaryCta;
        String landingCtaForInstructions = firstNonBlank(primaryAdCta, primaryCta, "CTA principal");

        if (section == ExperimentPipelineSection.AD_COPY) {
            sb.append("\nDiretriz específica para texto do anúncio:\n");
            sb.append(COMMON_CAMPAIGN_ASSET_RULES).append("\n");
            sb.append("Contexto do nicho: ").append(niche).append("\n\n");
            sb.append("Ângulo da campanha: ").append(campaignAngle).append("\n");
            sb.append("Dor principal: ").append(primaryPain).append("\n");
            sb.append("Promessa principal: ").append(primaryPromise).append("\n");
            sb.append("Mecanismo resumido: ").append(mechanismSummary).append("\n");
            sb.append("Prova resumida: ").append(proofSummary).append("\n\n");
            appendIfPresent(sb, "Promessa single-minded", singleMindedPromise);
            appendIfPresent(sb, "CTA principal", primaryCta);
            appendIfPresent(sb, "Linha de match com landing", landingMatchLine);
            sb.append("\n");
            sb.append("Objetivo do anúncio:\n");
            sb.append("Gerar clique qualificado para a landing page.\n\n");
            sb.append("Regras:\n");
            sb.append("1. O texto do anúncio deve ser entendido em poucos segundos.\n");
            sb.append("2. A primeira linha deve abrir com dor, consequência, resultado ou prova.\n");
            sb.append("3. O mecanismo deve aparecer só depois do benefício principal.\n");
            sb.append("4. O anúncio não pode parecer consultoria.\n");
            sb.append("5. A promessa precisa ser compatível com ativos digitais gerados por IA.\n");
            sb.append("6. Não usar jargão de tráfego pago.\n");
            sb.append("7. Criar 3 variações:\n");
            sb.append("   - V1 focada na dor\n");
            sb.append("   - V2 focada no resultado\n");
            sb.append("   - V3 focada na prova\n");
            sb.append("8. Para cada variação, entregar 3 comprimentos de texto principal: curta, media e longa.\n");
            sb.append("9. Definir openingHookType por variação com um valor entre: dor, consequência, resultado, prova.\n");
            sb.append("10. Definir placementHint por variação com um valor entre: feed, stories/reels.\n");
            sb.append("11. Aplicar trava de compliance em todas as variações:\n");
            sb.append("    - sem garantia absoluta\n");
            sb.append("    - sem promessa individual\n");
            sb.append("    - sem linguagem de consultoria\n");
            sb.append("12. O CTA deve combinar exatamente com a landing.\n");
            sb.append("13. Entregar copy testável por placement e comprimento para Meta Ads.\n\n");
            sb.append("Entregue apenas o JSON abaixo sem texto adicional:\n");
            sb.append("{\n");
            sb.append("  \"adCopy\": {\n");
            sb.append("    \"primaryTextVariants\": [\n");
            sb.append("    {\n");
            sb.append("      \"label\": \"dor\",\n");
            sb.append("      \"openingHookType\": \"dor\",\n");
            sb.append("      \"placementHint\": \"feed\",\n");
            sb.append("      \"lengthVariants\": {\n");
            sb.append("        \"curta\": \"\",\n");
            sb.append("        \"media\": \"\",\n");
            sb.append("        \"longa\": \"\"\n");
            sb.append("      },\n");
            sb.append("      \"headline\": \"\",\n");
            sb.append("      \"description\": \"\",\n");
            sb.append("      \"ctaText\": \"\",\n");
            sb.append("      \"compliance\": {\n");
            sb.append("        \"semGarantiaAbsoluta\": true,\n");
            sb.append("        \"semPromessaIndividual\": true,\n");
            sb.append("        \"semLinguagemDeConsultoria\": true\n");
            sb.append("      }\n");
            sb.append("    },\n");
            sb.append("    {\n");
            sb.append("      \"label\": \"resultado\",\n");
            sb.append("      \"openingHookType\": \"resultado\",\n");
            sb.append("      \"placementHint\": \"stories/reels\",\n");
            sb.append("      \"lengthVariants\": {\n");
            sb.append("        \"curta\": \"\",\n");
            sb.append("        \"media\": \"\",\n");
            sb.append("        \"longa\": \"\"\n");
            sb.append("      },\n");
            sb.append("      \"headline\": \"\",\n");
            sb.append("      \"description\": \"\",\n");
            sb.append("      \"ctaText\": \"\",\n");
            sb.append("      \"compliance\": {\n");
            sb.append("        \"semGarantiaAbsoluta\": true,\n");
            sb.append("        \"semPromessaIndividual\": true,\n");
            sb.append("        \"semLinguagemDeConsultoria\": true\n");
            sb.append("      }\n");
            sb.append("    },\n");
            sb.append("    {\n");
            sb.append("      \"label\": \"prova\",\n");
            sb.append("      \"openingHookType\": \"prova\",\n");
            sb.append("      \"placementHint\": \"feed\",\n");
            sb.append("      \"lengthVariants\": {\n");
            sb.append("        \"curta\": \"\",\n");
            sb.append("        \"media\": \"\",\n");
            sb.append("        \"longa\": \"\"\n");
            sb.append("      },\n");
            sb.append("      \"headline\": \"\",\n");
            sb.append("      \"description\": \"\",\n");
            sb.append("      \"ctaText\": \"\",\n");
            sb.append("      \"compliance\": {\n");
            sb.append("        \"semGarantiaAbsoluta\": true,\n");
            sb.append("        \"semPromessaIndividual\": true,\n");
            sb.append("        \"semLinguagemDeConsultoria\": true\n");
            sb.append("      }\n");
            sb.append("    }\n");
            sb.append("    ]\n");
            sb.append("  },\n");
            sb.append("  \"experimentMetadata\": {\n");
            sb.append("    \"primary_variable\": \"\",\n");
            sb.append("    \"variant_id\": \"\",\n");
            sb.append("    \"stage\": \"\",\n");
            sb.append("    \"control_or_treatment\": \"\",\n");
            sb.append("    \"asset_role\": \"ad-copy\"\n");
            sb.append("  }\n");
            sb.append("}\n");
            return;
        }

        if (section == ExperimentPipelineSection.AD_IMAGE_BRIEFING) {
            sb.append("\nDiretriz específica para prompt da imagem:\n");
            sb.append(COMMON_CAMPAIGN_ASSET_RULES).append("\n");
            sb.append("Contexto do nicho: ").append(niche).append("\n\n");
            sb.append("Ângulo da campanha: ").append(campaignAngle).append("\n");
            sb.append("Dor principal: ").append(primaryPain).append("\n");
            sb.append("Promessa principal: ").append(primaryPromise).append("\n");
            sb.append("Mecanismo resumido: ").append(mechanismSummary).append("\n");
            sb.append("Prova resumida: ").append(proofSummary).append("\n\n");
            appendIfPresent(sb, "Promessa single-minded", singleMindedPromise);
            appendIfPresent(sb, "CTA principal", primaryCta);
            appendIfPresent(sb, "Linha de match com landing", landingMatchLine);
            sb.append("\n");
            sb.append("Objetivo:\n");
            sb.append("Gerar briefing visual testável por variável do experimento (não apenas imagem bonita).\n\n");
            sb.append("Regras obrigatórias:\n");
            sb.append("1. Gerar exatamente 3 briefings, um por variação do anúncio: dor, resultado e prova.\n");
            sb.append("2. Em cada briefing, preencher visualAngle com: dor, resultado ou prova.\n");
            sb.append("3. Em cada briefing, preencher mustMatchAdVariant com: dor, resultado ou prova.\n");
            sb.append("4. Em cada briefing, definir assetType com um valor entre: estatico, carrossel, story-vertical.\n");
            sb.append("5. Em cada briefing, definir imageTextMaxWords (inteiro de 3 a 12) para limitar texto sobreposto.\n");
            sb.append("6. Garantir coerência entre criativo, copy da variação e promessa da landing pós-clique.\n");
            sb.append("7. Preservar hierarchy visual, safe margins e notas de compliance por peça.\n");
            sb.append("8. Evitar claims absolutos e qualquer linguagem de consultoria.\n\n");
        }

        if (section == ExperimentPipelineSection.LANDING_PAGE_COPY) {
            sb.append("\nDiretriz específica para copy da landing:\n");
            sb.append(COMMON_CAMPAIGN_ASSET_RULES).append("\n");
            sb.append("Headline clicada no anúncio: " + primaryAdHeadline + "\n");
            sb.append("CTA do anúncio: " + landingCtaForInstructions + "\n");
            appendIfPresent(sb, "Linha de match com landing", landingMatchLine);
            sb.append("\nObjetivo:\n");
            sb.append("Transformar o clique qualificado em " + landingCtaForInstructions + " mantendo a mesma promessa e CTA do anúncio.\n\n");
            sb.append("Regras:\n");
            sb.append("1. messageMatchSource deve citar exatamente qual headline do anúncio está sendo espelhada.\n");
            sb.append("2. hero.headline, hero.promise e pageGoal precisam repetir a single-minded promise sem variações criativas.\n");
            sb.append("3. hero.ctaLabel, primaryCTA e todos os ctaBlocks devem usar exatamente o mesmo texto do CTA do anúncio.\n");
            sb.append("4. bodySections deve cobrir pelo menos dor, mecanismo, prova e oferta com sectionType explícito.\n");
            sb.append("5. Cada bodySections[i] precisa preencher sectionDependsOn (primaryPromise, mechanismSummary, proofSummary ou primaryCTA) e messageMatchNotes descrevendo o vínculo.\n");
            sb.append("6. ctaBlocks deve repetir o CTA no hero, meio e final com placement entre hero, mid, final, sticky ou inline.\n");
            sb.append("7. faq precisa ter no mínimo 3 perguntas com objectionTag descrevendo a objeção atendida.\n");
            sb.append("8. consistencyChecks deve listar pelo menos CTA_MATCH, PROMISE_MATCH e GOOGLE_LANDING_BEST_PRACTICES indicando status PASS/WARN/FAIL.\n");
            sb.append("9. messageMatchNotes e messageMatchSource devem garantir continuidade literal da promessa do anúncio.\n");
            sb.append("10. complianceNotes precisa reforçar que a oferta é entregue por ativos digitais (sem consultoria ou call).\n\n");
            sb.append("Formato obrigatório:\n");
            sb.append("- Preencher hero com eyebrow, headline, subheadline, promise, supportingCopy, proofBadge, microcopy e CTA.\n");
            sb.append("- Preencher messageMatchSource com a headline real do anúncio usada como referência: " + primaryAdHeadline + ".\n");
            sb.append("- Preencher bodySections com sectionId único, sectionType, resumo, bullets e CTA de apoio quando existir.\n");
            sb.append("- Preencher ctaBlocks com placement, ctaVariant, matchAdCta e messageMatchNotes descrevendo onde o CTA aparece na página.\n");
            sb.append("- Preencher faq e consistencyChecks conforme regras acima.\n");
            sb.append("- Em primaryCTA e ctaBlocks[].matchAdCta usar exatamente: " + landingCtaForInstructions + ".\n");
            return;
        }

        if (section == ExperimentPipelineSection.LANDING_PAGE_WIREFRAME) {
            sb.append("\nDiretriz específica para wireframe da landing:\n");
            sb.append(COMMON_CAMPAIGN_ASSET_RULES).append("\n");
            sb.append("Contexto do nicho: ").append(niche).append("\n");
            sb.append("Dor principal: ").append(primaryPain).append("\n");
            sb.append("Promessa principal: ").append(primaryPromise).append("\n");
            sb.append("Mecanismo resumido: ").append(mechanismSummary).append("\n");
            sb.append("Prova resumida: ").append(proofSummary).append("\n");
            sb.append("Hero/headline atual: " + primaryAdHeadline + "\n");
            sb.append("CTA obrigatório: " + landingCtaForInstructions + "\n");
            appendIfPresent(sb, "Linha de match com landing", landingMatchLine);
            sb.append("\nObjetivo:\n");
            sb.append("Transformar o copy da landing em um wireframe testável, mobile-first e com message match obrigatório.\n\n");
            sb.append("Regras:\n");
            sb.append("1. A estrutura deve deixar claro, logo no primeiro bloco, para qual nicho a página foi feita.\n");
            sb.append("2. pageGoal deve deixar explícito o resultado da página (ex.: gerar pedido da prévia).\n");
            sb.append("3. variantLayoutId deve ser form-first, proof-first ou story-first.\n");
            sb.append("4. sectionOrder precisa listar cada bloco com sectionId, sectionName, objective, contentType (hero, form, split, proof, timeline, faq, cta), copySource e uiNotes.\n");
            sb.append("5. Cada sectionOrder[i] deve preencher mobilePriorityScore (1-10), dropOffRisk (baixo, medio ou alto) e sectionDependsOn amarrando com primaryPromise, mechanismSummary, proofSummary ou primaryCTA.\n");
            sb.append("6. Se a seção tiver CTA, preencher ctaSlot com hasCta=true, ctaLabel, ctaVariant (hero, mid, final, sticky ou inline) e matchAdCta.\n");
            sb.append("7. formPlacementNotes precisa informar quantos scrolls são necessários para ver o formulário e se existe versão sticky.\n");
            sb.append("8. ctaPlacementNotes deve garantir repetição literal do CTA principal em toda a página.\n");
            sb.append("9. consistencyChecks deve incluir CTA_MATCH e EXPERIENCE_CONTINUITY (status PASS/WARN/FAIL) descrevendo se o anúncio e a landing estão alinhados.\n");
            sb.append("10. mobilePriorityNotes deve destacar o que precisa aparecer antes da rolagem.\n");
            sb.append("11. Não usar linguagem de consultoria e não criar estrutura que pareça página genérica para qualquer mercado.\n");
            sb.append("12. Se a estrutura puder servir para qualquer nicho, reescreva até ficar específica para ").append(niche).append(".\n\n");
            sb.append("Formato obrigatório:\n");
            sb.append("- Preencher sectionOrder respeitando a ordem real da landing e referenciando sectionId de bodySections quando existir.\n");
            sb.append("- Preencher ctaSlot dentro das seções com CTA.\n");
            sb.append("- Preencher consistencyChecks e observações finais (mobilePriorityNotes, ctaPlacementNotes, formPlacementNotes).\n");
            sb.append("- Reforçar message match usando primaryAdHeadline, landingMatchLine e " + landingCtaForInstructions + " como referência fixa.\n");
            return;
        }

        }

    private void appendPreviousOutputs(StringBuilder sb,
                                       Experiment experiment,
                                       ExperimentPipelineSection section) {
        if (section.predecessor() == null) {
            return;
        }
        if (StringUtils.hasText(experiment.getCampaignAngle())) {
            sb.append("\nÂngulo da campanha:\n").append(experiment.getCampaignAngle().trim()).append("\n");
        }
        if (StringUtils.hasText(experiment.getAdCopy())) {
            sb.append("\nTexto do anúncio:\n").append(experiment.getAdCopy().trim()).append("\n");
        }
        if (StringUtils.hasText(experiment.getAdImageBriefing())) {
            sb.append("\nBriefing da imagem:\n").append(experiment.getAdImageBriefing().trim()).append("\n");
        }
        if (StringUtils.hasText(experiment.getLandingPageCopy())) {
            sb.append("\nTextos da landing:\n").append(experiment.getLandingPageCopy().trim()).append("\n");
        }
    }

    private void applySectionContent(Experiment experiment,
                                     ExperimentPipelineSection section,
                                     String content) {
        String normalized = StringUtils.hasText(content) ? content.trim() : null;
        switch (section) {
            case CAMPAIGN_ANGLE -> experiment.setCampaignAngle(normalized);
            case AD_COPY -> experiment.setAdCopy(normalized);
            case AD_IMAGE_BRIEFING -> experiment.setAdImageBriefing(normalized);
            case LANDING_PAGE_COPY -> experiment.setLandingPageCopy(normalized);
            case LANDING_PAGE_WIREFRAME -> experiment.setLandingPageWireframe(normalized);
        }
    }


    private ExperimentPipelineGenerationJobSummaryDto toSummaryDto(ExperimentPipelineGenerationJob job) {
        return ExperimentPipelineGenerationJobSummaryDto.builder()
                .id(job.getId())
                .experimentId(job.getExperiment().getId())
                .section(job.getSection())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .stage(job.getStage() != null ? job.getStage().name() : null)
                .model(job.getModel())
                .errorMessage(job.getErrorMessage())
                .costUsd(job.getCostUsd())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    private ExperimentPipelineGenerationJobDetailDto toDetailDto(ExperimentPipelineGenerationJob job) {
        return ExperimentPipelineGenerationJobDetailDto.builder()
                .id(job.getId())
                .experimentId(job.getExperiment().getId())
                .section(job.getSection())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .stage(job.getStage() != null ? job.getStage().name() : null)
                .model(job.getModel())
                .workerId(job.getWorkerId())
                .customInstructions(job.getCustomInstructions())
                .prompt(job.getPrompt())
                .requestBodyJson(job.getRequestBodyJson())
                .responseContent(job.getResponseContent())
                .rawResponse(job.getRawResponse())
                .errorMessage(job.getErrorMessage())
                .inputTokens(job.getInputTokens())
                .outputTokens(job.getOutputTokens())
                .costUsd(job.getCostUsd())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    private ExperimentPipelineGenerationJobDto toDto(ExperimentPipelineGenerationJob job) {
        return ExperimentPipelineGenerationJobDto.builder()
                .id(job.getId())
                .experimentId(job.getExperiment().getId())
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

    private Integer totalTokens(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }
        return (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
    }

    private String nonBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractCampaignAngleFields(String campaignAngle) {
        if (!StringUtils.hasText(campaignAngle)) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(campaignAngle, Map.class);
            if (parsed.get("campaignAngle") instanceof Map<?, ?> nestedCampaignAngle) {
                Map<String, Object> nested = (Map<String, Object>) nestedCampaignAngle;
                return toStringMap(nested);
            }
            return toStringMap(parsed);
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    private Map<String, String> toStringMap(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (value instanceof String text && StringUtils.hasText(text)) {
                values.put(key, text.trim());
            }
        });
        return values;
    }

    private List<String> extractAdCopyHeadlines(String adCopy) {
        return extractAdCopyField(adCopy, "headline");
    }

    private List<String> extractAdCopyCtas(String adCopy) {
        return extractAdCopyField(adCopy, "ctaText");
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAdCopyField(String adCopy, String fieldName) {
        if (!StringUtils.hasText(adCopy) || !StringUtils.hasText(fieldName)) {
            return List.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(adCopy, Map.class);
            Object adCopyNode = parsed.get("adCopy");
            if (adCopyNode instanceof Map<?, ?> nested) {
                parsed = (Map<String, Object>) nested;
            }
            Object variantsNode = parsed.get("primaryTextVariants");
            if (!(variantsNode instanceof List<?> variants)) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (Object variantObj : variants) {
                if (variantObj instanceof Map<?, ?> rawVariant) {
                    Object value = ((Map<String, Object>) rawVariant).get(fieldName);
                    if (value instanceof String text && StringUtils.hasText(text)) {
                        values.add(text.trim());
                    }
                }
            }
            return values;
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    private Map<String, Object> sectionSchema(ExperimentPipelineSection section) {
        Map<String, Object> metadataSchema = experimentMetadataSchema();
        return switch (section) {
            case CAMPAIGN_ANGLE -> schemaWithMetadata("campaignAngle", Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of(
                            "primaryPain", Map.of("type", "string"),
                            "primaryPromise", Map.of("type", "string"),
                            "mechanismSummary", Map.of("type", "string"),
                            "proofSummary", Map.of("type", "string"),
                            "cta", Map.of("type", "string"),
                            "singleMindedPromise", Map.of("type", "string"),
                            "primaryCTA", Map.of("type", "string"),
                            "landingMatchLine", Map.of("type", "string"),
                            "tone", Map.of("type", "string"),
                            "funnelStage", Map.of("type", "string")
                    ),
                    "required", List.of(
                            "primaryPain",
                            "primaryPromise",
                            "mechanismSummary",
                            "proofSummary",
                            "cta",
                            "singleMindedPromise",
                            "primaryCTA",
                            "landingMatchLine",
                            "tone",
                            "funnelStage")
            ), metadataSchema);
            case AD_COPY -> schemaWithMetadata("adCopy", Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of(
                            "primaryTextVariants", Map.of(
                                    "type", "array",
                                    "items", Map.of(
                                            "type", "object",
                                            "additionalProperties", false,
                                            "properties", Map.of(
                                                    "label", Map.of("type", "string"),
                                                    "openingHookType", Map.of(
                                                            "type", "string",
                                                            "enum", List.of("dor", "consequência", "resultado", "prova")
                                                    ),
                                                    "placementHint", Map.of(
                                                            "type", "string",
                                                            "enum", List.of("feed", "stories/reels")
                                                    ),
                                                    "lengthVariants", Map.of(
                                                            "type", "object",
                                                            "additionalProperties", false,
                                                            "properties", Map.of(
                                                                    "curta", Map.of("type", "string"),
                                                                    "media", Map.of("type", "string"),
                                                                    "longa", Map.of("type", "string")
                                                            ),
                                                            "required", List.of("curta", "media", "longa")
                                                    ),
                                                    "primaryText", Map.of("type", "string"),
                                                    "headline", Map.of("type", "string"),
                                                    "description", Map.of("type", "string"),
                                                    "ctaText", Map.of("type", "string"),
                                                    "compliance", Map.of(
                                                            "type", "object",
                                                            "additionalProperties", false,
                                                            "properties", Map.of(
                                                                    "semGarantiaAbsoluta", Map.of("type", "boolean"),
                                                                    "semPromessaIndividual", Map.of("type", "boolean"),
                                                                    "semLinguagemDeConsultoria", Map.of("type", "boolean")
                                                            ),
                                                            "required", List.of(
                                                                    "semGarantiaAbsoluta",
                                                                    "semPromessaIndividual",
                                                                    "semLinguagemDeConsultoria")
                                                    )
                                            ),
                                            "required", List.of(
                                                    "label",
                                                    "openingHookType",
                                                    "placementHint",
                                                    "lengthVariants",
                                                    "headline",
                                                    "description",
                                                    "ctaText",
                                                    "compliance")
                                    )
                            )
                    ),
                    "required", List.of("primaryTextVariants")
            ), metadataSchema);
            case AD_IMAGE_BRIEFING -> schemaWithMetadata("adImageBriefing", Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of(
                            "briefings", Map.of(
                                    "type", "array",
                                    "minItems", 3,
                                    "maxItems", 3,
                                    "items", Map.of(
                                            "type", "object",
                                            "additionalProperties", false,
                                            "properties", Map.of(
                                                    "mustMatchAdVariant", Map.of(
                                                            "type", "string",
                                                            "enum", List.of("dor", "resultado", "prova")
                                                    ),
                                                    "visualAngle", Map.of(
                                                            "type", "string",
                                                            "enum", List.of("dor", "resultado", "prova")
                                                    ),
                                                    "assetType", Map.of(
                                                            "type", "string",
                                                            "enum", List.of("estatico", "carrossel", "story-vertical")
                                                    ),
                                                    "imageTextMaxWords", Map.of(
                                                            "type", "integer",
                                                            "minimum", 3,
                                                            "maximum", 12
                                                    ),
                                                    "visualBriefing", Map.of("type", "string"),
                                                    "hierarchy", Map.of("type", "string"),
                                                    "formatByPlacement", Map.of("type", "string"),
                                                    "safeMargins", Map.of("type", "string"),
                                                    "complianceNotes", Map.of("type", "string"),
                                                    "messageMatchNotes", Map.of("type", "string")
                                            ),
                                            "required", List.of(
                                                    "mustMatchAdVariant",
                                                    "visualAngle",
                                                    "assetType",
                                                    "imageTextMaxWords",
                                                    "visualBriefing",
                                                    "hierarchy",
                                                    "formatByPlacement",
                                                    "safeMargins",
                                                    "complianceNotes",
                                                    "messageMatchNotes")
                                    )
                            )
                    ),
                    "required", List.of("briefings")
            ), metadataSchema);
            case LANDING_PAGE_COPY -> schemaWithMetadata("landingPageCopy", landingPageCopyFieldSchema(), metadataSchema);
            case LANDING_PAGE_WIREFRAME -> schemaWithMetadata("landingPageWireframe", landingPageWireframeFieldSchema(), metadataSchema);
        };
    }

    private Map<String, Object> landingPageCopyFieldSchema() {
        Map<String, Object> heroSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("eyebrow", stringSchema()),
                        Map.entry("headline", stringSchema()),
                        Map.entry("subheadline", stringSchema()),
                        Map.entry("promise", stringSchema()),
                        Map.entry("supportingCopy", stringSchema()),
                        Map.entry("proofBadge", stringSchema()),
                        Map.entry("microcopy", stringSchema()),
                        Map.entry("ctaLabel", stringSchema()),
                        Map.entry("ctaUrl", stringSchema()),
                        Map.entry("ctaMatchNotes", stringSchema())
                ),
                "required", List.of("headline", "promise", "ctaLabel", "ctaMatchNotes")
        );
        Map<String, Object> bodySectionSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("sectionId", stringSchema()),
                        Map.entry("sectionType", Map.of(
                                "type", "string",
                                "enum", List.of("hero", "pain", "mechanism", "proof", "offer", "cta", "faq", "bonus", "objection")
                        )),
                        Map.entry("title", stringSchema()),
                        Map.entry("summary", stringSchema()),
                        Map.entry("bullets", arrayOfStringsSchema(0)),
                        Map.entry("copy", stringSchema()),
                        Map.entry("ctaSupport", stringSchema()),
                        Map.entry("sectionDependsOn", stringSchema()),
                        Map.entry("messageMatchNotes", stringSchema())
                ),
                "required", List.of("sectionId", "sectionType", "title", "summary", "messageMatchNotes")
        );
        Map<String, Object> ctaBlockSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("placement", Map.of(
                                "type", "string",
                                "enum", List.of("hero", "mid", "final", "sticky", "inline")
                        )),
                        Map.entry("ctaVariant", Map.of(
                                "type", "string",
                                "enum", List.of("primary", "secondary", "ghost", "sticky")
                        )),
                        Map.entry("ctaLabel", stringSchema()),
                        Map.entry("ctaUrl", stringSchema()),
                        Map.entry("matchAdCta", stringSchema()),
                        Map.entry("ctaSupport", stringSchema()),
                        Map.entry("messageMatchNotes", stringSchema())
                ),
                "required", List.of("placement", "ctaVariant", "ctaLabel", "matchAdCta")
        );
        Map<String, Object> faqSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("question", stringSchema()),
                        Map.entry("answer", stringSchema()),
                        Map.entry("objectionTag", stringSchema())
                ),
                "required", List.of("question", "answer")
        );
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("pageGoal", stringSchema()),
                        Map.entry("messageMatchSource", stringSchema()),
                        Map.entry("messageMatchNotes", stringSchema()),
                        Map.entry("primaryCTA", stringSchema()),
                        Map.entry("hero", heroSchema),
                        Map.entry("bodySections", Map.of(
                                "type", "array",
                                "minItems", 4,
                                "items", bodySectionSchema
                        )),
                        Map.entry("ctaBlocks", Map.of(
                                "type", "array",
                                "minItems", 2,
                                "items", ctaBlockSchema
                        )),
                        Map.entry("faq", Map.of(
                                "type", "array",
                                "minItems", 3,
                                "items", faqSchema
                        )),
                        Map.entry("consistencyChecks", Map.of(
                                "type", "array",
                                "minItems", 2,
                                "items", consistencyCheckSchema()
                        )),
                        Map.entry("complianceNotes", stringSchema())
                ),
                "required", List.of("pageGoal", "messageMatchSource", "primaryCTA", "hero", "bodySections", "ctaBlocks", "consistencyChecks")
        );
    }

    private Map<String, Object> landingPageWireframeFieldSchema() {
        Map<String, Object> ctaSlotSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("hasCta", Map.of("type", "boolean")),
                        Map.entry("ctaLabel", stringSchema()),
                        Map.entry("ctaVariant", Map.of(
                                "type", "string",
                                "enum", List.of("hero", "mid", "final", "sticky", "inline")
                        )),
                        Map.entry("matchAdCta", stringSchema()),
                        Map.entry("notes", stringSchema())
                ),
                "required", List.of("hasCta")
        );
        Map<String, Object> sectionSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("sectionId", stringSchema()),
                        Map.entry("sectionName", stringSchema()),
                        Map.entry("objective", stringSchema()),
                        Map.entry("contentType", Map.of(
                                "type", "string",
                                "enum", List.of("hero", "form", "split", "proof", "timeline", "faq", "cta")
                        )),
                        Map.entry("copySource", stringSchema()),
                        Map.entry("uiNotes", stringSchema()),
                        Map.entry("messageMatchDependency", stringSchema()),
                        Map.entry("sectionDependsOn", stringSchema()),
                        Map.entry("mobilePriorityScore", integerSchema(1, 10)),
                        Map.entry("dropOffRisk", Map.of("type", "string", "enum", List.of("baixo", "medio", "alto"))),
                        Map.entry("ctaSlot", ctaSlotSchema)
                ),
                "required", List.of("sectionId", "sectionName", "objective", "contentType", "mobilePriorityScore", "dropOffRisk")
        );
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("pageGoal", stringSchema()),
                        Map.entry("variantLayoutId", Map.of(
                                "type", "string",
                                "enum", List.of("form-first", "proof-first", "story-first")
                        )),
                        Map.entry("messageMatchSummary", stringSchema()),
                        Map.entry("sectionOrder", Map.of(
                                "type", "array",
                                "minItems", 4,
                                "items", sectionSchema
                        )),
                        Map.entry("mobilePriorityNotes", stringSchema()),
                        Map.entry("ctaPlacementNotes", stringSchema()),
                        Map.entry("formPlacementNotes", stringSchema()),
                        Map.entry("consistencyChecks", Map.of(
                                "type", "array",
                                "minItems", 2,
                                "items", consistencyCheckSchema()
                        ))
                ),
                "required", List.of("pageGoal", "variantLayoutId", "sectionOrder", "consistencyChecks")
        );
    }

    private Map<String, Object> consistencyCheckSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("check", stringSchema()),
                        Map.entry("status", Map.of("type", "string", "enum", List.of("PASS", "WARN", "FAIL"))),
                        Map.entry("details", stringSchema())
                ),
                "required", List.of("check", "status")
        );
    }

    private Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    private Map<String, Object> arrayOfStringsSchema(int minItems) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", stringSchema());
        if (minItems > 0) {
            schema.put("minItems", minItems);
        }
        return schema;
    }

    private Map<String, Object> integerSchema(int min, int max) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "integer");
        schema.put("minimum", min);
        schema.put("maximum", max);
        return schema;
    }

    private Map<String, Object> experimentMetadataSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "primary_variable", Map.of("type", "string"),
                        "variant_id", Map.of("type", "string"),
                        "stage", Map.of("type", "string"),
                        "control_or_treatment", Map.of("type", "string"),
                        "asset_role", Map.of("type", "string")
                ),
                "required", List.of("primary_variable", "variant_id", "stage", "control_or_treatment", "asset_role")
        );
    }

    private Map<String, Object> schemaWithMetadata(String fieldName,
                                                   Map<String, Object> fieldSchema,
                                                   Map<String, Object> metadataSchema) {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        fieldName, fieldSchema,
                        "experimentMetadata", metadataSchema
                ),
                "required", List.of(fieldName, "experimentMetadata")
        );
    }
}
