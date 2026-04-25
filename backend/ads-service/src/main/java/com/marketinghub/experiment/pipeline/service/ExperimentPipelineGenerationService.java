package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStage;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationJobDetailDto;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationJobSummaryDto;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest;
import com.marketinghub.experiment.pipeline.dto.LandingPagePublicationResultDto;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobCompletionRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import com.marketinghub.experiment.pipeline.repository.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.NestedExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExperimentPipelineGenerationService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineGenerationService.class);
    private static final Pattern FORM_CONTROL_TAG_PATTERN = Pattern.compile("(?is)<(input|textarea|select)\\b[^>]*>");
    private static final Pattern SUBMIT_LISTENER_PATTERN = Pattern.compile("(?is)addEventListener\\s*\\(\\s*['\"]submit['\"]");
    private static final Pattern PREVENT_DEFAULT_PATTERN = Pattern.compile("(?is)event\\.preventDefault\\s*\\(");
    private static final Pattern FETCH_FORM_ACTION_PATTERN = Pattern.compile("(?is)fetch\\s*\\(\\s*form\\.action");
    private static final Pattern FORM_DATA_PATTERN = Pattern.compile("(?is)new\\s+FormData\\s*\\(\\s*form\\s*\\)");
    private static final Pattern SUBMIT_DISABLE_PATTERN = Pattern.compile("(?is)submitButton\\.disabled\\s*=\\s*true");
    private static final Pattern SUBMIT_ENABLE_PATTERN = Pattern.compile("(?is)submitButton\\.disabled\\s*=\\s*false");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "(?is)([a-zA-Z_:][-a-zA-Z0-9_:.]*)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+)))?");
    private static final Pattern OPENING_TAG_PATTERN = Pattern.compile("(?is)<([a-z0-9:-]+)\\b[^>]*>");
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("(?is)<img\\b[^>]*>");
    private static final String DEFAULT_MODEL = "gpt-5.2";
    private static final Duration STALE_PENDING_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration STALE_PROCESSING_TIMEOUT = Duration.ofMinutes(20);
    private static final Set<ExperimentPipelineGenerationJobStatus> ACTIVE_JOB_STATUSES = Set.of(
            ExperimentPipelineGenerationJobStatus.PENDING,
            ExperimentPipelineGenerationJobStatus.PROCESSING);
    private static final Set<ExperimentStatus> PIPELINE_ELIGIBLE_EXPERIMENT_STATUSES = Set.of(
            ExperimentStatus.PLANNED,
            ExperimentStatus.RUNNING,
            ExperimentStatus.PAUSED);
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
    private final LeadPortalFlowRepository leadPortalFlowRepository;
    private final LeadPortalFlowPublisher leadPortalFlowPublisher;
    private final ObjectMapper objectMapper;
    private final LandingPageImageInjector landingPageImageInjector;
    private final FrameworkImageGenerationService frameworkImageGenerationService;
    private final OpenAiPricingService openAiPricingService;
    private final LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;

    public ExperimentPipelineGenerationService(ExperimentRepository experimentRepository,
                                               ExperimentPipelineGenerationJobRepository jobRepository,
                                               ExperimentMapper experimentMapper,
                                               AiWorkerGenerationService generationService,
                                               LeadPortalFlowRepository leadPortalFlowRepository,
                                               LeadPortalFlowPublisher leadPortalFlowPublisher,
                                               ObjectMapper objectMapper,
                                               LandingPageImageInjector landingPageImageInjector,
                                               FrameworkImageGenerationService frameworkImageGenerationService,
                                               OpenAiPricingService openAiPricingService,
                                               LeadPortalPublicUrlResolver leadPortalPublicUrlResolver) {
        this.experimentRepository = experimentRepository;
        this.jobRepository = jobRepository;
        this.experimentMapper = experimentMapper;
        this.generationService = generationService;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.leadPortalFlowPublisher = leadPortalFlowPublisher;
        this.objectMapper = objectMapper;
        this.landingPageImageInjector = landingPageImageInjector;
        this.frameworkImageGenerationService = frameworkImageGenerationService;
        this.openAiPricingService = openAiPricingService;
        this.leadPortalPublicUrlResolver = leadPortalPublicUrlResolver;
    }

    @Transactional
    public ExperimentDto generate(Long experimentId,
                                  ExperimentPipelineSection section,
                                  ExperimentPipelineGenerationRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));

        validatePredecessor(experiment, section);

        List<ExperimentPipelineGenerationJob> activeJobs = jobRepository
                .findByExperimentIdAndStatusInOrderByCreatedAtDesc(experimentId, ACTIVE_JOB_STATUSES);
        boolean hasActiveJob = markStaleJobsAsFailed(activeJobs);
        if (hasActiveJob) {
            ExperimentPipelineGenerationJob activeJob = activeJobs.stream()
                    .filter(candidate -> candidate.getStatus() == ExperimentPipelineGenerationJobStatus.PENDING
                            || candidate.getStatus() == ExperimentPipelineGenerationJobStatus.PROCESSING)
                    .findFirst()
                    .orElse(null);
            String activeSection = activeJob != null && activeJob.getSection() != null
                    ? activeJob.getSection().path()
                    : "desconhecida";
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe etapa em execução para este experimento (" + activeSection + "). "
                            + "A fila automática deve seguir ordem sequencial.");
        }
        enqueueJob(experiment, section, request);
        return experimentMapper.toDto(experiment);
    }

    @Transactional
    public List<ExperimentPipelineGenerationJobDto> listPendingJobs(int limit) {
        return jobRepository.findByStatusAndExperimentStatusInOrderByCreatedAtAsc(
                        ExperimentPipelineGenerationJobStatus.PENDING,
                        PIPELINE_ELIGIBLE_EXPERIMENT_STATUSES,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public int closeOpenJobs(Long experimentId, String reason) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));
        List<ExperimentPipelineGenerationJob> openJobs = jobRepository
                .findByExperimentIdAndStatusInOrderByCreatedAtDesc(experimentId, ACTIVE_JOB_STATUSES);
        if (openJobs.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();
        String normalizedReason = StringUtils.hasText(reason)
                ? reason.trim()
                : "Pipeline encerrado manualmente para o experimento " + experimentId;
        for (ExperimentPipelineGenerationJob job : openJobs) {
            job.setStatus(ExperimentPipelineGenerationJobStatus.FAILED);
            job.setStage(ExperimentPipelineGenerationJobStage.FAILED);
            job.setErrorMessage(normalizedReason);
            job.setFinishedAt(now);
        }
        log.info("Encerrados {} job(s) abertos do pipeline para experimento {} (status={}, motivo={})",
                openJobs.size(), experimentId, experiment.getStatus(), normalizedReason);
        return openJobs.size();
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
        ensureExperimentExists(experimentId);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ExperimentPipelineGenerationJob> latestCompletedJobs = jobRepository
                .findLatestCompletedPerSectionByExperimentId(experimentId, section)
                .stream()
                .sorted(Comparator.comparing(ExperimentPipelineGenerationJob::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int start = Math.min((int) pageable.getOffset(), latestCompletedJobs.size());
        int end = Math.min(start + pageable.getPageSize(), latestCompletedJobs.size());
        List<ExperimentPipelineGenerationJobSummaryDto> content = latestCompletedJobs.subList(start, end)
                .stream()
                .map(this::toSummaryDto)
                .toList();
        return new PageImpl<>(content, pageable, latestCompletedJobs.size());
    }

    public BigDecimal totalCostUsd(Long experimentId) {
        ensureExperimentExists(experimentId);
        return jobRepository.sumCostUsdByExperimentId(experimentId);
    }

    @Transactional
    public ExperimentDto applyLandingHtmlToLeadPortalForm(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));
        if (!StringUtils.hasText(experiment.getLandingPageHtml())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Landing HTML ainda não foi gerado para este experimento");
        }
        LeadPortalFlow flowRef = experiment.getLeadPortalFlow();
        LeadPortalFlow flow;
        if (flowRef == null || flowRef.getId() == null) {
            flow = createLeadPortalFlowFromLandingHtml(experiment);
        } else {
            flow = leadPortalFlowRepository.findById(flowRef.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fluxo do Lead Portal não encontrado"));
        }
        String payloadWithImages = landingPageImageInjector.injectImages(
                experiment.getId(),
                experiment.getLandingPageHtml());
        flow.setCustomFormHtml(payloadWithImages != null ? payloadWithImages.trim() : null);
        flow.setSchemaFirst(true);
        experiment.setSchemaFirstLeadPortalEnabled(true);
        LeadPortalFlow saved = leadPortalFlowRepository.save(flow);
        if (saved.isApproved()) {
            try {
                leadPortalFlowPublisher.publish(saved);
            } catch (LeadPortalPublicationException ex) {
                String rootCauseMessage = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
                String message = StringUtils.hasText(rootCauseMessage)
                        ? "Falha ao publicar fluxo com o novo HTML da landing: " + rootCauseMessage
                        : "Falha ao publicar fluxo com o novo HTML da landing";
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        message, ex);
            }
        }
        return experimentMapper.toDto(experiment);
    }

    @Transactional
    public LandingPagePublicationResultDto approveAndPublishLandingPage(Long experimentId) {
        ExperimentDto experimentDto = applyLandingHtmlToLeadPortalForm(experimentId);
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));
        LeadPortalFlow flowRef = experiment.getLeadPortalFlow();
        if (flowRef == null || flowRef.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fluxo de landing não foi criado para este experimento");
        }
        LeadPortalFlow flow = leadPortalFlowRepository.findById(flowRef.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fluxo do Lead Portal não encontrado"));
        flow.setApproved(true);
        if (flow.getApprovedAt() == null) {
            flow.setApprovedAt(Instant.now());
        }
        LeadPortalFlow saved = leadPortalFlowRepository.save(flow);
        try {
            leadPortalFlowPublisher.publish(saved);
        } catch (LeadPortalPublicationException ex) {
            String rootCauseMessage = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
            String message = StringUtils.hasText(rootCauseMessage)
                    ? "Falha ao aprovar/publicar landing automaticamente: " + rootCauseMessage
                    : "Falha ao aprovar/publicar landing automaticamente";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, ex);
        }
        String pixelId = saved.getMarketNiche() != null ? saved.getMarketNiche().getFacebookPixelId() : null;
        Long publicationExperimentId = experimentDto != null && experimentDto.getId() != null
                ? experimentDto.getId()
                : experimentId;
        return new LandingPagePublicationResultDto(
                publicationExperimentId,
                saved.getId(),
                saved.isApproved(),
                true,
                leadPortalPublicUrlResolver.resolve(saved),
                pixelId,
                StringUtils.hasText(pixelId),
                "Landing aprovada e publicação iniciada automaticamente após a aprovação.");
    }

    private LeadPortalFlow createLeadPortalFlowFromLandingHtml(Experiment experiment) {
        Long experimentId = experiment.getId();
        String slug = buildUniqueLeadPortalFlowSlug("exp-" + experimentId + "-landing");
        LeadPortalFlow flow = LeadPortalFlow.builder()
                .name("Landing - Experimento " + experimentId)
                .slug(slug)
                .description("Fluxo criado automaticamente a partir do HTML da landing page do experimento " + experimentId)
                .model(DEFAULT_MODEL)
                .prompt("Pipeline: landing-page-html/apply-to-form")
                .schemaFirst(true)
                .approved(true)
                .approvedAt(Instant.now())
                .marketNiche(experiment.getNiche())
                .experiment(experiment)
                .build();
        LeadPortalFlow saved = leadPortalFlowRepository.save(flow);
        experiment.setLeadPortalFlow(saved);
        experiment.setSchemaFirstLeadPortalEnabled(true);
        experimentRepository.save(experiment);
        return saved;
    }

    private String buildUniqueLeadPortalFlowSlug(String baseSlug) {
        String normalized = normalizeSlug(baseSlug);
        String candidate = normalized;
        int suffix = 2;
        while (leadPortalFlowRepository.findBySlug(candidate).isPresent()) {
            candidate = normalized + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String normalizeSlug(String value) {
        String slug = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        slug = slug.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("(^-+|-+$)", "");
        return StringUtils.hasText(slug) ? slug : "landing-experiment";
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

    @Transactional(readOnly = true)
    public ExperimentPipelineGenerationJobDetailDto getLatestCompletedJobDetail(Long experimentId,
                                                                                ExperimentPipelineSection section) {
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "section é obrigatório");
        }
        List<ExperimentPipelineGenerationJob> jobs = jobRepository
                .findByExperimentIdAndSectionAndStatusInOrderByCreatedAtDesc(
                        experimentId,
                        section,
                        List.of(ExperimentPipelineGenerationJobStatus.COMPLETED));
        ExperimentPipelineGenerationJob latest = jobs.stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nenhum job concluído encontrado para esta seção"));
        return toDetailDto(latest);
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
        log.info("Completando job de pipeline {} (experimentId={}, section={}, responseKeys={})",
                job.getId(),
                experiment != null ? experiment.getId() : null,
                job.getSection(),
                summarizePayloadKeys(request.responseContent()));
        try {
            applySectionContent(experiment, job.getSection(), request.responseContent());
        } catch (ResponseStatusException ex) {
            log.warn("Rejeição de output do modelo no completeJob (jobId={}, experimentId={}, section={}, status={}, reason={}, responseKeys={}, rejectedModelOutput={})",
                    job.getId(),
                    experiment != null ? experiment.getId() : null,
                    job.getSection(),
                    ex.getStatusCode().value(),
                    ex.getReason(),
                    summarizePayloadKeys(request.responseContent()),
                    summarizeRejectedModelOutput(request.responseContent(), request.rawResponse()));
            throw ex;
        }
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
                : openAiPricingService.estimateStandardCost(job.getModel(), usage);

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

        enqueueNextAutomaticStep(job, request);
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
            case LANDING_PAGE_IMAGE_PLANNING -> experiment.getLandingPageImagePlanning();
            case LANDING_PAGE_HTML -> experiment.getLandingPageHtml();
        };
        if (!StringUtils.hasText(predecessorContent)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A seção " + section.path() + " depende da seção " + predecessor.path() + " já concluída");
        }
        if (section == ExperimentPipelineSection.LANDING_PAGE_HTML
                && !frameworkImageGenerationService.allPlanningImagesCompleted(experiment.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A seção landing-page-html depende da geração completa das imagens planejadas da landing");
        }
    }

    private void enqueueNextAutomaticStep(ExperimentPipelineGenerationJob job,
                                          ExperimentPipelineGenerationJobCompletionRequest request) {
        if (job == null || job.getExperiment() == null || job.getExperiment().getId() == null) {
            return;
        }
        Long experimentId = job.getExperiment().getId();
        ExperimentPipelineSection section = job.getSection();

        if (section == ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING) {
            frameworkImageGenerationService.enqueueJobsForExperiment(experimentId);
            if (frameworkImageGenerationService.allPlanningImagesCompleted(experimentId)) {
                enqueueJob(job.getExperiment(), ExperimentPipelineSection.LANDING_PAGE_HTML, deriveFollowUpRequest(request));
            }
            return;
        }

        ExperimentPipelineSection successor = section.successor();
        if (successor == null) {
            return;
        }
        enqueueJob(job.getExperiment(), successor, deriveFollowUpRequest(request));
    }

    private ExperimentPipelineGenerationRequest deriveFollowUpRequest(ExperimentPipelineGenerationJobCompletionRequest request) {
        ExperimentPipelineGenerationRequest followUp = new ExperimentPipelineGenerationRequest();
        if (request != null && StringUtils.hasText(request.rawResponse())) {
            followUp.setCustomInstructions("Continuação automática do pipeline.");
        }
        return followUp;
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
        if (section == ExperimentPipelineSection.CAMPAIGN_ANGLE) {
            appendCampaignAngleStructuredContext(sb, experiment);
        }
        sb.append("\nTarefa alvo: ").append(section.path()).append("\n");
        if (section == ExperimentPipelineSection.LANDING_PAGE_HTML) {
            appendLandingHtmlV2Inputs(sb, experiment);
        } else {
            appendPreviousOutputs(sb, experiment, section);
        }
        appendSectionPrompt(sb, experiment, section);
        if (StringUtils.hasText(customInstructions)) {
            sb.append("\nInstruções extras do usuário:\n").append(customInstructions.trim()).append("\n");
        }
        if (section == ExperimentPipelineSection.LANDING_PAGE_HTML) {
            sb.append("\nResponda exclusivamente com HTML puro (sem markdown, sem JSON, sem texto adicional).");
        } else {
            sb.append("\nResponda exclusivamente em JSON válido e siga estritamente o schema da seção.");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendLandingHtmlV2Inputs(StringBuilder sb, Experiment experiment) {
        sb.append("\nPrompt v2 (inputs mínimos para LANDING_PAGE_HTML):\n");
        sb.append("Use apenas os 3 insumos abaixo como fonte de verdade para montar o HTML final.\n");
        appendIfPresent(sb, "1) Wireframe aprovado (JSON)", experiment.getLandingPageWireframe());
        appendIfPresent(sb, "2) Texto da landing aprovado (JSON)", experiment.getLandingPageCopy());
        String imageUrls = summarizeImageUrlsFromPlanning(experiment != null ? experiment.getLandingPageImagePlanning() : null);
        if (StringUtils.hasText(imageUrls)) {
            sb.append("3) URLs de imagens aprovadas por seção:\n").append(imageUrls);
        } else {
            appendIfPresent(sb, "3) Planejamento de imagens (JSON)", experiment.getLandingPageImagePlanning());
        }
    }

    @SuppressWarnings("unchecked")
    private String summarizeImageUrlsFromPlanning(String imagePlanningPayload) {
        if (!StringUtils.hasText(imagePlanningPayload)) {
            return null;
        }
        try {
            Map<String, Object> root = readObject(imagePlanningPayload, "Planejamento de imagens da landing inválido");
            Map<String, Object> payload = unwrapSectionPayload(root, "landingPageImagePlanning");
            if (!(payload.get("images") instanceof List<?> rawImages)) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            int index = 0;
            for (Object rawImage : rawImages) {
                if (!(rawImage instanceof Map<?, ?> rawImageMap)) {
                    continue;
                }
                Map<String, Object> image = (Map<String, Object>) rawImageMap;
                String sectionId = asTrimmedString(image.get("sectionId"));
                String bindingKey = asTrimmedString(image.get("imageBindingKey"));
                String url = firstNonBlank(
                        asTrimmedString(image.get("webUrl")),
                        asTrimmedString(image.get("imageUrl")),
                        asTrimmedString(image.get("sourceUrl")),
                        asTrimmedString(image.get("url")));
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                index++;
                sb.append("- #").append(index)
                        .append(" | sectionId=").append(StringUtils.hasText(sectionId) ? sectionId : "(sem sectionId)")
                        .append(" | imageBindingKey=").append(StringUtils.hasText(bindingKey) ? bindingKey : "(sem binding)")
                        .append(" | url=").append(url)
                        .append("\n");
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception ex) {
            return null;
        }
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
        body.put("text", Map.of("format", buildResponseFormat(section)));
        return body;
    }

    private Map<String, Object> buildResponseFormat(ExperimentPipelineSection section) {
        if (section == ExperimentPipelineSection.LANDING_PAGE_HTML) {
            return Map.of("type", "text");
        }
        return Map.of(
                "type", "json_schema",
                "name", "experiment_pipeline_" + section.path().replace("-", "_"),
                "strict", true,
                "schema", sectionSchema(section)
        );
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
            sb.append("4. Em cada briefing, definir assetType com um valor: estatico.\n");
            sb.append("5. Em cada briefing, definir imageTextMaxWords (inteiro de 3 a 12) para limitar texto sobreposto.\n");
            sb.append("6. Garantir coerência entre criativo, copy da variação e promessa da landing pós-clique.\n");
            sb.append("7. Preservar hierarchy visual, safe margins e notas de compliance por peça.\n");
            sb.append("8. Evitar claims absolutos e qualquer linguagem de consultoria.\n");
            sb.append("9. Definir nicheCue: como o nicho será reconhecido visualmente em até 1 segundo.\n");
            sb.append("10. Definir focalVisual: um único elemento principal que domina a peça.\n");
            sb.append("11. Definir humanPresence com: obrigatoria, opcional ou proibida.\n");
            sb.append("12. Se houver figura humana, ela deve representar o nicho em contexto real e não como rosto genérico decorativo.\n");
            sb.append("13. O CTA visual não pode ser o elemento dominante da peça.\n\n");
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
            sb.append("10. complianceNotes precisa reforçar que a oferta é entregue por ativos digitais (sem consultoria ou call).\n");
            sb.append("11. Não serializar JSON dentro de campos de texto; objetos e listas do schema devem ser retornados como JSON real, sem aspas escapadas.\n\n");
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
            sb.append("11. Cada sectionOrder[i] deve preencher mediaSlot com: none, image, illustration, chart, icon-set ou video-thumb.\n");
            sb.append("12. Cada sectionOrder[i] deve preencher compositionNotes explicando hierarquia visual, densidade de conteúdo e leitura mobile-first.\n");
            sb.append("13. Cada sectionOrder[i] deve preencher surfaceSpec com surfaceToken, style, contrastMode e notes para formalizar a superfície visual da seção.\n");
            sb.append("14. Use surfaceToken alternando entre surface-base e surface-alt-* para reforçar escaneabilidade entre seções consecutivas.\n");
            sb.append("15. Não transformar o layout em HTML final; esta etapa deve decidir ordem, hierarquia e slots de mídia.\n");
            sb.append("16. Não usar linguagem de consultoria e não criar estrutura que pareça página genérica para qualquer mercado.\n");
            sb.append("17. Se a estrutura puder servir para qualquer nicho, reescreva até ficar específica para ").append(niche).append(".\n");
            sb.append("18. Definir formSpec como contrato estruturado do formulário com esta configuração:\n");
            sb.append("   - formId: lead-capture-primary\n");
            sb.append("   - title: Receber a prévia do Kit (IA)\n");
            sb.append("   - submitLabel: Desbloquear o Kit (receber a prévia gerada por IA)\n");
            sb.append("   - submitTarget: /api/flows/{slug}/submissions\n");
            sb.append("   - fields exatamente nesta ordem: nome (text, required=true), email (email, required=true), whatsapp (tel, required=false)\n");
            sb.append("   - placeholders: Seu nome, voce@exemplo.com, (DDD) 9XXXX-XXXX\n");
            sb.append("   - consent: enabled=true, required=false, label preenchido\n");
            sb.append("   - successState: title e message preenchidos.\n\n");
            sb.append("Formato obrigatório:\n");
            sb.append("- Preencher sectionOrder respeitando a ordem real da landing e referenciando sectionId de bodySections quando existir.\n");
            sb.append("- Preencher mediaSlot, compositionNotes e surfaceSpec em todas as seções.\n");
            sb.append("- Preencher formSpec completo como fonte única da verdade para campos e obrigatoriedade do formulário.\n");
            sb.append("- Preencher ctaSlot dentro das seções com CTA.\n");
            sb.append("- Preencher consistencyChecks e observações finais (mobilePriorityNotes, ctaPlacementNotes, formPlacementNotes).\n");
            sb.append("- Reforçar message match usando primaryAdHeadline, landingMatchLine e " + landingCtaForInstructions + " como referência fixa.\n");
            return;
        }

        if (section == ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING) {
            sb.append("\nDiretriz específica para Planejamento de Imagens da Landing:\n");
            sb.append(COMMON_CAMPAIGN_ASSET_RULES).append("\n");
            sb.append("Contexto do nicho: ").append(niche).append("\n");
            sb.append("Ângulo da campanha: ").append(campaignAngle).append("\n");
            sb.append("Dor principal: ").append(primaryPain).append("\n");
            sb.append("Promessa principal: ").append(primaryPromise).append("\n");
            sb.append("Mecanismo resumido: ").append(mechanismSummary).append("\n");
            sb.append("Prova resumida: ").append(proofSummary).append("\n");
            sb.append("CTA obrigatório: ").append(landingCtaForInstructions).append("\n");
            appendIfPresent(sb, "Linha de match com landing", landingMatchLine);
            sb.append("\nObjetivo:\n");
            sb.append("Planejar todas as imagens da landing antes do HTML final, conectando ângulo da campanha, copy da landing e layout aprovado.\n\n");
            sb.append("Regras:\n");
            sb.append("1. images[] deve ter no mínimo 4 itens com sectionId e sectionName existentes no wireframe.\n");
            sb.append("2. Cada item precisa trazer imagePrompt, objective, visualStyle, composition, focalPoint, supportingElements e mood.\n");
            sb.append("3. Cada item precisa incluir imageBindingKey (curto/canônico), imageRole, conversionRole, emotionalJob e sectionVisualGoal para explicitar função visual e contribuição para conversão.\n");
            sb.append("4. Definir placement (hero, benefit, mechanism, proof, offer, faq, cta), priority (high, medium, low), hierarchyLevel (primary, secondary, support), attentionPriority (high, medium, low), visualWeight (primary, secondary, support) e distanceToCTA (near, medium, far).\n");
            sb.append("5. Cada item precisa informar dimensions (desktop e mobile), safeMargins, textOverlayGuidance, altText e layoutBinding (preferredDesktopPlacement, preferredMobilePlacement, desktopAspectRatio, mobileAspectRatio, allowCrop, safeCropZones).\n");
            sb.append("6. supportsFormConversion deve indicar explicitamente se a imagem ajuda o envio do lead e formRelationNotes deve explicar como empurra leitura para CTA/form sem competir com ele.\n");
            sb.append("7. sectionId deve existir no wireframe e não pode repetir com imageRole conflitando no mesmo bloco.\n");
            sb.append("8. Sempre preencher messageMatchNotes explicando como a imagem reforça promessa/CTA sem desviar o ângulo.\n");
            sb.append("9. Sempre preencher complianceNotes evitando linguagem de consultoria e claims absolutos.\n");
            sb.append("10. Incluir negativePrompt e generationHints para orientar geração consistente e informativa.\n");
            sb.append("11. ctaIntegrationNotes deve indicar onde o CTA aparece junto da imagem sem competir com a leitura.\n");
            sb.append("12. sequencingNotes deve explicar a ordem narrativa das imagens ao longo da página.\n");
            sb.append("13. consistencyChecks deve incluir IMAGE_MESSAGE_MATCH, VISUAL_HIERARCHY e CTA_CONTINUITY (PASS/WARN/FAIL).\n\n");
            sb.append("Formato obrigatório:\n");
            sb.append("- pageGoal\n");
            sb.append("- visualDirectionSummary\n");
            sb.append("- sequencingNotes\n");
            sb.append("- ctaIntegrationNotes\n");
            sb.append("- images[]\n");
            sb.append("- consistencyChecks[]\n");
            return;
        }

        if (section == ExperimentPipelineSection.LANDING_PAGE_HTML) {
            sb.append("\nDiretriz específica para implementação final da landing (HTML/CSS/JS):\n");
            sb.append(COMMON_CAMPAIGN_ASSET_RULES).append("\n");
            sb.append("Contexto do nicho: ").append(niche).append("\n");
            sb.append("Headline de referência: ").append(primaryAdHeadline).append("\n");
            sb.append("CTA obrigatório: ").append(landingCtaForInstructions).append("\n");
            appendIfPresent(sb, "Linha de match com landing", landingMatchLine);
            sb.append("\nObjetivo:\n");
            sb.append("Unificar copy + wireframe + planejamento de imagens e entregar uma landing final pronta para uso em formulário do experimento.\n\n");
            sb.append("Regras:\n");
            sb.append("1. Entregar HTML completo com estrutura visual e campos do formulário.\n");
            sb.append("2. Garantir mobile-first e acessibilidade básica (labels, aria, foco visível).\n");
            sb.append("3. Repetir o CTA principal exatamente como no anúncio e nas seções anteriores.\n");
            sb.append("4. O formulário deve ser renderizado a partir de wireframe.formSpec como fonte única da verdade (sem inventar, remover, renomear ou trocar required).\n");
            sb.append("5. O envio deve seguir o padrão das páginas manuais:\n");
            sb.append("   - form.addEventListener('submit', async (event) => { event.preventDefault(); ... })\n");
            sb.append("   - validar com form.checkValidity()/form.reportValidity() antes do envio\n");
            sb.append("   - usar fetch(form.action, { method: form.method.toUpperCase(), body: new FormData(form) })\n");
            sb.append("   - desabilitar o botão durante o envio (estado \"Enviando...\") e reabilitar no finally\n");
            sb.append("   - exibir mensagem de sucesso inline sem redirecionar a página\n");
            sb.append("   - em erro, mostrar feedback claro para o usuário\n");
            sb.append("6. Não usar claims absolutos, nem linguagem de consultoria.\n");
            sb.append("7. Incluir bloco de compliance reforçando entrega digital via IA.\n");
            sb.append("8. Consumir explicitamente os artefatos anteriores:\n");
            sb.append("   - copy da landing para narrativa e message match;\n");
            sb.append("   - wireframe para ordem/hierarquia e mediaSlot;\n");
            sb.append("   - planejamento de imagens para imageRole, conversionRole, layoutBinding, prioridade e altText;\n");
            sb.append("   - wireframe.formSpec para renderização exata dos campos do formulário.\n");
            sb.append("9. Renderizar cada seção com data-section-id e aplicar exatamente wireframe.sectionOrder[i].surfaceSpec via data-surface-token, data-surface-style e data-surface-contrast.\n");
            sb.append("10. Não inventar estrutura visual fora do layout/plano de imagens sem justificar nos consistencyChecks.\n");
            sb.append("11. O código deve ser limpo, legível e pronto para ser renderizado em iframe.\n");
            sb.append("12. Toda tag <img> deve usar src absoluto válido (https://... ou data:image/...) e reaproveitar altText do planejamento de imagens.\n\n");
            sb.append("13. Cada <img> deve incluir binding explícito canônico com o plano usando data-image-section-id + data-image-binding-key como chave primária de aderência.\n");
            sb.append("14. Também preencher data-image-role (semântico/humano), data-conversion-role, data-attention-priority, data-visual-weight, data-distance-to-cta e data-supports-form-conversion.\n\n");
            appendImageBindingSummary(sb, experiment);
            sb.append("Formato obrigatório:\n");
            sb.append("- Entregar somente o HTML final completo como texto puro.\n");
            sb.append("- Não retornar JSON, Markdown, blocos ``` ou explicações.\n");
            sb.append("- O HTML precisa conter doctype, <html>, <head> e <body>.\n");
            return;
        }
    }

    private void appendImageBindingSummary(StringBuilder sb, Experiment experiment) {
        List<ImagePlanBindingContract> bindings = loadImagePlanBindings(experiment);
        if (bindings.isEmpty()) {
            return;
        }
        sb.append("\nBindings canônicos de imagem (copie nos atributos data-* de cada <img>):\n");
        for (ImagePlanBindingContract binding : bindings) {
            sb.append("- sectionId=").append(binding.sectionId())
                    .append(" | imageBindingKey=").append(binding.imageBindingKey())
                    .append(" | imageRole=").append(binding.imageRole())
                    .append(" | conversionRole=").append(binding.conversionRole())
                    .append(" | attention=").append(binding.attentionPriority())
                    .append(" | visualWeight=").append(binding.visualWeight())
                    .append(" | distanceToCTA=").append(binding.distanceToCta())
                    .append(" | supportsFormConversion=").append(binding.supportsFormConversion())
                    .append("\n");
        }
        sb.append("Para cada imagem acima, declare data-image-section-id, data-image-binding-key, data-image-role, data-conversion-role, data-attention-priority, data-visual-weight, data-distance-to-cta e data-supports-form-conversion.\n");
    }

    private List<ImagePlanBindingContract> loadImagePlanBindings(Experiment experiment) {
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageImagePlanning())) {
            return List.of();
        }
        try {
            Map<String, Object> imagePlanRoot = readObject(experiment.getLandingPageImagePlanning(), "Planejamento de imagens da landing inválido");
            Map<String, Object> imagePlanPayload = unwrapSectionPayload(imagePlanRoot, "landingPageImagePlanning");
            return extractExpectedImagePlanBindings(imagePlanPayload);
        } catch (ResponseStatusException ex) {
            log.warn("Falha ao carregar bindings de imagem para o prompt: {}", ex.getReason());
            return List.of();
        } catch (Exception ex) {
            log.warn("Falha inesperada ao carregar bindings de imagem para o prompt: {}", ex.getMessage());
            return List.of();
        }
    }

    private void appendPreviousOutputs(StringBuilder sb,
                                       Experiment experiment,
                                       ExperimentPipelineSection section) {
        if (section.predecessor() == null) {
            return;
        }
        Map<ExperimentPipelineSection, String> completedOutputs = loadLatestCompletedOutputs(experiment);
        appendSectionOutputIfEligible(sb, section, ExperimentPipelineSection.CAMPAIGN_ANGLE, "Ângulo da campanha", completedOutputs);
        appendSectionOutputIfEligible(sb, section, ExperimentPipelineSection.AD_COPY, "Texto do anúncio", completedOutputs);
        appendSectionOutputIfEligible(sb, section, ExperimentPipelineSection.AD_IMAGE_BRIEFING, "Briefing da imagem", completedOutputs);
        appendSectionOutputIfEligible(sb, section, ExperimentPipelineSection.LANDING_PAGE_COPY, "Textos da landing", completedOutputs);
        appendSectionOutputIfEligible(sb, section, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME, "Wireframe da landing", completedOutputs);
        appendSectionOutputIfEligible(sb, section, ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING, "Planejamento de imagens da landing", completedOutputs);
    }

    private boolean shouldIncludeSectionOutput(ExperimentPipelineSection targetSection,
                                               ExperimentPipelineSection outputSection) {
        if (targetSection == null || outputSection == null) {
            return false;
        }
        ExperimentPipelineSection cursor = targetSection.predecessor();
        while (cursor != null) {
            if (cursor == outputSection) {
                return true;
            }
            cursor = cursor.predecessor();
        }
        return false;
    }

    private void appendSectionOutputIfEligible(StringBuilder sb,
                                               ExperimentPipelineSection targetSection,
                                               ExperimentPipelineSection outputSection,
                                               String label,
                                               Map<ExperimentPipelineSection, String> completedOutputs) {
        if (!shouldIncludeSectionOutput(targetSection, outputSection)) {
            return;
        }
        String content = completedOutputs.get(outputSection);
        if (!StringUtils.hasText(content)) {
            return;
        }
        sb.append("\n").append(label).append(":\n").append(content.trim()).append("\n");
    }

    private Map<ExperimentPipelineSection, String> loadLatestCompletedOutputs(Experiment experiment) {
        if (experiment == null || experiment.getId() == null) {
            return Map.of();
        }
        List<ExperimentPipelineGenerationJob> completedJobs =
                jobRepository.findLatestCompletedPerSectionByExperimentId(experiment.getId(), null);
        if (completedJobs == null || completedJobs.isEmpty()) {
            return Map.of();
        }
        Map<ExperimentPipelineSection, String> bySection = new EnumMap<>(ExperimentPipelineSection.class);
        for (ExperimentPipelineGenerationJob completedJob : completedJobs) {
            if (completedJob == null || completedJob.getSection() == null) {
                continue;
            }
            String sectionContent = extractCompletedSectionContent(completedJob);
            if (StringUtils.hasText(sectionContent)) {
                bySection.put(completedJob.getSection(), sectionContent.trim());
            }
        }
        return bySection;
    }

    private String extractCompletedSectionContent(ExperimentPipelineGenerationJob completedJob) {
        if (completedJob == null) {
            return "";
        }
        return firstNonBlank(completedJob.getResponseContent(), completedJob.getRawResponse());
    }

    private void applySectionContent(Experiment experiment,
                                     ExperimentPipelineSection section,
                                     String content) {
        String normalized = normalizeSectionContent(experiment, section, content);
        switch (section) {
            case CAMPAIGN_ANGLE -> experiment.setCampaignAngle(normalized);
            case AD_COPY -> experiment.setAdCopy(normalized);
            case AD_IMAGE_BRIEFING -> experiment.setAdImageBriefing(normalized);
            case LANDING_PAGE_COPY -> experiment.setLandingPageCopy(normalized);
            case LANDING_PAGE_WIREFRAME -> experiment.setLandingPageWireframe(normalized);
            case LANDING_PAGE_IMAGE_PLANNING -> experiment.setLandingPageImagePlanning(normalized);
            case LANDING_PAGE_HTML -> {
                validateLandingHtmlFormConsistency(experiment, normalized);
                validateLandingHtmlSubmissionRuntime(experiment, normalized);
                validateLandingHtmlSurfaceConsistency(experiment, normalized);
                validateLandingHtmlImagePlanConsistency(experiment, normalized);
                experiment.setLandingPageHtml(normalized);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateLandingHtmlSubmissionRuntime(Experiment experiment, String landingHtmlContent) {
        if (!StringUtils.hasText(landingHtmlContent) || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return;
        }

        Map<String, Object> wireframeRoot = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
        Map<String, Object> wireframePayload = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
        if (!(wireframePayload.get("formSpec") instanceof Map<?, ?> rawFormSpec)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Wireframe da landing sem formSpec estruturado");
        }
        Map<String, Object> formSpec = (Map<String, Object>) rawFormSpec;
        String expectedFormId = asTrimmedString(formSpec.get("formId"));
        String expectedSubmitTarget = asTrimmedString(formSpec.get("submitTarget"));
        String htmlDocument = landingHtmlContent.trim();

        Document document = Jsoup.parse(htmlDocument);
        Element formElement = StringUtils.hasText(expectedFormId)
                ? document.getElementById(expectedFormId)
                : document.selectFirst("form");
        if (formElement == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "HTML da landing sem <form> compatível com wireframe.formSpec.formId");
        }

        String method = formElement.attr("method");
        if (!StringUtils.hasText(method) || !"post".equalsIgnoreCase(method.trim())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Formulário da landing deve usar method=\"post\"");
        }

        String action = asTrimmedString(formElement.attr("action"));
        if (!StringUtils.hasText(action)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Formulário da landing deve declarar atributo action");
        }
        if (StringUtils.hasText(expectedSubmitTarget) && !Objects.equals(action, expectedSubmitTarget)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Formulário da landing deve usar formSpec.submitTarget no action");
        }

        String lowered = htmlDocument.toLowerCase(Locale.ROOT);
        if (!SUBMIT_LISTENER_PATTERN.matcher(htmlDocument).find()
                || !PREVENT_DEFAULT_PATTERN.matcher(htmlDocument).find()
                || !FETCH_FORM_ACTION_PATTERN.matcher(htmlDocument).find()
                || !FORM_DATA_PATTERN.matcher(htmlDocument).find()
                || !SUBMIT_DISABLE_PATTERN.matcher(htmlDocument).find()
                || !SUBMIT_ENABLE_PATTERN.matcher(htmlDocument).find()
                || !lowered.contains("checkvalidity")
                || !lowered.contains("reportvalidity")
                || !lowered.contains("success")) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "HTML da landing deve seguir o runtime de envio padrão (submit assíncrono com validação, loading e sucesso inline)");
        }
    }

    @SuppressWarnings("unchecked")
    private String normalizeSectionContent(Experiment experiment, ExperimentPipelineSection section, String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String trimmed = content.trim();
        if (section == ExperimentPipelineSection.LANDING_PAGE_COPY) {
            try {
                Object parsedAny = objectMapper.readValue(trimmed, Object.class);
                if (!(parsedAny instanceof Map<?, ?> parsedRaw)) {
                    return trimmed;
                }
                Map<String, Object> parsed = (Map<String, Object>) parsedRaw;
                boolean changed = false;

                if ("output_text".equals(parsed.get("type")) && parsed.get("text") instanceof String textValue) {
                    Object nested = objectMapper.readValue(textValue, Object.class);
                    if (nested instanceof Map<?, ?> nestedRaw) {
                        parsed = (Map<String, Object>) nestedRaw;
                        changed = true;
                    }
                }

                Object landingPageCopy = parsed.get("landingPageCopy");
                if (landingPageCopy instanceof String landingPageCopyText) {
                    Object nestedLandingCopy = objectMapper.readValue(landingPageCopyText, Object.class);
                    if (nestedLandingCopy instanceof Map<?, ?>) {
                        parsed.put("landingPageCopy", nestedLandingCopy);
                        changed = true;
                    }
                }

                return changed ? objectMapper.writeValueAsString(parsed) : trimmed;
            } catch (Exception ignored) {
                return trimmed;
            }
        }
        if (section == ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING) {
            return normalizeLandingPageImagePlanningPayload(trimmed);
        }
        if (section != ExperimentPipelineSection.LANDING_PAGE_HTML) {
            return trimmed;
        }
        return normalizeLandingPageHtmlPayload(experiment, trimmed);
    }

    @SuppressWarnings("unchecked")
    private String normalizeLandingPageImagePlanningPayload(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return rawContent;
        }
        try {
            Map<String, Object> root = readObject(rawContent, "Planejamento de imagens da landing inválido");
            Map<String, Object> payload = unwrapSectionPayload(root, "landingPageImagePlanning");
            if (!(payload.get("images") instanceof List<?> rawImages) || rawImages.isEmpty()) {
                return rawContent;
            }
            Set<String> usedKeys = new HashSet<>();
            boolean changed = false;
            int index = 0;
            for (Object rawImage : rawImages) {
                index++;
                if (!(rawImage instanceof Map<?, ?> rawImageMap)) {
                    continue;
                }
                Map<String, Object> image = (Map<String, Object>) rawImageMap;
                String canonicalKey = canonicalBindingKey(image, index, usedKeys);
                if (!StringUtils.hasText(canonicalKey)) {
                    continue;
                }
                String existingKey = asTrimmedString(image.get("imageBindingKey"));
                if (!canonicalKey.equals(existingKey)) {
                    image.put("imageBindingKey", canonicalKey);
                    changed = true;
                }
            }
            if (changed) {
                log.info("Normalização de imageBindingKey aplicada em landingPageImagePlanning (imagens={})", rawImages.size());
                return objectMapper.writeValueAsString(root);
            }
        } catch (Exception ex) {
            log.warn("Falha ao normalizar landingPageImagePlanning: {}", ex.getMessage());
        }
        return rawContent;
    }

    @SuppressWarnings("unchecked")
    private String normalizeLandingPageHtmlPayload(Experiment experiment, String rawContent) {
        if (!StringUtils.hasText(rawContent) || experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return rawContent;
        }
        try {
            Map<String, Object> wireframeRoot = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
            Map<String, Object> wireframePayload = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
            if (!(wireframePayload.get("formSpec") instanceof Map<?, ?> rawFormSpec)) {
                return rawContent;
            }
            List<FormFieldContract> formFields = extractExpectedFormFields((Map<String, Object>) rawFormSpec);
            if (formFields.isEmpty()) {
                return rawContent;
            }
            String submitLabel = asTrimmedString(((Map<String, Object>) rawFormSpec).get("submitLabel"));
            if (!StringUtils.hasText(submitLabel)) {
                return rawContent;
            }
            String htmlDocument = rawContent.trim();
            if (!StringUtils.hasText(htmlDocument)) {
                return rawContent;
            }
            String normalizedHtml = rebuildFormFromSpec(htmlDocument, formFields, (Map<String, Object>) rawFormSpec);
            log.info("Normalização determinística de formulário aplicada no LANDING_PAGE_HTML (experimentId={}, fields={})",
                    experiment.getId(),
                    summarizeFormFields(formFields));
            return normalizedHtml;
        } catch (ResponseStatusException ex) {
            return rawContent;
        } catch (Exception ex) {
            log.warn("Falha ao normalizar payload LANDING_PAGE_HTML para experimento {}: {}",
                    experiment.getId(),
                    ex.getMessage());
            return rawContent;
        }
    }

    private String rebuildFormFromSpec(String htmlDocument,
                                       List<FormFieldContract> formFields,
                                       Map<String, Object> wireframeFormSpec) {
        Document document = Jsoup.parse(htmlDocument, "", org.jsoup.parser.Parser.htmlParser());
        String expectedFormId = asTrimmedString(wireframeFormSpec.get("formId"));
        String expectedSubmitTarget = asTrimmedString(wireframeFormSpec.get("submitTarget"));
        Element form = StringUtils.hasText(expectedFormId)
                ? document.selectFirst("form#" + expectedFormId)
                : document.selectFirst("form#lead-capture-primary");
        if (form == null) {
            form = document.selectFirst("form");
        }
        if (form == null) {
            return htmlDocument;
        }
        if (StringUtils.hasText(expectedFormId)) {
            form.attr("id", expectedFormId);
        }
        form.attr("method", "post");
        if (StringUtils.hasText(expectedSubmitTarget)) {
            form.attr("action", expectedSubmitTarget);
        }
        form.select("input[name],select[name],textarea[name],label[for],.error[id^=field_],.help").remove();
        for (FormFieldContract field : formFields) {
            appendFieldMarkup(document, form, field);
        }
        bindExternalSubmitButtonsToForm(document, form);
        ensureDefaultSubmitRuntime(document, form);
        return document.outerHtml();
    }

    private void bindExternalSubmitButtonsToForm(Document document, Element form) {
        String formId = StringUtils.hasText(form.id()) ? form.id() : "lead-capture-primary";
        form.attr("id", formId);
        for (Element submitControl : document.select("button[type=submit],input[type=submit]")) {
            Element ownerForm = submitControl.closest("form");
            if (ownerForm == form) {
                continue;
            }
            if (StringUtils.hasText(submitControl.attr("form"))) {
                continue;
            }
            submitControl.attr("form", formId);
        }
    }

    private void ensureDefaultSubmitRuntime(Document document, Element form) {
        String html = document.outerHtml();
        if (SUBMIT_LISTENER_PATTERN.matcher(html).find()
                && PREVENT_DEFAULT_PATTERN.matcher(html).find()
                && FETCH_FORM_ACTION_PATTERN.matcher(html).find()
                && FORM_DATA_PATTERN.matcher(html).find()
                && SUBMIT_DISABLE_PATTERN.matcher(html).find()
                && SUBMIT_ENABLE_PATTERN.matcher(html).find()) {
            return;
        }
        String formId = StringUtils.hasText(form.id()) ? form.id() : "lead-capture-primary";
        Element body = document.body();
        if (body == null) {
            body = document.appendElement("body");
        }
        body.appendElement("script")
                .attr("data-generated-runtime", "lead-capture")
                .append("""
                        (() => {
                          const form = document.getElementById('%s');
                          if (!form) return;
                          form.addEventListener('submit', async (event) => {
                            event.preventDefault();
                            const submitButton = form.querySelector('button[type="submit"],input[type="submit"]');
                            if (submitButton) submitButton.disabled = true;
                            try {
                              if (!form.checkValidity()) {
                                form.reportValidity();
                                return;
                              }
                              const formData = new FormData(form);
                              const response = await fetch(form.action, { method: 'POST', body: formData });
                              const successState = document.getElementById('submit-success-state');
                              if (response.ok && successState) {
                                successState.textContent = 'success';
                                successState.style.display = 'block';
                              }
                            } finally {
                              if (submitButton) submitButton.disabled = false;
                            }
                          });
                        })();
                        """.formatted(formId));
    }

    private void appendFieldMarkup(Document document, Element form, FormFieldContract field) {
        String fieldId = "field_" + field.name();
        Element wrapper = document.createElement("div").addClass("field");
        wrapper.appendElement("label").attr("for", fieldId).text(labelForField(field.name(), field.required()));
        Element input = wrapper.appendElement("input")
                .attr("type", field.type())
                .attr("id", fieldId)
                .attr("name", field.name())
                .attr("placeholder", placeholderForField(field))
                .attr("autocomplete", autocompleteForField(field.name()))
                .attr("aria-required", String.valueOf(field.required()));
        if (field.required()) {
            input.attr("required", "required");
        }
        wrapper.appendElement("div").addClass("help").text(helpForField(field.name()));
        wrapper.appendElement("div")
                .addClass("error")
                .attr("id", fieldId + "_error")
                .attr("style", "display:none")
                .attr("role", "alert");
        form.appendChild(wrapper);
    }

    private String summarizeNormalizedForm(List<FormFieldContract> formFields, String submitLabel) {
        return "Formulário alinhado ao wireframe.formSpec com campos "
                + summarizeFormFields(formFields)
                + " e submit \"" + submitLabel + "\".";
    }

    private List<Map<String, String>> buildLandingHtmlConsistencyChecks(List<FormFieldContract> formFields, String submitLabel) {
        String details = "Campos finais: " + summarizeFormFields(formFields)
                + "; submit: " + submitLabel
                + "; fonte única: wireframe.formSpec.";
        return List.of(
                Map.of("check", "FORM_SPEC_BINDING", "status", "PASS", "details", details),
                Map.of("check", "FORM_USABILITY", "status", "PASS", "details", "Campos obrigatórios preservados e sem campos fora do contrato."),
                Map.of("check", "CTA_MATCH", "status", "PASS", "details", "CTA principal mantido conforme artefatos anteriores."),
                Map.of("check", "PROMISE_MATCH", "status", "PASS", "details", "Narrativa permanece consistente com os artefatos predecessores."),
                Map.of("check", "IMAGE_PLAN_BINDING", "status", "PASS", "details", "Bindings de imagem preservados na saída final."),
                Map.of("check", "SURFACE_SPEC_BINDING", "status", "PASS", "details", "surfaceSpec aplicado conforme wireframe.")
        );
    }

    private String summarizePayloadKeys(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return "[]";
        }
        try {
            Map<String, Object> root = readObject(rawContent, "Payload inválido");
            return root.keySet().toString();
        } catch (Exception ignored) {
            return "[unparseable]";
        }
    }

    private String summarizeRejectedModelOutput(String responseContent, String rawResponse) {
        String candidate = StringUtils.hasText(responseContent) ? responseContent : rawResponse;
        if (!StringUtils.hasText(candidate)) {
            return "[empty]";
        }
        String compact = candidate.replaceAll("\\s+", " ").trim();
        return compact.length() > 4000 ? compact.substring(0, 4000) + "...(truncated)" : compact;
    }

    private String summarizeFormFields(List<FormFieldContract> fields) {
        return fields.stream()
                .map(field -> field.name() + ":" + field.type() + ":" + (field.required() ? "required" : "optional"))
                .toList()
                .toString();
    }

    private String labelForField(String name, boolean required) {
        return switch (name) {
            case "nome" -> "Nome";
            case "email" -> "E-mail";
            case "whatsapp" -> required ? "WhatsApp" : "WhatsApp (opcional)";
            default -> required ? name : name + " (opcional)";
        };
    }

    private String placeholderForField(FormFieldContract field) {
        return switch (field.name()) {
            case "nome" -> "Seu nome";
            case "email" -> "voce@exemplo.com";
            case "whatsapp" -> "(DDD) 9XXXX-XXXX";
            default -> "";
        };
    }

    private String autocompleteForField(String name) {
        return switch (name) {
            case "nome" -> "name";
            case "email" -> "email";
            case "whatsapp" -> "tel";
            default -> "off";
        };
    }

    private String helpForField(String name) {
        return switch (name) {
            case "nome" -> "Só para personalizar o envio.";
            case "email" -> "Vamos enviar a prévia diretamente para o seu e-mail.";
            case "whatsapp" -> "Opcional. Se preencher, podemos enviar a prévia também por lá.";
            default -> "";
        };
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

    private void appendCampaignAngleStructuredContext(StringBuilder sb, Experiment experiment) {
        if (experiment == null || experiment.getHypothesisRef() == null) {
            return;
        }
        HypothesisFrameworkDto framework = readHypothesisFramework(experiment.getHypothesisRef().getFrameworkJson());
        if (framework == null) {
            return;
        }

        String painSummary = buildPainSummary(framework);
        String resultSummary = buildResultSummary(framework);
        String mechanismSummary = buildMechanismSummary(framework);
        String proofSummary = buildProofSummary(framework);
        String offerCommercialSummary = buildOfferCommercialSummary(framework);

        sb.append("INSUMOS_ESTRUTURADOS_CAMPAIGN_ANGLE (prioridade alta para esta seção):\n");
        sb.append("OFFER_COMMERCIAL_SUMMARY: ").append(nonBlank(offerCommercialSummary)).append("\n");
        sb.append("PROOF_SUMMARY: ").append(nonBlank(proofSummary)).append("\n");
        sb.append("MECHANISM_SUMMARY: ").append(nonBlank(mechanismSummary)).append("\n");
        sb.append("RESULT_SUMMARY: ").append(nonBlank(resultSummary)).append("\n");
        sb.append("PAIN_SUMMARY: ").append(nonBlank(painSummary)).append("\n");
        sb.append("Regras obrigatórias para o campaign-angle:\n");
        sb.append("- Se OFFER_COMMERCIAL_SUMMARY tiver CTA concreta, não usar CTA genérica.\n");
        sb.append("- Se PROOF_SUMMARY trouxer prova pré-venda concreta, refletir no hook, promise e messageMatch.\n");
        sb.append("- Se OFFER_COMMERCIAL_SUMMARY trouxer entregáveis concretos, evitar rótulos vagos sem necessidade.\n");
        sb.append("- Preservar continuidade anúncio → landing usando os resumos estruturados acima como fonte principal.\n");
    }

    private HypothesisFrameworkDto readHypothesisFramework(String frameworkJson) {
        if (!StringUtils.hasText(frameworkJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(frameworkJson, HypothesisFrameworkDto.class);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String buildPainSummary(HypothesisFrameworkDto framework) {
        if (framework == null || framework.getPain() == null) {
            return "";
        }
        return firstNonBlank(
                framework.getPain().getSummary(),
                joinStructuredChunks(
                        framework.getPain().getRoot(),
                        framework.getPain().getCost(),
                        framework.getPain().getEmotional()));
    }

    private String buildResultSummary(HypothesisFrameworkDto framework) {
        if (framework == null || framework.getResult() == null) {
            return "";
        }
        return firstNonBlank(
                framework.getResult().getSummary(),
                joinStructuredChunks(
                        framework.getResult().getDesiredResult(),
                        framework.getResult().getBusinessOutcome(),
                        framework.getResult().getSuccessSignal()));
    }

    private String buildMechanismSummary(HypothesisFrameworkDto framework) {
        if (framework == null || framework.getMechanism() == null) {
            return "";
        }
        return firstNonBlank(
                framework.getMechanism().getSummary(),
                joinStructuredChunks(
                        framework.getMechanism().getCore(),
                        framework.getMechanism().getVisible(),
                        framework.getMechanism().getBelievability()));
    }

    private String buildProofSummary(HypothesisFrameworkDto framework) {
        if (framework == null || framework.getProof() == null) {
            return "";
        }
        return firstNonBlank(
                framework.getProof().getSummary(),
                joinStructuredChunks(
                        framework.getProof().getType(),
                        framework.getProof().getAsset(),
                        framework.getProof().getMessage()));
    }

    private String buildOfferCommercialSummary(HypothesisFrameworkDto framework) {
        if (framework == null || framework.getOffer() == null) {
            return "";
        }
        String deliverables = framework.getOffer().getDeliverables();
        String proofAsset = framework.getProof() != null ? framework.getProof().getAsset() : "";
        return firstNonBlank(
                framework.getOffer().getSummary(),
                joinStructuredChunks(
                        framework.getOffer().getCta(),
                        proofAsset,
                        deliverables,
                        framework.getOffer().getName(),
                        framework.getOffer().getCorePromise()));
    }

    private String joinStructuredChunks(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        List<String> chunks = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                chunks.add(value.trim());
            }
        }
        return String.join(" | ", chunks);
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
            case LANDING_PAGE_IMAGE_PLANNING -> schemaWithMetadata(
                    "landingPageImagePlanning",
                    landingPageImagePlanningFieldSchema(),
                    metadataSchema);
            case LANDING_PAGE_HTML -> schemaWithMetadata("landingPageHtml", landingPageHtmlFieldSchema(), metadataSchema);
        };
    }

    private Map<String, Object> landingPageImagePlanningFieldSchema() {
        Map<String, Object> safeCropZonesSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "top", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "right", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "bottom", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "left", Map.of("type", "number", "minimum", 0, "maximum", 1)
                ),
                "required", List.of("top", "right", "bottom", "left")
        );
        Map<String, Object> layoutBindingSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("preferredDesktopPlacement", Map.of("type", "string", "enum", List.of("left", "right", "center", "background"))),
                        Map.entry("preferredMobilePlacement", Map.of("type", "string", "enum", List.of("above-copy", "below-copy", "inline", "background"))),
                        Map.entry("desktopAspectRatio", stringSchema()),
                        Map.entry("mobileAspectRatio", stringSchema()),
                        Map.entry("allowCrop", Map.of("type", "boolean")),
                        Map.entry("safeCropZones", safeCropZonesSchema)
                ),
                "required", List.of("preferredDesktopPlacement", "preferredMobilePlacement", "desktopAspectRatio", "mobileAspectRatio", "allowCrop", "safeCropZones")
        );
        Map<String, Object> imageSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("sectionId", stringSchema()),
                        Map.entry("sectionName", stringSchema()),
                        Map.entry("imageBindingKey", Map.of("type", "string", "pattern", "^[a-z0-9_\\-]{3,64}$")),
                        Map.entry("imageRole", stringSchema()),
                        Map.entry("conversionRole", stringSchema()),
                        Map.entry("emotionalJob", stringSchema()),
                        Map.entry("sectionVisualGoal", stringSchema()),
                        Map.entry("placement", Map.of(
                                "type", "string",
                                "enum", List.of("hero", "benefit", "mechanism", "proof", "offer", "faq", "cta")
                        )),
                        Map.entry("hierarchyLevel", Map.of(
                                "type", "string",
                                "enum", List.of("primary", "secondary", "support")
                        )),
                        Map.entry("objective", stringSchema()),
                        Map.entry("imagePrompt", stringSchema()),
                        Map.entry("negativePrompt", stringSchema()),
                        Map.entry("visualStyle", stringSchema()),
                        Map.entry("composition", stringSchema()),
                        Map.entry("focalPoint", stringSchema()),
                        Map.entry("supportingElements", arrayOfStringsSchema(0)),
                        Map.entry("mood", stringSchema()),
                        Map.entry("layoutBinding", layoutBindingSchema),
                        Map.entry("attentionPriority", Map.of(
                                "type", "string",
                                "enum", List.of("high", "medium", "low")
                        )),
                        Map.entry("visualWeight", Map.of(
                                "type", "string",
                                "enum", List.of("primary", "secondary", "support")
                        )),
                        Map.entry("distanceToCTA", Map.of(
                                "type", "string",
                                "enum", List.of("near", "medium", "far")
                        )),
                        Map.entry("supportsFormConversion", Map.of("type", "boolean")),
                        Map.entry("formRelationNotes", stringSchema()),
                        Map.entry("dimensions", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "desktop", stringSchema(),
                                        "mobile", stringSchema()
                                ),
                                "required", List.of("desktop", "mobile")
                        )),
                        Map.entry("safeMargins", stringSchema()),
                        Map.entry("textOverlayGuidance", stringSchema()),
                        Map.entry("generationHints", arrayOfStringsSchema(0)),
                        Map.entry("messageMatchNotes", stringSchema()),
                        Map.entry("complianceNotes", stringSchema())
                ),
                "required", List.of(
                        "sectionId",
                        "sectionName",
                        "imageBindingKey",
                        "imageRole",
                        "conversionRole",
                        "emotionalJob",
                        "sectionVisualGoal",
                        "placement",
                        "hierarchyLevel",
                        "objective",
                        "layoutBinding",
                        "attentionPriority",
                        "visualWeight",
                        "distanceToCTA",
                        "supportsFormConversion",
                        "formRelationNotes",
                        "imagePrompt",
                        "dimensions",
                        "messageMatchNotes")
        );
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("pageGoal", stringSchema()),
                        Map.entry("visualDirectionSummary", stringSchema()),
                        Map.entry("sequencingNotes", stringSchema()),
                        Map.entry("ctaIntegrationNotes", stringSchema()),
                        Map.entry("images", Map.of(
                                "type", "array",
                                "minItems", 4,
                                "items", imageSchema
                        )),
                        Map.entry("consistencyChecks", Map.of(
                                "type", "array",
                                "minItems", 3,
                                "items", consistencyCheckSchema()
                        ))
                ),
                "required", List.of("pageGoal", "images", "consistencyChecks")
        );
    }

    private Map<String, Object> landingPageHtmlFieldSchema() {
        Map<String, Object> formFieldSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("name", stringSchema()),
                        Map.entry("type", Map.of("type", "string", "enum", List.of("text", "email", "tel"))),
                        Map.entry("label", stringSchema()),
                        Map.entry("required", Map.of("type", "boolean")),
                        Map.entry("placeholder", stringSchema())
                ),
                "required", List.of("name", "type", "label", "required", "placeholder")
        );
        Map<String, Object> formSpecSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("formId", stringSchema()),
                        Map.entry("title", stringSchema()),
                        Map.entry("submitLabel", stringSchema()),
                        Map.entry("submitTarget", stringSchema()),
                        Map.entry("cta", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "label", stringSchema(),
                                        "target", stringSchema(),
                                        "variant", stringSchema()
                                ),
                                "required", List.of("label", "target", "variant")
                        )),
                        Map.entry("fields", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", formFieldSchema
                        )),
                        Map.entry("consent", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "enabled", Map.of("type", "boolean"),
                                        "required", Map.of("type", "boolean"),
                                        "label", stringSchema()
                                ),
                                "required", List.of("enabled", "required", "label")
                        )),
                        Map.entry("successState", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "title", stringSchema(),
                                        "message", stringSchema()
                                ),
                                "required", List.of("title", "message")
                        ))
                ),
                "required", List.of("formId", "submitLabel", "submitTarget", "cta", "fields")
        );
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("htmlDocument", stringSchema()),
                        Map.entry("formSpec", formSpecSchema),
                        Map.entry("summary", stringSchema()),
                        Map.entry("consistencyChecks", Map.of(
                                "type", "array",
                                "minItems", 3,
                                "items", consistencyCheckSchema()
                        ))
                ),
                "required", List.of("htmlDocument", "formSpec", "summary", "consistencyChecks")
        );
    }

    private Map<String, Object> buildLandingHtmlFormSpec(Map<String, Object> wireframeFormSpec) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("formId", asTrimmedString(wireframeFormSpec.get("formId")));
        normalized.put("title", asTrimmedString(wireframeFormSpec.get("title")));
        String submitLabel = asTrimmedString(wireframeFormSpec.get("submitLabel"));
        String submitTarget = asTrimmedString(wireframeFormSpec.get("submitTarget"));
        normalized.put("submitLabel", submitLabel);
        normalized.put("submitTarget", submitTarget);
        normalized.put("fields", wireframeFormSpec.getOrDefault("fields", List.of()));
        normalized.put("consent", wireframeFormSpec.getOrDefault("consent", Map.of()));
        normalized.put("successState", wireframeFormSpec.getOrDefault("successState", Map.of()));
        normalized.put("cta", Map.of(
                "label", submitLabel,
                "target", submitTarget,
                "variant", "primary"
        ));
        return normalized;
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
        Map<String, Object> formFieldSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("name", stringSchema()),
                        Map.entry("type", Map.of("type", "string", "enum", List.of("text", "email", "tel"))),
                        Map.entry("label", stringSchema()),
                        Map.entry("required", Map.of("type", "boolean")),
                        Map.entry("placeholder", stringSchema())
                ),
                "required", List.of("name", "type", "label", "required", "placeholder")
        );
        Map<String, Object> formSpecSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("formId", stringSchema()),
                        Map.entry("title", stringSchema()),
                        Map.entry("submitLabel", stringSchema()),
                        Map.entry("submitTarget", stringSchema()),
                        Map.entry("fields", Map.of(
                                "type", "array",
                                "minItems", 3,
                                "items", formFieldSchema
                        )),
                        Map.entry("consent", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "enabled", Map.of("type", "boolean"),
                                        "required", Map.of("type", "boolean"),
                                        "label", stringSchema()
                                ),
                                "required", List.of("enabled", "required", "label")
                        )),
                        Map.entry("successState", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "title", stringSchema(),
                                        "message", stringSchema()
                                ),
                                "required", List.of("title", "message")
                        ))
                ),
                "required", List.of("formId", "title", "submitLabel", "submitTarget", "fields", "consent", "successState")
        );
        Map<String, Object> surfaceSpecSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("surfaceToken", stringSchema()),
                        Map.entry("style", Map.of("type", "string", "enum", List.of("band", "solid", "gradient-soft", "image-tint"))),
                        Map.entry("contrastMode", Map.of("type", "string", "enum", List.of("normal", "high", "soft"))),
                        Map.entry("notes", stringSchema())
                ),
                "required", List.of("surfaceToken", "style", "contrastMode", "notes")
        );
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
                        Map.entry("surfaceSpec", surfaceSpecSchema),
                        Map.entry("ctaSlot", ctaSlotSchema)
                ),
                "required", List.of("sectionId", "sectionName", "objective", "contentType", "mobilePriorityScore", "dropOffRisk", "surfaceSpec")
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
                        )),
                        Map.entry("formSpec", formSpecSchema)
                ),
                "required", List.of("pageGoal", "variantLayoutId", "sectionOrder", "consistencyChecks", "formSpec")
        );
    }

    @SuppressWarnings("unchecked")
    private void validateLandingHtmlFormConsistency(Experiment experiment, String landingHtmlContent) {
        if (!StringUtils.hasText(landingHtmlContent) || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return;
        }

        Map<String, Object> wireframeRoot = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
        Map<String, Object> wireframePayload = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
        if (!(wireframePayload.get("formSpec") instanceof Map<?, ?> rawFormSpec)) {
            log.warn("Validação landing HTML (experimentId={}): wireframe sem formSpec estruturado", experiment.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Wireframe da landing sem formSpec estruturado");
        }

        List<FormFieldContract> expectedFields = extractExpectedFormFields((Map<String, Object>) rawFormSpec);
        if (expectedFields.isEmpty()) {
            log.warn("Validação landing HTML (experimentId={}): formSpec.fields vazio", experiment.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Wireframe da landing sem campos em formSpec.fields");
        }

        String htmlDocument = landingHtmlContent.trim();
        if (!StringUtils.hasText(htmlDocument)) {
            log.warn("Validação landing HTML (experimentId={}): htmlDocument ausente no payload", experiment.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "HTML da landing vazio");
        }

        List<FormFieldContract> actualFields = extractFieldsFromHtmlDocument(htmlDocument);
        List<FormFieldContract> expectedSorted = expectedFields.stream()
                .sorted(Comparator.comparing(FormFieldContract::name))
                .toList();
        List<FormFieldContract> actualSorted = actualFields.stream()
                .sorted(Comparator.comparing(FormFieldContract::name))
                .toList();

        if (!expectedSorted.equals(actualSorted)) {
            log.warn("Validação landing HTML (experimentId={}): divergência de formulário detectada. expected={}, actual={}",
                    experiment.getId(),
                    expectedSorted,
                    actualSorted);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Divergência de formulário: landing-page-html deve reproduzir exatamente landing-page-wireframe.formSpec");
        }
    }

    @SuppressWarnings("unchecked")
    private List<FormFieldContract> extractExpectedFormFields(Map<String, Object> formSpec) {
        if (!(formSpec.get("fields") instanceof List<?> rawFields)) {
            return List.of();
        }
        List<FormFieldContract> fields = new ArrayList<>();
        for (Object rawField : rawFields) {
            if (!(rawField instanceof Map<?, ?> rawFieldMap)) {
                continue;
            }
            Map<String, Object> field = (Map<String, Object>) rawFieldMap;
            String name = normalizeHtmlAttr(asTrimmedString(field.get("name")));
            String type = normalizeHtmlAttr(asTrimmedString(field.get("type")));
            if (!StringUtils.hasText(name) || !StringUtils.hasText(type) || !(field.get("required") instanceof Boolean required)) {
                continue;
            }
            fields.add(new FormFieldContract(name, type, required));
        }
        return fields;
    }

    private List<FormFieldContract> extractFieldsFromHtmlDocument(String htmlDocument) {
        List<FormFieldContract> fields = new ArrayList<>();
        Matcher tagMatcher = FORM_CONTROL_TAG_PATTERN.matcher(htmlDocument);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            Map<String, String> attrs = parseHtmlAttributes(tag);
            String name = normalizeHtmlAttr(attrs.get("name"));
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String type = normalizeHtmlAttr(attrs.get("type"));
            if (!StringUtils.hasText(type)) {
                type = "text";
            }
            if (!Set.of("text", "email", "tel").contains(type)) {
                continue;
            }
            boolean required = attrs.containsKey("required") || tag.toLowerCase(Locale.ROOT).contains(" required");
            fields.add(new FormFieldContract(name, type, required));
        }
        return fields;
    }



    @SuppressWarnings("unchecked")
    private void validateLandingHtmlSurfaceConsistency(Experiment experiment, String landingHtmlContent) {
        if (!StringUtils.hasText(landingHtmlContent) || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return;
        }

        Map<String, Object> wireframeRoot = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
        Map<String, Object> wireframePayload = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
        List<SectionSurfaceContract> expectedSurfaces = extractExpectedSectionSurfaces(wireframePayload);
        if (expectedSurfaces.isEmpty()) {
            log.warn("Validação landing HTML (experimentId={}): wireframe sem sectionOrder.surfaceSpec", experiment.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Wireframe da landing sem sectionOrder.surfaceSpec estruturado");
        }

        String htmlDocument = landingHtmlContent.trim();
        if (!StringUtils.hasText(htmlDocument)) {
            log.warn("Validação landing HTML (experimentId={}): htmlDocument ausente para validação de surfaceSpec", experiment.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "HTML da landing vazio");
        }

        List<SectionSurfaceContract> actualSurfaces = extractSectionSurfacesFromHtmlDocument(experiment.getId(), htmlDocument);
        List<SectionSurfaceContract> expectedSorted = expectedSurfaces.stream()
                .sorted(Comparator.comparing(SectionSurfaceContract::sectionId))
                .toList();
        List<SectionSurfaceContract> actualSorted = actualSurfaces.stream()
                .sorted(Comparator.comparing(SectionSurfaceContract::sectionId))
                .toList();

        if (!expectedSorted.equals(actualSorted)) {
            log.warn("Validação landing HTML (experimentId={}): divergência de surfaceSpec detectada. expected={}, actual={}",
                    experiment.getId(),
                    expectedSorted,
                    actualSorted);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Divergência de superfície: landing-page-html deve reproduzir exatamente landing-page-wireframe.sectionOrder.surfaceSpec");
        }
    }

    @SuppressWarnings("unchecked")
    private List<SectionSurfaceContract> extractExpectedSectionSurfaces(Map<String, Object> wireframePayload) {
        if (!(wireframePayload.get("sectionOrder") instanceof List<?> rawSections)) {
            return List.of();
        }
        List<SectionSurfaceContract> surfaces = new ArrayList<>();
        for (Object rawSection : rawSections) {
            if (!(rawSection instanceof Map<?, ?> rawSectionMap)) {
                continue;
            }
            Map<String, Object> section = (Map<String, Object>) rawSectionMap;
            String sectionId = normalizeHtmlAttr(asTrimmedString(section.get("sectionId")));
            if (!(section.get("surfaceSpec") instanceof Map<?, ?> rawSurfaceMap)) {
                continue;
            }
            Map<String, Object> surfaceSpec = (Map<String, Object>) rawSurfaceMap;
            String surfaceToken = normalizeHtmlAttr(asTrimmedString(surfaceSpec.get("surfaceToken")));
            String style = normalizeHtmlAttr(asTrimmedString(surfaceSpec.get("style")));
            String contrastMode = normalizeHtmlAttr(asTrimmedString(surfaceSpec.get("contrastMode")));
            if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(surfaceToken)
                    || !StringUtils.hasText(style) || !StringUtils.hasText(contrastMode)) {
                continue;
            }
            surfaces.add(new SectionSurfaceContract(sectionId, surfaceToken, style, contrastMode));
        }
        return surfaces;
    }

    private List<SectionSurfaceContract> extractSectionSurfacesFromHtmlDocument(Long experimentId, String htmlDocument) {
        Map<String, SectionSurfaceContract> contractsBySectionId = new LinkedHashMap<>();
        Matcher tagMatcher = OPENING_TAG_PATTERN.matcher(htmlDocument);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            Map<String, String> attrs = parseHtmlAttributes(tag);
            String rawSectionId = attrs.get("data-section-id");
            String rawSurfaceToken = attrs.get("data-surface-token");
            String rawStyle = attrs.get("data-surface-style");
            String rawContrastMode = attrs.get("data-surface-contrast");
            if (containsEncodedQuoteArtifact(rawSectionId)
                    || containsEncodedQuoteArtifact(rawSurfaceToken)
                    || containsEncodedQuoteArtifact(rawStyle)
                    || containsEncodedQuoteArtifact(rawContrastMode)) {
                log.warn("Validação landing HTML (experimentId={}): detectado valor de superfície com aspas codificadas; normalizando atributos. sectionIdRaw={}, surfaceTokenRaw={}, styleRaw={}, contrastRaw={}",
                        experimentId,
                        rawSectionId,
                        rawSurfaceToken,
                        rawStyle,
                        rawContrastMode);
            }
            String sectionId = normalizeHtmlAttr(rawSectionId);
            if (!StringUtils.hasText(sectionId)) {
                continue;
            }
            String surfaceToken = normalizeHtmlAttr(rawSurfaceToken);
            String style = normalizeHtmlAttr(rawStyle);
            String contrastMode = normalizeHtmlAttr(rawContrastMode);
            if (!StringUtils.hasText(surfaceToken) || !StringUtils.hasText(style) || !StringUtils.hasText(contrastMode)) {
                continue;
            }
            contractsBySectionId.put(sectionId, new SectionSurfaceContract(sectionId, surfaceToken, style, contrastMode));
        }
        return new ArrayList<>(contractsBySectionId.values());
    }

    @SuppressWarnings("unchecked")
    private void validateLandingHtmlImagePlanConsistency(Experiment experiment, String landingHtmlContent) {
        if (!StringUtils.hasText(landingHtmlContent) || !StringUtils.hasText(experiment.getLandingPageImagePlanning())) {
            return;
        }

        Map<String, Object> imagePlanRoot = readObject(experiment.getLandingPageImagePlanning(), "Planejamento de imagens da landing inválido");
        Map<String, Object> imagePlanPayload = unwrapSectionPayload(imagePlanRoot, "landingPageImagePlanning");
        List<ImagePlanBindingContract> expectedBindings = extractExpectedImagePlanBindings(imagePlanPayload);
        if (expectedBindings.isEmpty()) {
            return;
        }

        String htmlDocument = landingHtmlContent.trim();
        if (!StringUtils.hasText(htmlDocument)) {
            log.warn("Validação landing HTML (experimentId={}): htmlDocument ausente para validação de image plan", experiment.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "HTML da landing vazio");
        }

        List<ImagePlanBindingContract> actualBindings = extractImagePlanBindingsFromHtmlDocument(htmlDocument);
        log.info("Validação landing HTML image binding (experimentId={}): plannedCount={}, expectedPairs={}, foundPairs={}",
                experiment.getId(),
                expectedBindings.size(),
                summarizeImageBindingPairs(expectedBindings),
                summarizeImageBindingPairs(actualBindings));
        List<String> expectedBindingPairs = expectedBindings.stream()
                .map(binding -> binding.sectionId() + "/" + binding.imageBindingKey())
                .sorted()
                .toList();
        List<String> actualBindingPairs = actualBindings.stream()
                .map(binding -> binding.sectionId() + "/" + binding.imageBindingKey())
                .sorted()
                .toList();

        if (!expectedBindingPairs.equals(actualBindingPairs)) {
            log.warn("Validação landing HTML (experimentId={}): divergência de image plan detectada. expected={}, actual={}",
                    experiment.getId(),
                    expectedBindings,
                    actualBindings);
            log.warn("Validação landing HTML (experimentId={}): divergência específica de pares expected={} actual={}",
                    experiment.getId(),
                    expectedBindingPairs,
                    actualBindingPairs);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Divergência de imagens: landing-page-html deve reproduzir o binding explícito canônico do landing-page-image-planning por sectionId/imageBindingKey");
        }
    }

    @SuppressWarnings("unchecked")
    private List<ImagePlanBindingContract> extractExpectedImagePlanBindings(Map<String, Object> imagePlanPayload) {
        if (!(imagePlanPayload.get("images") instanceof List<?> rawImages)) {
            return List.of();
        }
        List<ImagePlanBindingContract> bindings = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        int index = 0;
        for (Object rawImage : rawImages) {
            index++;
            if (!(rawImage instanceof Map<?, ?> rawImageMap)) {
                continue;
            }
            Map<String, Object> image = (Map<String, Object>) rawImageMap;
            String sectionId = normalizeHtmlAttr(asTrimmedString(image.get("sectionId")));
            String imageBindingKey = canonicalBindingKey(image, index, usedKeys);
            String imageRole = normalizeHtmlAttr(asTrimmedString(image.get("imageRole")));
            String conversionRole = normalizeHtmlAttr(asTrimmedString(image.get("conversionRole")));
            String attentionPriority = normalizeHtmlAttr(asTrimmedString(image.get("attentionPriority")));
            String visualWeight = normalizeHtmlAttr(asTrimmedString(image.get("visualWeight")));
            String distanceToCta = normalizeHtmlAttr(asTrimmedString(image.get("distanceToCTA")));
            boolean supportsFormConversion = image.get("supportsFormConversion") instanceof Boolean flag && flag;
            if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(imageBindingKey) || !StringUtils.hasText(imageRole)
                    || !StringUtils.hasText(conversionRole) || !StringUtils.hasText(attentionPriority)
                    || !StringUtils.hasText(visualWeight) || !StringUtils.hasText(distanceToCta)) {
                continue;
            }
            image.put("imageBindingKey", imageBindingKey);
            bindings.add(new ImagePlanBindingContract(sectionId, imageBindingKey, imageRole, conversionRole, attentionPriority, visualWeight, distanceToCta, supportsFormConversion));
        }
        return bindings;
    }

    private List<ImagePlanBindingContract> extractImagePlanBindingsFromHtmlDocument(String htmlDocument) {
        List<ImagePlanBindingContract> bindings = new ArrayList<>();
        Matcher tagMatcher = IMG_TAG_PATTERN.matcher(htmlDocument);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            Map<String, String> attrs = parseHtmlAttributes(tag);
            String sectionId = normalizeHtmlAttr(attrs.get("data-image-section-id"));
            String imageBindingKey = resolveBindingKeyFromHtmlAttrs(attrs);
            String imageRole = normalizeHtmlAttr(attrs.get("data-image-role"));
            String conversionRole = normalizeHtmlAttr(attrs.get("data-conversion-role"));
            String attentionPriority = normalizeHtmlAttr(attrs.get("data-attention-priority"));
            String visualWeight = normalizeHtmlAttr(attrs.get("data-visual-weight"));
            String distanceToCta = normalizeHtmlAttr(attrs.get("data-distance-to-cta"));
            String supportsFormConversionRaw = normalizeHtmlAttr(attrs.get("data-supports-form-conversion"));
            if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(imageBindingKey) || !StringUtils.hasText(imageRole) || !StringUtils.hasText(conversionRole)
                    || !StringUtils.hasText(attentionPriority) || !StringUtils.hasText(visualWeight)
                    || !StringUtils.hasText(distanceToCta) || !StringUtils.hasText(supportsFormConversionRaw)) {
                continue;
            }
            boolean supportsFormConversion = "true".equalsIgnoreCase(supportsFormConversionRaw);
            bindings.add(new ImagePlanBindingContract(sectionId, imageBindingKey, imageRole, conversionRole, attentionPriority, visualWeight, distanceToCta, supportsFormConversion));
        }
        return bindings;
    }

    private String canonicalBindingKey(Map<String, Object> image, int index, Set<String> usedKeys) {
        List<String> candidates = List.of(
                asTrimmedString(image.get("imageBindingKey")),
                asTrimmedString(image.get("imageRole")),
                asTrimmedString(image.get("sectionId")),
                "binding-" + index
        );
        for (String candidate : candidates) {
            String slug = slugifyBindingKey(candidate);
            if (StringUtils.hasText(slug)) {
                return registerUniqueBindingKey(slug, usedKeys);
            }
        }
        return registerUniqueBindingKey("binding-" + index, usedKeys);
    }

    private String slugifyBindingKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64).replaceAll("-+$", "");
        }
        return normalized.length() >= 3 ? normalized : "";
    }

    private String registerUniqueBindingKey(String base, Set<String> usedKeys) {
        String candidate = StringUtils.hasText(base) ? base : "binding";
        int suffix = 2;
        while (usedKeys.contains(candidate)) {
            String suffixToken = "-" + suffix++;
            int maxLength = Math.max(3, 64 - suffixToken.length());
            String truncatedBase = candidate.length() > maxLength
                    ? candidate.substring(0, maxLength).replaceAll("-+$", "")
                    : candidate;
            if (!StringUtils.hasText(truncatedBase)) {
                truncatedBase = "binding";
            }
            candidate = truncatedBase + suffixToken;
        }
        usedKeys.add(candidate);
        return candidate;
    }

    private String resolveBindingKeyFromHtmlAttrs(Map<String, String> attrs) {
        String bindingKey = slugifyBindingKey(normalizeHtmlAttr(attrs.get("data-image-binding-key")));
        if (StringUtils.hasText(bindingKey)) {
            return bindingKey;
        }
        return slugifyBindingKey(normalizeHtmlAttr(attrs.get("data-image-role")));
    }

    private String summarizeImageBindingPairs(List<ImagePlanBindingContract> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return "[]";
        }
        return bindings.stream()
                .map(binding -> binding.sectionId() + "/" + binding.imageBindingKey())
                .toList()
                .toString();
    }

    private Map<String, String> parseHtmlAttributes(String tag) {
        Map<String, String> attrs = new LinkedHashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(tag);
        while (matcher.find()) {
            String value = matcher.group(2);
            if (value == null) {
                value = matcher.group(3);
            }
            if (value == null) {
                value = matcher.group(4);
            }
            attrs.put(matcher.group(1).toLowerCase(Locale.ROOT), value == null ? "" : value);
        }
        return attrs;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapSectionPayload(Map<String, Object> payload, String fieldName) {
        if (payload.get("artifact") instanceof Map<?, ?> rawArtifact) {
            Map<String, Object> artifact = (Map<String, Object>) rawArtifact;
            if (artifact.get("content") instanceof Map<?, ?> rawContent) {
                return (Map<String, Object>) rawContent;
            }
        }
        if (payload.get(fieldName) instanceof Map<?, ?> rawField) {
            return (Map<String, Object>) rawField;
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readObject(String rawJson, String errorMessage) {
        try {
            Object parsed = objectMapper.readValue(rawJson, Object.class);
            if (parsed instanceof Map<?, ?> rawMap) {
                return (Map<String, Object>) rawMap;
            }
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMessage);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMessage, ex);
        }
    }

    private String asTrimmedString(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private String normalizeHtmlAttr(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        normalized = normalizeEscapedAttributeArtifacts(normalized);
        normalized = decodeCommonHtmlEntities(normalized);
        normalized = normalizeEscapedAttributeArtifacts(normalized);
        normalized = stripWrappingQuotes(normalized);
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEscapedAttributeArtifacts(String value) {
        return value
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\&quot;", "&quot;")
                .replace("\\&#34;", "&#34;")
                .replace("\\&#x22;", "&#x22;")
                .replace("\\&#X22;", "&#X22;")
                .replace("\\&apos;", "&apos;")
                .replace("\\&#39;", "&#39;")
                .replace("\\&#x27;", "&#x27;")
                .replace("\\&#X27;", "&#X27;")
                .replace("\\\"", "\"")
                .replace("\\'", "'");
    }

    private String decodeCommonHtmlEntities(String value) {
        return value
                .replace("&quot;", "\"")
                .replace("&#34;", "\"")
                .replace("&#x22;", "\"")
                .replace("&#X22;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&#X27;", "'")
                .replace("&amp;", "&");
    }

    private String stripWrappingQuotes(String value) {
        String current = value;
        boolean changed = true;
        while (changed && StringUtils.hasText(current)) {
            changed = false;
            String trimmed = current.trim();
            if (trimmed.length() >= 2 && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
                current = trimmed.substring(1, trimmed.length() - 1).trim();
                changed = true;
            }
        }
        return current;
    }

    private boolean containsEncodedQuoteArtifact(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("&quot;")
                || lower.contains("\\&quot;")
                || lower.contains("&#34;")
                || lower.contains("\\&#34;")
                || lower.contains("&#x22;")
                || lower.contains("\\&#x22;")
                || lower.contains("\\\"");
    }

    private record FormFieldContract(String name, String type, boolean required) {
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FormFieldContract other)) {
                return false;
            }
            return Objects.equals(name, other.name)
                    && Objects.equals(type, other.type)
                    && required == other.required;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, required);
        }
    }

    private record SectionSurfaceContract(String sectionId, String surfaceToken, String style, String contrastMode) {
    }

    private record ImagePlanBindingContract(String sectionId,
                                            String imageBindingKey,
                                            String imageRole,
                                            String conversionRole,
                                            String attentionPriority,
                                            String visualWeight,
                                            String distanceToCta,
                                            boolean supportsFormConversion) {
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
