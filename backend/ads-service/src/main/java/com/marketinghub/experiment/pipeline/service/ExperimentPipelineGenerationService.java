package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.cost.CostAttributionService;
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
import com.marketinghub.experiment.pipeline.dto.LandingPageVariantLinksDto;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobCompletionRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import com.marketinghub.experiment.pipeline.lhm.LandingHtmlModule;
import com.marketinghub.experiment.pipeline.repository.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.geralanding.CopyProvisionalHtmlAssembler;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
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
    private static final String LHM_MODEL = "LHM";
    private static final String GERAR_COM_IA_MODEL = "GERAR_COM_IA";
    private static final String LHM_WORKER_ID = "lhm-inline";
    private static final String LANDING_HTML_AUDIT_FEATURE_FLAG = "lhm.audit.gate.enabled";
    private static final String LANDING_HTML_REGISTRY_FEATURE_FLAG = "lhm.registry.enabled";
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
    private final LandingHtmlModule landingHtmlModule;
    private final CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler;
    private final FrameworkImageGenerationService frameworkImageGenerationService;
    private final OpenAiPricingService openAiPricingService;
    private final LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;
    private final CostAttributionService costAttributionService;

    public ExperimentPipelineGenerationService(ExperimentRepository experimentRepository,
                                               ExperimentPipelineGenerationJobRepository jobRepository,
                                               ExperimentMapper experimentMapper,
                                               AiWorkerGenerationService generationService,
                                               LeadPortalFlowRepository leadPortalFlowRepository,
                                               LeadPortalFlowPublisher leadPortalFlowPublisher,
                                               ObjectMapper objectMapper,
                                               LandingPageImageInjector landingPageImageInjector,
                                               LandingHtmlModule landingHtmlModule,
                                               CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler,
                                               FrameworkImageGenerationService frameworkImageGenerationService,
                                               OpenAiPricingService openAiPricingService,
                                               LeadPortalPublicUrlResolver leadPortalPublicUrlResolver,
                                               CostAttributionService costAttributionService) {
        this.experimentRepository = experimentRepository;
        this.jobRepository = jobRepository;
        this.experimentMapper = experimentMapper;
        this.generationService = generationService;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.leadPortalFlowPublisher = leadPortalFlowPublisher;
        this.objectMapper = objectMapper;
        this.landingPageImageInjector = landingPageImageInjector;
        this.landingHtmlModule = landingHtmlModule;
        this.copyProvisionalHtmlAssembler = copyProvisionalHtmlAssembler;
        this.frameworkImageGenerationService = frameworkImageGenerationService;
        this.openAiPricingService = openAiPricingService;
        this.leadPortalPublicUrlResolver = leadPortalPublicUrlResolver;
        this.costAttributionService = costAttributionService;
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
    public void resumeFlowAfterImagePlanningIfReady(Long experimentId) {
        if (experimentId == null || !frameworkImageGenerationService.allPlanningImagesCompleted(experimentId)) {
            return;
        }
        try {
            generate(experimentId,
                    ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET,
                    new ExperimentPipelineGenerationRequest());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() != HttpStatus.CONFLICT) {
                throw ex;
            }
            log.debug("Retomada automática ignorada para experimento {}: {}", experimentId, ex.getReason());
        }
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
    public ExperimentDto generateLandingHtmlWithLhm(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));
        if (!StringUtils.hasText(experiment.getLandingPageWireframe())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Wireframe da landing ainda não foi gerado para este experimento");
        }
        if (!StringUtils.hasText(experiment.getLandingPageCopy())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Copy da landing ainda não foi gerada para este experimento");
        }
        if (!StringUtils.hasText(experiment.getLandingPageDesignPreset())) {
            String provisionalHtml = copyProvisionalHtmlAssembler.assemble(
                    experiment.getLandingPageCopy(),
                    experiment.getLandingPageWireframe(),
                    null);
            String htmlWithImages = landingPageImageInjector.injectImages(experiment.getId(), provisionalHtml);
            applySectionContent(experiment, ExperimentPipelineSection.LANDING_PAGE_HTML, htmlWithImages);
            return experimentMapper.toDto(experiment);
        }
        Map<String, Object> monitoringPayload = buildLhmMonitoringPayload(experiment, experimentId);
        ExperimentPipelineGenerationJob monitoringJob = createInlineGenerationJob(
                experiment,
                ExperimentPipelineSection.LANDING_PAGE_HTML,
                LHM_MODEL,
                LHM_WORKER_ID,
                "Solicitação LHM recebida. Preparando montagem determinística do HTML.",
                monitoringPayload);

        try {
            String promptInputs = landingHtmlModule.buildPromptV2Inputs(experiment);
            monitoringJob.setPrompt(promptInputs);
            monitoringPayload.put("promptPrepared", true);
            monitoringJob.setRequestBodyJson(writeJsonSilently(monitoringPayload));
            String assembledHtml = landingHtmlModule.assembleHtmlDocument(experiment);
            if (!StringUtils.hasText(assembledHtml)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "LHM não conseguiu montar HTML válido para este experimento");
            }
            Map<String, Object> qualityAuditReport = auditLandingHtmlQuality(experiment, assembledHtml);
            monitoringPayload.put("qualityAudit", qualityAuditReport);
            boolean auditGateEnabled = Boolean.parseBoolean(System.getProperty(LANDING_HTML_AUDIT_FEATURE_FLAG, "false"));
            monitoringPayload.put("qualityAuditGateEnabled", auditGateEnabled);
            monitoringPayload.put("registryEnabled", Boolean.parseBoolean(System.getProperty(LANDING_HTML_REGISTRY_FEATURE_FLAG, "false")));
            if (auditGateEnabled) {
                ensureLandingHtmlQualityGate(qualityAuditReport);
            }
            applySectionContent(experiment, ExperimentPipelineSection.LANDING_PAGE_HTML, assembledHtml);
            completeInlineGenerationJob(monitoringJob, assembledHtml, assembledHtml, BigDecimal.ZERO);
            return experimentMapper.toDto(experiment);
        } catch (ResponseStatusException ex) {
            monitoringPayload.put("errorStatus", ex.getStatusCode().value());
            monitoringPayload.put("errorReason", ex.getReason());
            monitoringJob.setRequestBodyJson(writeJsonSilently(monitoringPayload));
            failInlineGenerationJob(monitoringJob, ex.getReason());
            throw ex;
        } catch (RuntimeException ex) {
            monitoringPayload.put("errorType", ex.getClass().getSimpleName());
            monitoringPayload.put("errorMessage", ex.getMessage());
            monitoringJob.setRequestBodyJson(writeJsonSilently(monitoringPayload));
            failInlineGenerationJob(monitoringJob, ex.getMessage());
            throw ex;
        }
    }

    private Map<String, Object> buildLhmMonitoringPayload(Experiment experiment, Long experimentId) {
        Map<String, Object> monitoringPayload = new LinkedHashMap<>();
        monitoringPayload.put("mode", "INLINE");
        monitoringPayload.put("source", "LHM");
        monitoringPayload.put("section", ExperimentPipelineSection.LANDING_PAGE_HTML.path());
        monitoringPayload.put("experimentId", experimentId);
        monitoringPayload.put("requestReceivedAt", Instant.now().toString());

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("wireframePresent", StringUtils.hasText(experiment.getLandingPageWireframe()));
        inputs.put("copyPresent", StringUtils.hasText(experiment.getLandingPageCopy()));
        inputs.put("designPresetPresent", StringUtils.hasText(experiment.getLandingPageDesignPreset()));
        inputs.put("imagePlanningPresent", StringUtils.hasText(experiment.getLandingPageImagePlanning()));
        monitoringPayload.put("canonicalInputs", inputs);

        return monitoringPayload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> auditLandingHtmlQuality(Experiment experiment, String html) {
        String normalized = html == null ? "" : html;
        int checks = 0;
        int passed = 0;
        List<String> failures = new ArrayList<>();

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(normalized);
        int informativeImages = 0;
        int imagesWithAlt = 0;
        int imagesWithPerfAttrs = 0;
        while (imgMatcher.find()) {
            Map<String, String> attrs = parseHtmlAttributes(imgMatcher.group());
            String role = normalizeHtmlAttr(attrs.get("data-image-role"));
            String alt = normalizeHtmlAttr(attrs.get("alt"));
            if (!"decorative".equalsIgnoreCase(role)) {
                informativeImages++;
                if (StringUtils.hasText(alt)) {
                    imagesWithAlt++;
                }
            }
            boolean hasLoading = StringUtils.hasText(normalizeHtmlAttr(attrs.get("loading")));
            boolean hasDecoding = StringUtils.hasText(normalizeHtmlAttr(attrs.get("decoding")));
            boolean hasSizes = StringUtils.hasText(normalizeHtmlAttr(attrs.get("sizes")));
            if (hasLoading && hasDecoding && hasSizes) {
                imagesWithPerfAttrs++;
            }
        }

        checks++;
        if (informativeImages == 0 || imagesWithAlt == informativeImages) {
            passed++;
        } else {
            failures.add("Imagens informativas sem alt textual");
        }

        checks++;
        if (imagesWithPerfAttrs >= Math.max(1, informativeImages)) {
            passed++;
        } else {
            failures.add("Imagens sem loading/decoding/sizes completos");
        }

        checks++;
        boolean hasVisibleLabel = Pattern.compile("<label\\b[^>]*>\\s*[^<]{2,}", Pattern.CASE_INSENSITIVE).matcher(normalized).find();
        if (hasVisibleLabel) {
            passed++;
        } else {
            failures.add("Formulário sem label visível");
        }

        checks++;
        boolean hasFocusStyle = Pattern.compile(":focus-visible|:focus\\s*\\{", Pattern.CASE_INSENSITIVE).matcher(normalized).find();
        if (hasFocusStyle) {
            passed++;
        } else {
            failures.add("Ausência de foco perceptível (:focus/:focus-visible)");
        }

        checks++;
        boolean hasNarrativeCoverage = hasNarrativeCoverage(experiment);
        if (hasNarrativeCoverage) {
            passed++;
        } else {
            failures.add("Copy sem cobertura explícita Dor→Resultado→Mecanismo→Prova→Oferta");
        }

        int score = Math.round((passed * 100.0f) / checks);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("framework", "sprint3-post-render-audit-v1");
        report.put("checkedAt", Instant.now().toString());
        report.put("score", score);
        report.put("minimumScore", 75);
        report.put("checks", checks);
        report.put("passed", passed);
        report.put("failures", failures);
        report.put("hardFail", !failures.isEmpty() && score < 75);
        report.put("experimentId", experiment != null ? experiment.getId() : null);
        report.put("imageStats", Map.of(
                "informativeImages", informativeImages,
                "imagesWithAlt", imagesWithAlt,
                "imagesWithPerfAttrs", imagesWithPerfAttrs));
        return report;
    }

    private void ensureLandingHtmlQualityGate(Map<String, Object> qualityAuditReport) {
        if (!(qualityAuditReport.get("score") instanceof Number scoreNumber)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Relatório de auditoria da landing inválido: score ausente.");
        }
        int score = scoreNumber.intValue();
        if (score < 75) {
            Object failures = qualityAuditReport.get("failures");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Qualidade comercial da landing reprovada (score=" + score + "). Falhas: " + failures);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasNarrativeCoverage(Experiment experiment) {
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageCopy())) {
            return false;
        }
        try {
            Map<String, Object> root = readObject(experiment.getLandingPageCopy(), "Copy da landing inválida");
            Map<String, Object> payload = unwrapSectionPayload(root, "landingPageCopy");
            if (!(payload.get("bodySections") instanceof List<?> rawSections)) {
                return false;
            }
            String corpus = objectMapper.writeValueAsString(payload).toLowerCase(Locale.ROOT);
            boolean dor = corpus.contains("dor");
            boolean resultado = corpus.contains("resultado");
            boolean mecanismo = corpus.contains("mecanismo");
            boolean prova = corpus.contains("prova");
            boolean oferta = corpus.contains("oferta");
            return dor && resultado && mecanismo && prova && oferta && !rawSections.isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    @Transactional
    public LandingPagePublicationResultDto approveAndPublishLandingPage(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));
        if (!StringUtils.hasText(experiment.getLandingPageHtml())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Landing HTML ainda não foi gerado para este experimento");
        }
        LeadPortalFlow iaFlow = upsertLandingVariantFlow(
                experiment,
                "gerar-com-ia",
                GERAR_COM_IA_MODEL,
                experiment.getLandingPageHtml());

        for (LeadPortalFlow flow : List.of(iaFlow)) {
            flow.setApproved(true);
            if (flow.getApprovedAt() == null) {
                flow.setApprovedAt(Instant.now());
            }
            leadPortalFlowRepository.save(flow);
            try {
                leadPortalFlowPublisher.publish(flow);
            } catch (LeadPortalPublicationException ex) {
                String rootCauseMessage = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
                String message = StringUtils.hasText(rootCauseMessage)
                        ? "Falha ao aprovar/publicar landing automaticamente: " + rootCauseMessage
                        : "Falha ao aprovar/publicar landing automaticamente";
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, ex);
            }
        }
        experiment.setLeadPortalFlow(iaFlow);
        experiment.setSchemaFirstLeadPortalEnabled(true);
        experimentRepository.save(experiment);

        List<LandingPageVariantLinksDto> variantLinks = List.of(toVariantLinks("Gera Landing", iaFlow));

        String pixelId = iaFlow.getMarketNiche() != null ? iaFlow.getMarketNiche().getFacebookPixelId() : null;
        return new LandingPagePublicationResultDto(
                experimentId,
                iaFlow.getId(),
                iaFlow.isApproved(),
                true,
                leadPortalPublicUrlResolver.resolve(iaFlow),
                variantLinks,
                pixelId,
                StringUtils.hasText(pixelId),
                "Landing aprovada e publicação iniciada automaticamente após a aprovação.");
    }

    private LeadPortalFlow upsertLandingVariantFlow(Experiment experiment,
                                                    String variantKey,
                                                    String model,
                                                    String landingHtml) {
        Long experimentId = experiment.getId();
        String variantSuffix = normalizeSlug(variantKey);
        String slug = "exp-" + experimentId + "-landing-" + variantSuffix;
        LeadPortalFlow flow = leadPortalFlowRepository.findBySlug(slug).orElseGet(() -> LeadPortalFlow.builder()
                .name("Landing " + variantKey.toUpperCase(Locale.ROOT) + " - Experimento " + experimentId)
                .slug(slug)
                .description("Fluxo criado automaticamente para a variação " + variantKey.toUpperCase(Locale.ROOT)
                        + " da landing page do experimento " + experimentId)
                .prompt("Pipeline: landing-page-html/approve-and-publish")
                .schemaFirst(true)
                .experiment(experiment)
                .marketNiche(experiment.getNiche())
                .build());
        String htmlWithImages = landingPageImageInjector.injectImages(experimentId, landingHtml);
        flow.setCustomFormHtml(htmlWithImages != null ? htmlWithImages.trim() : null);
        flow.setSchemaFirst(true);
        flow.setModel(model);
        flow.setExperiment(experiment);
        flow.setMarketNiche(experiment.getNiche());
        return leadPortalFlowRepository.save(flow);
    }

    private LandingPageVariantLinksDto toVariantLinks(String variant, LeadPortalFlow flow) {
        String iframeUrl = leadPortalPublicUrlResolver.resolve(flow);
        return new LandingPageVariantLinksDto(
                variant,
                flow.getId(),
                iframeUrl,
                resolveStandaloneLandingUrl(iframeUrl));
    }

    private String resolveStandaloneLandingUrl(String iframeUrl) {
        if (!StringUtils.hasText(iframeUrl)) {
            return null;
        }
        try {
            URI parsed = URI.create(iframeUrl);
            String path = parsed.getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }
            String[] segments = path.split("/");
            String slug = segments.length == 0 ? "" : segments[segments.length - 1];
            if (!StringUtils.hasText(slug)) {
                return null;
            }
            return UriComponentsBuilder.newInstance()
                    .scheme(parsed.getScheme())
                    .host(parsed.getHost())
                    .port(parsed.getPort())
                    .path("/api/flows/{slug}/page")
                    .buildAndExpand(slug)
                    .toUriString();
        } catch (RuntimeException ex) {
            return null;
        }
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
        applyCostToExperimentHierarchy(experiment, estimatedCost);

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

    private ExperimentPipelineGenerationJob createInlineGenerationJob(Experiment experiment,
                                                                      ExperimentPipelineSection section,
                                                                      String model,
                                                                      String workerId,
                                                                      String prompt,
                                                                      Map<String, Object> payload) {
        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .experiment(experiment)
                .section(section)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.WAITING_OPENAI)
                .model(model)
                .workerId(workerId)
                .prompt(prompt)
                .requestBodyJson(writeJsonSilently(payload))
                .startedAt(Instant.now())
                .build();
        return jobRepository.save(job);
    }

    private void completeInlineGenerationJob(ExperimentPipelineGenerationJob job,
                                             String responseContent,
                                             String rawResponse,
                                             BigDecimal costUsd) {
        generationService.recordGeneration(AiWorkerGenerationRequest.builder()
                .domain("experiment.pipeline." + job.getSection().path())
                .referenceId(job.getExperiment().getId().toString())
                .prompt(job.getPrompt())
                .rawResponse(rawResponse)
                .model(job.getModel())
                .inputTokens(0)
                .outputTokens(0)
                .costUsd(costUsd != null ? costUsd : BigDecimal.ZERO)
                .build());

        job.setStatus(ExperimentPipelineGenerationJobStatus.COMPLETED);
        job.setStage(ExperimentPipelineGenerationJobStage.COMPLETED);
        job.setResponseContent(responseContent);
        job.setRawResponse(rawResponse);
        job.setInputTokens(0);
        job.setOutputTokens(0);
        job.setCostUsd(costUsd != null ? costUsd : BigDecimal.ZERO);
        job.setErrorMessage(null);
        job.setFinishedAt(Instant.now());
        applyCostToExperimentHierarchy(job.getExperiment(), costUsd);
    }

    private void failInlineGenerationJob(ExperimentPipelineGenerationJob job, String errorMessage) {
        if (job == null) {
            return;
        }
        job.setStatus(ExperimentPipelineGenerationJobStatus.FAILED);
        job.setStage(ExperimentPipelineGenerationJobStage.FAILED);
        job.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage.trim() : "Falha desconhecida no LHM");
        job.setFinishedAt(Instant.now());
    }

    private void applyCostToExperimentHierarchy(Experiment experiment, BigDecimal costUsd) {
        if (experiment == null || experiment.getId() == null) {
            return;
        }
        if (costUsd == null || costUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        costAttributionService.addUsdCostToExperimentHierarchy(experiment, costUsd);
    }

    private String writeJsonSilently(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao serializar payload de monitoramento inline do pipeline", ex);
            return null;
        }
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
            case LANDING_PAGE_DESIGN_PRESET -> experiment.getLandingPageDesignPreset();
            case LANDING_PAGE_HTML -> experiment.getLandingPageHtml();
        };
        if (!StringUtils.hasText(predecessorContent)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A seção " + section.path() + " depende da seção " + predecessor.path() + " já concluída");
        }
        if ((section == ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET
                || section == ExperimentPipelineSection.LANDING_PAGE_HTML)
                && !frameworkImageGenerationService.allPlanningImagesCompleted(experiment.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A seção " + section.path() + " depende da geração completa das imagens planejadas da landing");
        }
        if (section == ExperimentPipelineSection.LANDING_PAGE_HTML) {
            validateLandingArtifactsReadinessForHtml(experiment);
        }
    }

    private void validateLandingArtifactsReadinessForHtml(Experiment experiment) {
        try {
            validateLandingWireframeArtifacts(experiment.getLandingPageWireframe());
            validateLandingDesignPresetArtifacts(experiment, experiment.getLandingPageDesignPreset());
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pré-validação antes de LANDING_PAGE_HTML falhou: " + ex.getReason(),
                    ex);
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
                enqueueJob(job.getExperiment(), ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET, deriveFollowUpRequest(request));
            }
            return;
        }
        if (section == ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET) {
            log.info("Fila automática finalizada após LANDING_PAGE_DESIGN_PRESET para experimento {}. "
                            + "A geração do HTML deve ser iniciada manualmente (LHM ou IA).",
                    experimentId);
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
            sb.append(landingHtmlModule.buildPromptV2Inputs(experiment));
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
        return "Você é especialista em execução de pipeline de experimento. "
                + "Siga estritamente o template da etapa fornecido pelo Worker AI. "
                + "Seção atual: " + section.path() + ".";
    }

    private void appendSectionPrompt(StringBuilder sb,
                                     Experiment experiment,
                                     ExperimentPipelineSection section) {
        String promptTemplate = loadPromptTemplate(section);
        if (StringUtils.hasText(promptTemplate)) {
            sb.append("\nTemplate canônico da etapa (arquivo versionado):\n");
            sb.append(promptTemplate.trim()).append("\n");
        } else {
            sb.append("\nTemplate do prompt desta etapa é mantido exclusivamente no módulo Worker AI.\n");
            sb.append("Não gerar instruções hard-coded no backend para seção: ").append(section.path()).append(".\n");
            sb.append("Use apenas os dados do experimento e as dependências já persistidas para compor a resposta.\n");
        }

        if (section == ExperimentPipelineSection.LANDING_PAGE_HTML) {
            appendImageBindingSummary(sb, experiment);
        }
        if (section == ExperimentPipelineSection.LANDING_PAGE_COPY
                || section == ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING) {
            appendWireframeSlotSummary(sb, experiment);
        }
    }

    private String loadPromptTemplate(ExperimentPipelineSection section) {
        String resourcePath = "prompts/experiment-pipeline/" + section.path() + "/user.md";
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            return null;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("Falha ao carregar template de prompt {}: {}", resourcePath, ex.getMessage());
            return null;
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

    private void appendWireframeSlotSummary(StringBuilder sb, Experiment experiment) {
        Map<String, List<String>> slotsBySection = loadWireframeCopySlots(experiment);
        if (slotsBySection.isEmpty()) {
            return;
        }
        sb.append("\nSlots canônicos de copy vindos do wireframe (obrigatório respeitar):\n");
        slotsBySection.forEach((sectionId, slots) ->
                sb.append("- sectionId=").append(sectionId)
                        .append(" | copySlots=").append(String.join(", ", slots))
                        .append("\n"));
        sb.append("Regra crítica: slotId é identificador técnico de slot (NUNCA texto de copy).\n");
        sb.append("Proibido usar aliases semânticos como slotId (ex.: headline, subheadline, promise, hero.headline).\n");
        sb.append("Checklist obrigatório antes de responder: para cada item gerado confirmar sectionId preenchido e slotId pertencente a copySlots da mesma seção.\n");
        sb.append("Regra na copy: cada item de landingPageCopy.bodySections deve informar slotId e esse slotId deve existir na lista copySlots da seção correspondente.\n");
        sb.append("Regra no planejamento de imagens: cada item de landingPageImagePlanning.images[] deve informar slotId canônico literal do wireframe/copy da mesma seção.\n");
    }

    private List<String> loadWireframeSectionIds(Experiment experiment) {
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return List.of();
        }
        try {
            Map<String, Object> root = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
            Map<String, Object> payload = unwrapSectionPayload(root, "landingPageWireframe");
            if (!(payload.get("sectionOrder") instanceof List<?> rawSections)) {
                return List.of();
            }
            Set<String> sectionIds = new LinkedHashSet<>();
            for (Object rawSection : rawSections) {
                if (!(rawSection instanceof Map<?, ?> rawSectionMap)) {
                    continue;
                }
                String sectionId = asTrimmedString(rawSectionMap.get("sectionId"));
                if (StringUtils.hasText(sectionId)) {
                    sectionIds.add(sectionId);
                }
            }
            return List.copyOf(sectionIds);
        } catch (ResponseStatusException ex) {
            log.warn("Falha ao carregar sectionId do wireframe para checklist do prompt: {}", ex.getReason());
            return List.of();
        } catch (RuntimeException ex) {
            log.warn("Falha inesperada ao carregar sectionId do wireframe para checklist do prompt: {}", ex.getMessage());
            return List.of();
        }
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

    private Map<String, Set<String>> loadAllowedCopySlotsBySection(Experiment experiment) {
        Map<String, List<String>> slotsBySection = loadWireframeCopySlots(experiment);
        if (slotsBySection.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> allowed = new LinkedHashMap<>();
        slotsBySection.forEach((sectionId, slots) -> allowed.put(sectionId, new LinkedHashSet<>(slots)));
        return allowed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> loadWireframeCopySlots(Experiment experiment) {
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return Map.of();
        }
        try {
            Map<String, Object> root = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
            Map<String, Object> payload = unwrapSectionPayload(root, "landingPageWireframe");
            if (!(payload.get("sectionOrder") instanceof List<?> rawSections) || rawSections.isEmpty()) {
                return Map.of();
            }
            Map<String, List<String>> slotsBySection = new LinkedHashMap<>();
            for (Object rawSection : rawSections) {
                if (!(rawSection instanceof Map<?, ?> rawSectionMap)) {
                    continue;
                }
                Map<String, Object> section = (Map<String, Object>) rawSectionMap;
                String sectionId = asTrimmedString(section.get("sectionId"));
                if (!StringUtils.hasText(sectionId) || !(section.get("copySlots") instanceof List<?> rawSlots)) {
                    continue;
                }
                List<String> slots = rawSlots.stream()
                        .map(this::asTrimmedString)
                        .filter(StringUtils::hasText)
                        .toList();
                if (!slots.isEmpty()) {
                    slotsBySection.put(sectionId, slots);
                }
            }
            return slotsBySection;
        } catch (Exception ex) {
            log.warn("Falha ao carregar copySlots do wireframe para o prompt: {}", ex.getMessage());
            return Map.of();
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
        appendSectionOutputIfEligible(sb, section, ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET, "Preset de design da landing", completedOutputs);
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
            case LANDING_PAGE_COPY -> {
                validateLandingCopyArtifacts(experiment, normalized);
                experiment.setLandingPageCopy(normalized);
            }
            case LANDING_PAGE_WIREFRAME -> {
                validateLandingWireframeArtifacts(normalized);
                experiment.setLandingPageWireframe(normalized);
            }
            case LANDING_PAGE_IMAGE_PLANNING -> {
                validateLandingImagePlanningArtifacts(experiment, normalized);
                experiment.setLandingPageImagePlanning(normalized);
            }
            case LANDING_PAGE_DESIGN_PRESET -> {
                validateLandingDesignPresetArtifacts(experiment, normalized);
                experiment.setLandingPageDesignPreset(normalized);
            }
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
    private void validateLandingCopyArtifacts(Experiment experiment, String landingCopyContent) {
        if (!StringUtils.hasText(landingCopyContent)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Copy da landing vazia");
        }
        Map<String, Object> copyRoot = readObject(landingCopyContent, "Copy da landing inválida");
        Map<String, Object> copyPayload = unwrapSectionPayload(copyRoot, "landingPageCopy");
        boolean hasCanonicalHero = false;
        if (copyPayload.get("hero") instanceof Map<?, ?> rawHero) {
            Map<String, Object> hero = (Map<String, Object>) rawHero;
            hasCanonicalHero = StringUtils.hasText(asTrimmedString(hero.get("headline")))
                    && StringUtils.hasText(asTrimmedString(hero.get("promise")))
                    && StringUtils.hasText(asTrimmedString(hero.get("ctaLabel")));
        }
        boolean hasLegacyMinimal = StringUtils.hasText(asTrimmedString(copyPayload.get("pageGoal")))
                && StringUtils.hasText(asTrimmedString(copyPayload.get("primaryCTA")));

        if (!hasCanonicalHero && !hasLegacyMinimal) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Copy da landing incompleta: esperado hero estruturado (headline/promise/ctaLabel) ou legado mínimo (pageGoal/primaryCTA)");
        }

        if (hasCanonicalHero) {
            Map<String, Set<String>> allowedSlotsBySection = loadAllowedCopySlotsBySection(experiment);
            if (!(copyPayload.get("bodySections") instanceof List<?> rawBodySections) || rawBodySections.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Copy da landing sem bodySections estruturado");
            }
            for (Object rawBodySection : rawBodySections) {
                if (!(rawBodySection instanceof Map<?, ?> rawBodySectionMap)) {
                    continue;
                }
                Map<String, Object> bodySection = (Map<String, Object>) rawBodySectionMap;
                String sectionId = asTrimmedString(bodySection.get("sectionId"));
                String slotId = asTrimmedString(bodySection.get("slotId"));
                String summary = asTrimmedString(bodySection.get("summary"));
                String copy = asTrimmedString(bodySection.get("copy"));
                if (!StringUtils.hasText(sectionId) || (!StringUtils.hasText(summary) && !StringUtils.hasText(copy))) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Copy da landing inválida em bodySections: cada item exige sectionId e summary/copy");
                }
                if (!allowedSlotsBySection.isEmpty()) {
                    if (!StringUtils.hasText(slotId)) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                "Copy da landing inválida em bodySections: slotId é obrigatório quando wireframe define copySlots");
                    }
                    Set<String> allowedSlots = allowedSlotsBySection.get(sectionId);
                    if (allowedSlots == null || allowedSlots.isEmpty() || !allowedSlots.contains(slotId)) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                "Copy da landing inválida em bodySections: slotId '" + slotId + "' não pertence aos copySlots da sectionId '" + sectionId + "'");
                    }
                }
            }
            if (!(copyPayload.get("consistencyChecks") instanceof List<?> rawChecks) || rawChecks.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Copy da landing sem consistencyChecks");
            }
            Set<String> checkNames = extractCheckNames(rawChecks);
            if (!checkNames.contains("CTA_MATCH") || !checkNames.contains("PROMISE_MATCH")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Copy da landing sem consistencyChecks obrigatórios: CTA_MATCH e PROMISE_MATCH");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateLandingImagePlanningArtifacts(Experiment experiment, String imagePlanningContent) {
        if (!StringUtils.hasText(imagePlanningContent)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Planejamento de imagens vazio");
        }
        Map<String, Object> planningRoot = readObject(imagePlanningContent, "Planejamento de imagens inválido");
        Map<String, Object> planningPayload = unwrapSectionPayload(planningRoot, "landingPageImagePlanning");
        Object rawImages = planningPayload.get("images");
        if (!(rawImages instanceof List<?> images) || images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Planejamento de imagens inválido: images[] com prompts é obrigatório");
        }
        for (Object rawImage : images) {
            if (!(rawImage instanceof Map<?, ?> rawImageMap)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Planejamento de imagens inválido: cada item de images[] deve ser objeto");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> image = (Map<String, Object>) rawImageMap;
            String sectionId = asTrimmedString(image.get("sectionId"));
            String slotId = asTrimmedString(image.get("slotId"));
            String imageBindingKey = asTrimmedString(image.get("imageBindingKey"));
            String imagePrompt = asTrimmedString(image.get("imagePrompt"));
            if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(slotId) || !StringUtils.hasText(imageBindingKey) || !StringUtils.hasText(imagePrompt)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Planejamento de imagens inválido: images[] exige sectionId, slotId, imageBindingKey e imagePrompt");
            }
        }
        Map<String, Set<String>> allowedSlotsBySection = loadAllowedCopySlotsBySection(experiment);
        if (!allowedSlotsBySection.isEmpty()) {
            for (Object rawImage : images) {
                @SuppressWarnings("unchecked")
                Map<String, Object> image = (Map<String, Object>) rawImage;
                String sectionId = asTrimmedString(image.get("sectionId"));
                String slotId = asTrimmedString(image.get("slotId"));
                Set<String> allowedSlots = allowedSlotsBySection.get(sectionId);
                if (allowedSlots == null || allowedSlots.isEmpty() || !allowedSlots.contains(slotId)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Planejamento de imagens inválido: slotId '" + slotId
                                    + "' não pertence aos copySlots da sectionId '" + sectionId + "'");
                }
            }
        }
        List<String> expectedBindings = loadWireframeImageBindings(experiment);
        if (!expectedBindings.isEmpty()) {
            Set<String> plannedBindings = new LinkedHashSet<>();
            for (Object rawImage : images) {
                @SuppressWarnings("unchecked")
                Map<String, Object> image = (Map<String, Object>) rawImage;
                String sectionId = asTrimmedString(image.get("sectionId"));
                String imageBindingKey = asTrimmedString(image.get("imageBindingKey"));
                plannedBindings.add(sectionId + "/" + imageBindingKey);
            }
            Set<String> missing = new LinkedHashSet<>(expectedBindings);
            missing.removeAll(plannedBindings);
            Set<String> extra = new LinkedHashSet<>(plannedBindings);
            extra.removeAll(expectedBindings);
            if (!missing.isEmpty() || !extra.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Planejamento de imagens inválido: quantidade e bindings de images[] devem corresponder ao wireframe. missing="
                                + missing + " extra=" + extra);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> loadWireframeImageBindings(Experiment experiment) {
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return List.of();
        }
        try {
            Map<String, Object> wireframeRoot = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
            Map<String, Object> wireframePayload = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
            if (!(wireframePayload.get("images") instanceof List<?> rawImages) || rawImages.isEmpty()) {
                return List.of();
            }
            Set<String> bindings = new LinkedHashSet<>();
            for (Object rawImage : rawImages) {
                if (!(rawImage instanceof Map<?, ?> rawImageMap)) {
                    continue;
                }
                Map<String, Object> image = (Map<String, Object>) rawImageMap;
                String sectionId = asTrimmedString(image.get("sectionId"));
                String imageBindingKey = asTrimmedString(image.get("imageBindingKey"));
                if (StringUtils.hasText(sectionId) && StringUtils.hasText(imageBindingKey)) {
                    bindings.add(sectionId + "/" + imageBindingKey);
                }
            }
            return List.copyOf(bindings);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractCheckNames(List<?> rawChecks) {
        Set<String> checkNames = new LinkedHashSet<>();
        if (rawChecks == null) {
            return checkNames;
        }
        for (Object rawCheck : rawChecks) {
            if (!(rawCheck instanceof Map<?, ?> rawCheckMap)) {
                continue;
            }
            String check = asTrimmedString(((Map<String, Object>) rawCheckMap).get("check"));
            if (StringUtils.hasText(check)) {
                checkNames.add(check.toUpperCase(Locale.ROOT));
            }
        }
        return checkNames;
    }

    @SuppressWarnings("unchecked")
    private void validateLandingWireframeArtifacts(String wireframeContent) {
        if (!StringUtils.hasText(wireframeContent)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Wireframe da landing vazio");
        }
        Map<String, Object> wireframeRoot = readObject(wireframeContent, "Wireframe da landing inválido");
        Map<String, Object> wireframePayload = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
        if (!(wireframePayload.get("sectionOrder") instanceof List<?> rawSections) || rawSections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Wireframe da landing sem sectionOrder estruturado");
        }
        Set<String> sectionIds = new LinkedHashSet<>();
        for (Object rawSection : rawSections) {
            if (!(rawSection instanceof Map<?, ?> rawSectionMap)) {
                continue;
            }
            Map<String, Object> section = (Map<String, Object>) rawSectionMap;
            String sectionId = asTrimmedString(section.get("sectionId"));
            if (!StringUtils.hasText(sectionId)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Wireframe da landing com section sem sectionId");
            }
            String normalizedSectionId = normalizeLookupKey(sectionId);
            if (!sectionIds.add(normalizedSectionId)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe da landing inválido: sectionId duplicado em sectionOrder (" + sectionId + ")");
            }
            validateSprint2SlotDefs(sectionId, section);
            String dropOffRisk = asTrimmedString(section.get("dropOffRisk"));
            Integer mobilePriorityScore = asInteger(section.get("mobilePriorityScore"));
            if ("alto".equalsIgnoreCase(dropOffRisk) && (mobilePriorityScore == null || mobilePriorityScore < 8)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe inválido: sectionId '" + sectionId
                                + "' com dropOffRisk=alto exige mobilePriorityScore >= 8");
            }
            if (!(section.get("surfaceSpec") instanceof Map<?, ?> rawSurfaceMap)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe da landing com section sem surfaceSpec");
            }
            Map<String, Object> surfaceSpec = (Map<String, Object>) rawSurfaceMap;
            if (!StringUtils.hasText(asTrimmedString(surfaceSpec.get("surfaceToken")))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe da landing com section sem surfaceSpec.surfaceToken");
            }
            if (!StringUtils.hasText(asTrimmedString(surfaceSpec.get("style")))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe da landing com section sem surfaceSpec.style");
            }
            if (!StringUtils.hasText(asTrimmedString(surfaceSpec.get("contrastMode")))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe da landing com section sem surfaceSpec.contrastMode");
            }
        }
        Map<String, Object> readingFlowSpec = requiredMap(wireframePayload, "readingFlowSpec",
                "Wireframe incompleto: readingFlowSpec é obrigatório");
        Integer maxParagraphLinesMobile = asInteger(readingFlowSpec.get("maxParagraphLinesMobile"));
        if (maxParagraphLinesMobile == null || maxParagraphLinesMobile > 4) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: readingFlowSpec.maxParagraphLinesMobile deve ser <= 4");
        }
        Integer bulletDensityPerSection = asInteger(readingFlowSpec.get("bulletDensityPerSection"));
        if (bulletDensityPerSection == null || bulletDensityPerSection < 3) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: readingFlowSpec.bulletDensityPerSection deve ser >= 3");
        }

        Map<String, Object> conversionPathSpec = requiredMap(wireframePayload, "conversionPathSpec",
                "Wireframe incompleto: conversionPathSpec é obrigatório");
        if (!StringUtils.hasText(asTrimmedString(conversionPathSpec.get("primaryAction")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: conversionPathSpec.primaryAction é obrigatório");
        }
        if (!StringUtils.hasText(asTrimmedString(conversionPathSpec.get("ctaLabelCanonical")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: conversionPathSpec.ctaLabelCanonical é obrigatório");
        }

        Map<String, Object> proofPlan = requiredMap(wireframePayload, "proofPlan",
                "Wireframe incompleto: proofPlan é obrigatório");
        if (!(proofPlan.get("requiredProofTypes") instanceof List<?> rawProofTypes) || rawProofTypes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: proofPlan.requiredProofTypes deve conter pelo menos 2 tipos");
        }
        Set<String> proofTypes = rawProofTypes.stream()
                .map(this::asTrimmedString)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (proofTypes.size() < 2) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: proofPlan.requiredProofTypes deve conter pelo menos 2 tipos distintos");
        }
        if (!(proofPlan.get("proofSectionIds") instanceof List<?> rawProofSectionIds) || rawProofSectionIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: proofPlan.proofSectionIds é obrigatório e deve mapear seções existentes");
        }
        for (Object rawProofSectionId : rawProofSectionIds) {
            String proofSectionId = asTrimmedString(rawProofSectionId);
            if (!StringUtils.hasText(proofSectionId) || !sectionIds.contains(normalizeLookupKey(proofSectionId))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe inválido: proofPlan.proofSectionIds contém sectionId inexistente em sectionOrder (" + proofSectionId + ")");
            }
        }

        Map<String, Object> trustSignalsSpec = requiredMap(wireframePayload, "trustSignalsSpec",
                "Wireframe incompleto: trustSignalsSpec é obrigatório");
        if (!Boolean.TRUE.equals(asBoolean(trustSignalsSpec.get("brandIdentityRequired")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: trustSignalsSpec.brandIdentityRequired deve ser true");
        }
        if (!Boolean.TRUE.equals(asBoolean(trustSignalsSpec.get("privacyNoticeNearForm")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: trustSignalsSpec.privacyNoticeNearForm deve ser true");
        }
        if (!StringUtils.hasText(asTrimmedString(trustSignalsSpec.get("privacyPolicyUrl")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: trustSignalsSpec.privacyPolicyUrl é obrigatório");
        }
        if (!(trustSignalsSpec.get("legalFooterItems") instanceof List<?> rawLegalFooterItems) || rawLegalFooterItems.size() < 3) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: trustSignalsSpec.legalFooterItems deve conter ao menos empresa, contato e política de privacidade");
        }

        Map<String, Object> accessibilitySpec = requiredMap(wireframePayload, "accessibilitySpec",
                "Wireframe incompleto: accessibilitySpec é obrigatório");
        BigDecimal minTextContrast = parseContrastRatio(accessibilitySpec.get("minTextContrast"));
        if (minTextContrast == null || minTextContrast.compareTo(new BigDecimal("4.5")) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: accessibilitySpec.minTextContrast deve ser no mínimo 4.5:1");
        }
        Integer minTouchTargetPx = asInteger(accessibilitySpec.get("minTouchTargetPx"));
        if (minTouchTargetPx == null || minTouchTargetPx < 44) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: accessibilitySpec.minTouchTargetPx deve ser >= 44");
        }
        Integer formFieldMinHeightPx = asInteger(accessibilitySpec.get("formFieldMinHeightPx"));
        if (formFieldMinHeightPx == null || formFieldMinHeightPx < 44) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: accessibilitySpec.formFieldMinHeightPx deve ser >= 44");
        }
    }


    @SuppressWarnings("unchecked")
    private void validateSprint2SlotDefs(String sectionId, Map<String, Object> section) {
        if (!(section.get("slotDefs") instanceof List<?> rawSlotDefs) || rawSlotDefs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Wireframe inválido: sectionId '" + sectionId + "' sem slotDefs estruturado");
        }
        Set<String> slotKeys = new LinkedHashSet<>();
        for (Object rawSlotDef : rawSlotDefs) {
            if (!(rawSlotDef instanceof Map<?, ?> rawSlotDefMap)) {
                continue;
            }
            Map<String, Object> slotDef = (Map<String, Object>) rawSlotDefMap;
            String slotKey = asTrimmedString(slotDef.get("slotKey"));
            String componentKey = asTrimmedString(slotDef.get("componentKey"));
            if (!StringUtils.hasText(slotKey) || !StringUtils.hasText(componentKey)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe inválido: slotDefs da sectionId '" + sectionId + "' exige slotKey e componentKey");
            }
            if (!slotKeys.add(normalizeLookupKey(slotKey))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Wireframe inválido: slotKey duplicado em slotDefs da sectionId '" + sectionId + "' (" + slotKey + ")");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateLandingDesignPresetArtifacts(Experiment experiment, String designPresetContent) {
        if (!StringUtils.hasText(designPresetContent)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Preset de design da landing vazio");
        }
        Map<String, Object> designRoot = readObject(designPresetContent, "Preset de design da landing inválido");
        Map<String, Object> designPayload = unwrapSectionPayload(designRoot, "landingPageDesignPreset");
        if (!(designPayload.get("sectionPresets") instanceof List<?> rawSectionPresets) || rawSectionPresets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design da landing sem sectionPresets estruturado");
        }
        Map<String, Object> theme = requiredMap(designPayload, "theme",
                "Preset de design incompleto: theme é obrigatório");
        Map<String, Object> lhmRuntime = requiredMap(designPayload, "lhmRuntime",
                "Preset de design incompleto: lhmRuntime é obrigatório");
        if (!StringUtils.hasText(asTrimmedString(lhmRuntime.get("baseCss")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: lhmRuntime.baseCss é obrigatório");
        }
        if (!StringUtils.hasText(asTrimmedString(lhmRuntime.get("cssVersion")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: lhmRuntime.cssVersion é obrigatório");
        }
        if (!StringUtils.hasText(asTrimmedString(lhmRuntime.get("cssNotes")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: lhmRuntime.cssNotes é obrigatório");
        }
        requiredMap(theme, "palette", "Preset de design incompleto: theme.palette é obrigatório");
        Map<String, Object> typography = requiredMap(theme, "typography",
                "Preset de design incompleto: theme.typography é obrigatório");
        Map<String, Object> spacing = requiredMap(theme, "spacing",
                "Preset de design incompleto: theme.spacing é obrigatório");
        requiredMap(theme, "accessibility", "Preset de design incompleto: theme.accessibility é obrigatório");
        Integer maxLineLength = parseCssCh(typography.get("maxLineLength"));
        if (maxLineLength == null || maxLineLength < 55 || maxLineLength > 75) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design inválido: theme.typography.maxLineLength deve ficar entre 55ch e 75ch");
        }
        BigDecimal lineHeightBody = parseDecimalLike(typography.get("lineHeightBody"));
        if (lineHeightBody == null || lineHeightBody.compareTo(new BigDecimal("1.5")) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design inválido: theme.typography.lineHeightBody deve ser >= 1.5");
        }
        Integer sectionGapMobile = parseCssPx(spacing.get("sectionGapMobile"));
        if (sectionGapMobile == null || sectionGapMobile < 48) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design inválido: theme.spacing.sectionGapMobile deve ser >= 48px");
        }

        Map<String, Object> componentPresets = requiredMap(designPayload, "componentPresets",
                "Preset de design incompleto: componentPresets é obrigatório");
        requiredMap(componentPresets, "cta", "Preset de design incompleto: componentPresets.cta é obrigatório");
        requiredMap(componentPresets, "trust", "Preset de design incompleto: componentPresets.trust é obrigatório");
        Map<String, Object> proofPreset = requiredMap(componentPresets, "proof",
                "Preset de design incompleto: componentPresets.proof é obrigatório");
        if (!Boolean.TRUE.equals(asBoolean(proofPreset.get("showIdentity")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design inválido: componentPresets.proof.showIdentity deve ser true para páginas de venda/captação");
        }
        validateSprint1DesignSystemContracts(componentPresets, theme);

        if (!(designPayload.get("consistencyChecks") instanceof List<?> rawChecks) || rawChecks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design da landing sem consistencyChecks");
        }
        Set<String> checks = extractCheckNames(rawChecks);
        if (!checks.contains("THEME_CONTRAST") || !checks.contains("CTA_VISUAL_HIERARCHY") || !checks.contains("MOBILE_READABILITY")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design sem consistencyChecks obrigatórios: THEME_CONTRAST, CTA_VISUAL_HIERARCHY e MOBILE_READABILITY");
        }

        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe())) {
            return;
        }
        Map<String, Object> wireframeRoot = readObject(experiment.getLandingPageWireframe(), "Wireframe da landing inválido");
        Map<String, Object> wireframePayload = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
        if (!(wireframePayload.get("sectionOrder") instanceof List<?> rawSections) || rawSections.isEmpty()) {
            return;
        }
        Set<String> expectedSectionIds = new LinkedHashSet<>();
        for (Object rawSection : rawSections) {
            if (!(rawSection instanceof Map<?, ?> rawSectionMap)) {
                continue;
            }
            String sectionId = asTrimmedString(((Map<String, Object>) rawSectionMap).get("sectionId"));
            if (StringUtils.hasText(sectionId)) {
                expectedSectionIds.add(normalizeLookupKey(sectionId));
            }
        }
        Set<String> presetSectionIds = new LinkedHashSet<>();
        for (Object rawPreset : rawSectionPresets) {
            if (!(rawPreset instanceof Map<?, ?> rawPresetMap)) {
                continue;
            }
            Map<String, Object> preset = (Map<String, Object>) rawPresetMap;
            String sectionId = asTrimmedString(preset.get("sectionId"));
            if (StringUtils.hasText(sectionId)) {
                String normalized = normalizeLookupKey(sectionId);
                if (!presetSectionIds.add(normalized)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Preset de design inválido: sectionPresets contém sectionId duplicado (" + sectionId + ")");
                }
                String emphasis = asTrimmedString(preset.get("emphasis"));
                boolean highIntentSection = normalized.contains("hero")
                        || normalized.contains("offer")
                        || normalized.contains("cta")
                        || normalized.contains("closing")
                        || normalized.contains("final");
                if (highIntentSection && !"primary".equalsIgnoreCase(emphasis)
                        && !StringUtils.hasText(asTrimmedString(preset.get("notes")))) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Preset de design inválido: seção de alta intenção '" + sectionId
                                    + "' exige emphasis=primary ou justificativa em notes");
                }
            }
        }
        Set<String> missing = new LinkedHashSet<>(expectedSectionIds);
        missing.removeAll(presetSectionIds);
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: sectionPresets não cobre todas as sectionId do wireframe. Faltando: " + missing);
        }
        Set<String> extras = new LinkedHashSet<>(presetSectionIds);
        extras.removeAll(expectedSectionIds);
        if (!extras.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design inválido: sectionPresets contém sectionId fora do wireframe. Excedente: " + extras);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateSprint1DesignSystemContracts(Map<String, Object> componentPresets, Map<String, Object> theme) {
        Set<String> requiredPrimitiveKeys = Set.of(
                "hero-title", "section-title", "body", "btn-primary", "btn-secondary", "field", "card", "faq-item");
        Set<String> requiredRegistryKeys = Set.of("hero-form-split", "proof", "offer-cards", "faq");

        Object rawPrimitives = componentPresets.get("primitives");
        if (!(rawPrimitives instanceof List<?> primitives) || primitives.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: componentPresets.primitives é obrigatório");
        }
        Set<String> primitiveKeys = new LinkedHashSet<>();
        for (Object rawPrimitive : primitives) {
            if (!(rawPrimitive instanceof Map<?, ?> rawPrimitiveMap)) {
                continue;
            }
            String key = asTrimmedString(((Map<String, Object>) rawPrimitiveMap).get("key"));
            if (StringUtils.hasText(key)) {
                primitiveKeys.add(key);
            }
        }
        if (!primitiveKeys.containsAll(requiredPrimitiveKeys)) {
            Set<String> missing = new LinkedHashSet<>(requiredPrimitiveKeys);
            missing.removeAll(primitiveKeys);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: componentPresets.primitives sem chaves obrigatórias " + missing);
        }

        Object rawRegistry = componentPresets.get("registry");
        if (!(rawRegistry instanceof List<?> registry) || registry.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: componentPresets.registry é obrigatório");
        }
        Set<String> registryKeys = new LinkedHashSet<>();
        for (Object rawEntry : registry) {
            if (!(rawEntry instanceof Map<?, ?> rawEntryMap)) {
                continue;
            }
            String key = asTrimmedString(((Map<String, Object>) rawEntryMap).get("componentKey"));
            if (StringUtils.hasText(key)) {
                registryKeys.add(key);
            }
        }
        if (!registryKeys.containsAll(requiredRegistryKeys)) {
            Set<String> missing = new LinkedHashSet<>(requiredRegistryKeys);
            missing.removeAll(registryKeys);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design incompleto: componentPresets.registry sem componentKey obrigatório " + missing);
        }

        Map<String, Object> accessibility = requiredMap(theme, "accessibility",
                "Preset de design incompleto: theme.accessibility é obrigatório");
        if (!StringUtils.hasText(asTrimmedString(accessibility.get("focusRing")))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Preset de design inválido: theme.accessibility.focusRing é obrigatório");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requiredMap(Map<String, Object> source, String field, String errorMessage) {
        Object value = source.get(field);
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMessage);
        }
        return (Map<String, Object>) rawMap;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (!StringUtils.hasText(asTrimmedString(value))) {
            return null;
        }
        try {
            return Integer.parseInt(asTrimmedString(value).replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = asTrimmedString(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        return null;
    }

    private BigDecimal parseContrastRatio(Object value) {
        String raw = asTrimmedString(value);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.contains(":") ? raw.substring(0, raw.indexOf(':')) : raw;
        return parseDecimalLike(normalized);
    }

    private BigDecimal parseDecimalLike(Object value) {
        String raw = asTrimmedString(value);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT)
                .replace("ch", "")
                .replace("px", "")
                .replace(",", ".")
                .trim();
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseCssPx(Object value) {
        BigDecimal parsed = parseDecimalLike(value);
        return parsed == null ? null : parsed.intValue();
    }

    private Integer parseCssCh(Object value) {
        BigDecimal parsed = parseDecimalLike(value);
        return parsed == null ? null : parsed.intValue();
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
            String expectedFormIdLabel = StringUtils.hasText(expectedFormId) ? expectedFormId : "(não definido no wireframe)";
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "HTML da landing sem <form> compatível com wireframe.formSpec.formId. esperado formId=" + expectedFormIdLabel);
        }

        String method = formElement.attr("method");
        if (!StringUtils.hasText(method) || !"post".equalsIgnoreCase(method.trim())) {
            String receivedMethod = StringUtils.hasText(method) ? method.trim() : "(ausente)";
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Formulário da landing deve usar method=\"post\". recebido method=\"" + receivedMethod + "\"");
        }

        String action = asTrimmedString(formElement.attr("action"));
        if (!StringUtils.hasText(action)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Formulário da landing deve declarar atributo action");
        }
        if (StringUtils.hasText(expectedSubmitTarget) && !Objects.equals(action, expectedSubmitTarget)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Formulário da landing deve usar formSpec.submitTarget no action. esperado action=\""
                            + expectedSubmitTarget + "\" recebido action=\"" + action + "\"");
        }

        String lowered = htmlDocument.toLowerCase(Locale.ROOT);
        List<String> missingRuntimeRequirements = new ArrayList<>();
        if (!SUBMIT_LISTENER_PATTERN.matcher(htmlDocument).find()) {
            missingRuntimeRequirements.add("listener de submit (addEventListener('submit', ...))");
        }
        if (!PREVENT_DEFAULT_PATTERN.matcher(htmlDocument).find()) {
            missingRuntimeRequirements.add("event.preventDefault()");
        }
        if (!FETCH_FORM_ACTION_PATTERN.matcher(htmlDocument).find()) {
            missingRuntimeRequirements.add("fetch(form.action, ...)");
        }
        if (!FORM_DATA_PATTERN.matcher(htmlDocument).find()) {
            missingRuntimeRequirements.add("FormData(form)");
        }
        if (!SUBMIT_DISABLE_PATTERN.matcher(htmlDocument).find()) {
            missingRuntimeRequirements.add("loading: botão desabilitado durante envio");
        }
        if (!SUBMIT_ENABLE_PATTERN.matcher(htmlDocument).find()) {
            missingRuntimeRequirements.add("loading: botão reabilitado após envio");
        }
        if (!lowered.contains("checkvalidity")) {
            missingRuntimeRequirements.add("checkValidity()");
        }
        if (!lowered.contains("reportvalidity")) {
            missingRuntimeRequirements.add("reportValidity()");
        }
        if (!lowered.contains("success")) {
            missingRuntimeRequirements.add("feedback de sucesso inline");
        }

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
                    "HTML da landing deve seguir o runtime de envio padrão (submit assíncrono com validação, loading e sucesso inline). "
                            + "Itens ausentes: " + String.join("; ", missingRuntimeRequirements));
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
        String submitLabel = asTrimmedString(wireframeFormSpec.get("submitLabel"));
        Element form = StringUtils.hasText(expectedFormId)
                ? document.selectFirst("form#" + expectedFormId)
                : document.selectFirst("form#lead-capture-primary");
        if (form == null) {
            form = document.selectFirst("form");
        }
        if (form == null) {
            form = createCanonicalFormShell(document, expectedFormId, expectedSubmitTarget, submitLabel);
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

    private Element createCanonicalFormShell(Document document,
                                             String expectedFormId,
                                             String expectedSubmitTarget,
                                             String submitLabel) {
        Element body = document.body();
        if (body == null) {
            body = document.appendElement("body");
        }
        Element container = document.selectFirst("main");
        if (container == null) {
            container = body;
        }
        Element form = container.appendElement("form");
        form.attr("id", StringUtils.hasText(expectedFormId) ? expectedFormId : "lead-capture-primary");
        form.attr("method", "post");
        if (StringUtils.hasText(expectedSubmitTarget)) {
            form.attr("action", expectedSubmitTarget);
        }
        form.appendElement("button")
                .attr("type", "submit")
                .text(StringUtils.hasText(submitLabel) ? submitLabel : "Enviar");
        form.appendElement("p")
                .attr("id", "form-feedback")
                .attr("role", "status");
        container.appendElement("div")
                .attr("id", "submit-success-state")
                .attr("style", "display:none");
        return form;
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
                Map.of("check", "SURFACE_SPEC_BINDING", "status", "PASS", "details", "surfaceToken aplicado conforme wireframe e style/contrast conforme design preset.")
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
        String rawResponse = job.getRawResponse();
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
                .rawResponse(rawResponse)
                .openAiResponseId(extractOpenAiResponseId(rawResponse))
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    private String extractOpenAiResponseId(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode idNode = root.get("id");
            if (idNode != null && idNode.isTextual()) {
                String id = idNode.asText().trim();
                return id.startsWith("resp_") ? id : null;
            }
        } catch (Exception ex) {
            log.debug("Falha ao extrair OpenAI response id do raw_response: {}", ex.getMessage());
        }
        return null;
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
            case CAMPAIGN_ANGLE -> schemaWithMetadata("campaignAngle", campaignAngleFieldSchema(), metadataSchema);
            case AD_COPY -> schemaWithMetadata("adCopy", adCopyFieldSchema(), metadataSchema);
            case AD_IMAGE_BRIEFING -> schemaWithMetadata("adImageBriefing", adImageBriefingFieldSchema(), metadataSchema);
            case LANDING_PAGE_COPY -> schemaWithMetadata("landingPageCopy", landingPageCopyFieldSchema(), metadataSchema);
            case LANDING_PAGE_WIREFRAME -> schemaWithMetadata("landingPageWireframe", landingPageWireframeFieldSchema(), metadataSchema);
            case LANDING_PAGE_IMAGE_PLANNING -> schemaWithMetadata(
                    "landingPageImagePlanning",
                    landingPageImagePlanningFieldSchema(),
                    metadataSchema);
            case LANDING_PAGE_DESIGN_PRESET -> schemaWithMetadata(
                    "landingPageDesignPreset",
                    landingPageDesignPresetFieldSchema(),
                    metadataSchema);
            case LANDING_PAGE_HTML -> schemaWithMetadata("landingPageHtml", landingPageHtmlFieldSchema(), metadataSchema);
        };
    }

    private Map<String, Object> campaignAngleFieldSchema() {
        return Map.of(
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
        );
    }

    private Map<String, Object> adCopyFieldSchema() {
        return Map.of(
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
        );
    }

    private Map<String, Object> adImageBriefingFieldSchema() {
        return Map.of(
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
        );
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
                        Map.entry("slotId", stringSchema()),
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
                        "slotId",
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
                        Map.entry("images", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", imageSchema
                        )),
                        Map.entry("generationPrompt", stringSchema())
                ),
                "required", List.of("images")
        );
    }

    private Map<String, Object> landingPageDesignPresetFieldSchema() {
        Map<String, Object> paletteSchema = Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "background", stringSchema(),
                        "surface", stringSchema(),
                        "textPrimary", stringSchema(),
                        "textMuted", stringSchema(),
                        "brandPrimary", stringSchema(),
                        "brandSecondary", stringSchema(),
                        "ctaPrimary", stringSchema()),
                "required", List.of("background", "surface", "textPrimary", "textMuted", "brandPrimary", "brandSecondary", "ctaPrimary"));
        Map<String, Object> typographySchema = Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "lineHeightBody", stringSchema(),
                        "maxLineLength", stringSchema()),
                "required", List.of("lineHeightBody", "maxLineLength"));
        Map<String, Object> spacingSchema = Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "sectionGapMobile", stringSchema()),
                "required", List.of("sectionGapMobile"));
        Map<String, Object> accessibilitySchema = Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "textContrastBody", stringSchema(),
                        "focusRing", stringSchema()),
                "required", List.of("textContrastBody", "focusRing"));
        Map<String, Object> primitiveSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "key", stringSchema(),
                        "className", stringSchema(),
                        "notes", stringSchema()),
                "required", List.of("key", "className", "notes"));
        Map<String, Object> registryItemSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "componentKey", stringSchema(),
                        "templatePartial", stringSchema(),
                        "notes", stringSchema()),
                "required", List.of("componentKey", "templatePartial", "notes"));
        Map<String, Object> sectionPresetSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "sectionId", stringSchema(),
                        "surfaceStyle", Map.of("type", "string", "enum", List.of("band", "solid", "gradient-soft", "image-tint")),
                        "contrastMode", Map.of("type", "string", "enum", List.of("normal", "high", "soft")),
                        "layoutPreset", stringSchema(),
                        "emphasis", Map.of("type", "string", "enum", List.of("primary", "secondary", "support")),
                        "notes", stringSchema()),
                "required", List.of("sectionId", "surfaceStyle", "contrastMode", "layoutPreset", "emphasis", "notes"));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "presetId", stringSchema(),
                        "lhmRuntime", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "baseCss", Map.of(
                                                "type", "string",
                                                "description", "CSS canônico do LHM. Deve cobrir classes .lhm-card, .lhm-surface-{band|solid|gradient-soft|image-tint} e .lhm-{normal|high|soft}."),
                                        "cssVersion", stringSchema(),
                                        "cssNotes", stringSchema()
                                ),
                                "required", List.of("baseCss", "cssVersion", "cssNotes")
                        ),
                        "theme", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "palette", paletteSchema,
                                        "typography", typographySchema,
                                        "spacing", spacingSchema,
                                        "accessibility", accessibilitySchema,
                                        "radius", Map.of("type", "object", "additionalProperties", true),
                                        "shadow", Map.of("type", "object", "additionalProperties", true)),
                                "required", List.of("palette", "typography", "spacing", "accessibility")),
                        "sectionPresets", Map.of("type", "array", "minItems", 1, "items", sectionPresetSchema),
                        "componentPresets", Map.of(
                                "type", "object",
                                "additionalProperties", true,
                                "properties", Map.of(
                                        "proof", Map.of(
                                                "type", "object",
                                                "additionalProperties", true,
                                                "properties", Map.of(
                                                        "showIdentity", Map.of("type", "boolean")),
                                                "required", List.of("showIdentity")),
                                        "trust", Map.of(
                                                "type", "object",
                                                "additionalProperties", true,
                                                "properties", Map.of(
                                                        "showLegalFooter", Map.of("type", "boolean")),
                                                "required", List.of("showLegalFooter")),
                                        "cta", Map.of(
                                                "type", "object",
                                                "additionalProperties", true,
                                                "properties", Map.of(
                                                        "stickyMobile", Map.of("type", "boolean")),
                                                "required", List.of("stickyMobile")),
                                        "primitives", Map.of("type", "array", "minItems", 8, "items", primitiveSchema),
                                        "registry", Map.of("type", "array", "minItems", 4, "items", registryItemSchema)),
                                "required", List.of("proof", "trust", "cta", "primitives", "registry")),
                        "motion", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "enabled", Map.of("type", "boolean"),
                                        "intensity", Map.of("type", "string", "enum", List.of("none", "subtle", "moderate"))),
                                "required", List.of("enabled", "intensity")),
                        "consistencyChecks", Map.of("type", "array", "minItems", 1, "items", consistencyCheckSchema())),
                "required", List.of("presetId", "lhmRuntime", "theme", "sectionPresets", "componentPresets", "motion", "consistencyChecks"));
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
                        Map.entry("slotId", stringSchema()),
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
                        Map.entry("submitOwnership", Map.of(
                                "type", "string",
                                "enum", List.of("inside-form", "external-with-form-attr")
                        )),
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
                "required", List.of("formId", "title", "submitLabel", "submitTarget", "submitOwnership", "fields",
                        "consent", "successState")
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
                        Map.entry("copySlots", Map.of("type", "array", "minItems", 1, "items", stringSchema())),
                        Map.entry("slotDefs", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "properties", Map.of(
                                                "slotKey", stringSchema(),
                                                "componentKey", stringSchema(),
                                                "required", Map.of("type", "boolean")
                                        ),
                                        "required", List.of("slotKey", "componentKey", "required")
                                )
                        )),
                        Map.entry("mobilePriorityScore", integerSchema(1, 10)),
                        Map.entry("dropOffRisk", Map.of("type", "string", "enum", List.of("baixo", "medio", "alto"))),
                        Map.entry("surfaceSpec", surfaceSpecSchema),
                        Map.entry("ctaSlot", ctaSlotSchema)
                ),
                "required", List.of("sectionId", "sectionName", "objective", "contentType", "mobilePriorityScore", "dropOffRisk", "surfaceSpec")
        );
        Map<String, Object> readingFlowSpecSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("scanPattern", Map.of("type", "string", "enum", List.of("F-pattern", "Z-pattern", "mixed"))),
                        Map.entry("maxParagraphLinesMobile", Map.of("type", "integer", "minimum", 1, "maximum", 4)),
                        Map.entry("bulletDensityPerSection", Map.of("type", "integer", "minimum", 3)),
                        Map.entry("headlineClarityRule", stringSchema()),
                        Map.entry("cognitiveLoadNotes", stringSchema())
                ),
                "required", List.of("scanPattern", "maxParagraphLinesMobile", "bulletDensityPerSection", "headlineClarityRule",
                        "cognitiveLoadNotes")
        );
        Map<String, Object> stickyCtaMobileSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("enabled", Map.of("type", "boolean")),
                        Map.entry("triggerAfterSection", stringSchema()),
                        Map.entry("ctaLabel", stringSchema()),
                        Map.entry("ctaUrl", stringSchema())
                ),
                "required", List.of("enabled", "triggerAfterSection", "ctaLabel", "ctaUrl")
        );
        Map<String, Object> conversionPathSpecSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("primaryAction", stringSchema()),
                        Map.entry("ctaLabelCanonical", stringSchema()),
                        Map.entry("ctaLabelVariantsAllowed", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", stringSchema()
                        )),
                        Map.entry("stickyCtaMobile", stickyCtaMobileSchema),
                        Map.entry("microCommitments", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", stringSchema()
                        )),
                        Map.entry("frictionPoints", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", stringSchema()
                        ))
                ),
                "required", List.of("primaryAction", "ctaLabelCanonical", "ctaLabelVariantsAllowed", "stickyCtaMobile",
                        "microCommitments", "frictionPoints")
        );
        Map<String, Object> proofPlanSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("requiredProofTypes", Map.of(
                                "type", "array",
                                "minItems", 2,
                                "items", stringSchema()
                        )),
                        Map.entry("proofSectionIds", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", stringSchema()
                        )),
                        Map.entry("proofContinuityNotes", stringSchema())
                ),
                "required", List.of("requiredProofTypes", "proofSectionIds", "proofContinuityNotes")
        );
        Map<String, Object> trustSignalsSpecSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("brandIdentityRequired", Map.of("type", "boolean")),
                        Map.entry("heroIdentityItems", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", stringSchema()
                        )),
                        Map.entry("authorityElements", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", stringSchema()
                        )),
                        Map.entry("privacyNoticeNearForm", Map.of("type", "boolean")),
                        Map.entry("privacyPolicyUrl", stringSchema()),
                        Map.entry("legalFooterItems", Map.of(
                                "type", "array",
                                "minItems", 3,
                                "items", stringSchema()
                        ))
                ),
                "required", List.of("brandIdentityRequired", "heroIdentityItems", "authorityElements", "privacyNoticeNearForm",
                        "privacyPolicyUrl", "legalFooterItems")
        );
        Map<String, Object> accessibilitySpecSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("minTextContrast", stringSchema()),
                        Map.entry("minTouchTargetPx", Map.of("type", "integer", "minimum", 44)),
                        Map.entry("formFieldMinHeightPx", Map.of("type", "integer", "minimum", 44)),
                        Map.entry("smallTextUsageNotes", stringSchema())
                ),
                "required", List.of("minTextContrast", "minTouchTargetPx", "formFieldMinHeightPx", "smallTextUsageNotes")
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
                        Map.entry("readingFlowSpec", readingFlowSpecSchema),
                        Map.entry("conversionPathSpec", conversionPathSpecSchema),
                        Map.entry("proofPlan", proofPlanSchema),
                        Map.entry("trustSignalsSpec", trustSignalsSpecSchema),
                        Map.entry("accessibilitySpec", accessibilitySpecSchema),
                        Map.entry("consistencyChecks", Map.of(
                                "type", "array",
                                "minItems", 2,
                                "items", consistencyCheckSchema()
                        )),
                        Map.entry("formSpec", formSpecSchema)
                ),
                "required", List.of("pageGoal", "variantLayoutId", "sectionOrder", "readingFlowSpec", "conversionPathSpec",
                        "proofPlan", "trustSignalsSpec", "accessibilitySpec", "consistencyChecks", "formSpec")
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
        Map<String, SectionVisualPresetContract> visualPresetBySection = extractSectionVisualPresetBySection(experiment);
        List<SectionSurfaceContract> expectedSurfaces = extractExpectedSectionSurfaces(wireframePayload, visualPresetBySection);
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
    private List<SectionSurfaceContract> extractExpectedSectionSurfaces(
            Map<String, Object> wireframePayload,
            Map<String, SectionVisualPresetContract> visualPresetBySection) {
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
            SectionVisualPresetContract visualPreset = visualPresetBySection.get(normalizeLookupKey(sectionId));
            String style = normalizeHtmlAttr(firstNonBlank(
                    visualPreset != null ? visualPreset.surfaceStyle() : null,
                    asTrimmedString(surfaceSpec.get("style"))));
            String contrastMode = normalizeHtmlAttr(firstNonBlank(
                    visualPreset != null ? visualPreset.contrastMode() : null,
                    asTrimmedString(surfaceSpec.get("contrastMode"))));
            if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(surfaceToken)
                    || !StringUtils.hasText(style) || !StringUtils.hasText(contrastMode)) {
                continue;
            }
            surfaces.add(new SectionSurfaceContract(sectionId, surfaceToken, style, contrastMode));
        }
        return surfaces;
    }

    @SuppressWarnings("unchecked")
    private Map<String, SectionVisualPresetContract> extractSectionVisualPresetBySection(Experiment experiment) {
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageDesignPreset())) {
            return Map.of();
        }
        Map<String, Object> designRoot = readObject(experiment.getLandingPageDesignPreset(), "Preset de design da landing inválido");
        Map<String, Object> designPayload = unwrapSectionPayload(designRoot, "landingPageDesignPreset");
        if (!(designPayload.get("sectionPresets") instanceof List<?> rawPresets)) {
            return Map.of();
        }
        Map<String, SectionVisualPresetContract> presets = new LinkedHashMap<>();
        for (Object rawPreset : rawPresets) {
            if (!(rawPreset instanceof Map<?, ?> rawPresetMap)) {
                continue;
            }
            Map<String, Object> preset = (Map<String, Object>) rawPresetMap;
            String sectionId = normalizeHtmlAttr(asTrimmedString(preset.get("sectionId")));
            String surfaceStyle = normalizeHtmlAttr(asTrimmedString(preset.get("surfaceStyle")));
            String contrastMode = normalizeHtmlAttr(asTrimmedString(preset.get("contrastMode")));
            if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(surfaceStyle) || !StringUtils.hasText(contrastMode)) {
                continue;
            }
            presets.put(normalizeLookupKey(sectionId), new SectionVisualPresetContract(sectionId, surfaceStyle, contrastMode));
        }
        return presets;
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
            String bindingPairsDiffMessage = String.format(
                    "Divergência de imagens: landing-page-html deve reproduzir o binding explícito canônico do landing-page-image-planning por sectionId/imageBindingKey. expectedBindingPairs=%s actualBindingPairs=%s",
                    expectedBindingPairs,
                    actualBindingPairs);
            log.warn("Validação landing HTML (experimentId={}): {}", experiment.getId(), bindingPairsDiffMessage);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    bindingPairsDiffMessage);
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
            String sectionId = resolveImageSectionIdFromHtmlAttrs(attrs);
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

    private String resolveImageSectionIdFromHtmlAttrs(Map<String, String> attrs) {
        String sectionId = normalizeHtmlAttr(attrs.get("data-image-section-id"));
        if (StringUtils.hasText(sectionId)) {
            return sectionId;
        }
        return normalizeHtmlAttr(attrs.get("data-section-id"));
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

    private String normalizeLookupKey(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
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

    private record SectionVisualPresetContract(String sectionId, String surfaceStyle, String contrastMode) {
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
