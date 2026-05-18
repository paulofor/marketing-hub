package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.pipeline.service.LandingPageImageInjector;
import com.marketinghub.experiment.repository.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;

@Service
public class GeraLandingStageExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingStageExecutionService.class);
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING_OPENAI_DISPATCH = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_PROCESSING = "EM_PROCESSAMENTO";
    private static final String STATUS_FAILED = "FALHA";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STAGE_WIREFRAME = "landing-page-wireframe";
    private static final String STAGE_COPY = "landing-page-copy";
    private static final String STAGE_IMAGE_PLANNING = "landing-page-image-planning";
    private static final String STAGE_DESIGN_PRESET = "landing-page-design-preset";
    private static final String STAGE_DELIVERABLES = "landing-page-deliverables";

    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;
    private final WireframeProvisionalHtmlAssembler wireframeProvisionalHtmlAssembler;
    private final CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler;
    private final DesignPresetProvisionalHtmlAssembler designPresetProvisionalHtmlAssembler;
    private final LandingPageImageInjector landingPageImageInjector;
    private final RestTemplate restTemplate;
    private final String leadPortalBaseUrl;

    public GeraLandingStageExecutionService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            WireframeProvisionalHtmlAssembler wireframeProvisionalHtmlAssembler,
            CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler,
            DesignPresetProvisionalHtmlAssembler designPresetProvisionalHtmlAssembler,
            LandingPageImageInjector landingPageImageInjector,
            RestTemplate restTemplate,
            @Value("${integrations.lead-portal.base-url:}") String leadPortalBaseUrl) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.wireframeProvisionalHtmlAssembler = wireframeProvisionalHtmlAssembler;
        this.copyProvisionalHtmlAssembler = copyProvisionalHtmlAssembler;
        this.designPresetProvisionalHtmlAssembler = designPresetProvisionalHtmlAssembler;
        this.landingPageImageInjector = landingPageImageInjector;
        this.restTemplate = restTemplate;
        this.leadPortalBaseUrl = leadPortalBaseUrl;
    }

    @Transactional
    public GeraLandingPublishResponse approveAndPublishLanding(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        if (!StringUtils.hasText(experiment.getLandingPageHtml())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Landing HTML ainda não foi gerado para este experimento");
        }
        String slug = "exp-" + experimentId + "-landing-geralanding";
        String htmlWithFunnelControls = injectFunnelControls(experiment.getLandingPageHtml());
        String htmlWithFacebookPixel = injectFacebookPixel(htmlWithFunnelControls, resolveFacebookPixelId(experiment));
        publishToLeadPortal(slug, "Landing GeraLanding - Experimento " + experimentId, htmlWithFacebookPixel.trim());
        try {
            String iframeUrl = resolveIframeUrl(slug);
            String standaloneUrl = resolveStandaloneLandingUrl(iframeUrl);
            experiment.setFollowUpActionUrl(standaloneUrl);
            experimentRepository.save(experiment);
            return new GeraLandingPublishResponse(experimentId, null, iframeUrl, standaloneUrl,
                    "Landing publicada com sucesso pelo GeraLanding.");
        } catch (RuntimeException ex) {
            String rootCauseMessage = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao publicar landing do GeraLanding: " + rootCauseMessage, ex);
        }
    }

    private void publishToLeadPortal(String slug, String name, String html) {
        if (!StringUtils.hasText(leadPortalBaseUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lead Portal base URL não configurada");
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(leadPortalBaseUrl).path("/api/flows/{slug}").buildAndExpand(slug).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        GeraLandingLeadPortalPublishRequest payload = new GeraLandingLeadPortalPublishRequest(
                slug, name, "Fluxo publicado pelo módulo GeraLanding", html, html, "custom_html");
        try {
            restTemplate.put(uri, new HttpEntity<>(payload, headers));
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao enviar fluxo para o Lead Portal", ex);
        }
    }

    private String resolveIframeUrl(String slug) {
        return UriComponentsBuilder.fromHttpUrl(leadPortalBaseUrl)
                .path("/api/public/flows/{slug}")
                .buildAndExpand(slug)
                .toUriString();
    }

    private String injectFunnelControls(String html) {
        String controls = """
                <script data-mh-funnel-controls=\"true\">
                  window.__MH_FUNNEL_CONTROLS__ = { enabled: true, source: 'geralanding' };
                </script>
                """;
        if (html.toLowerCase(Locale.ROOT).contains("data-mh-funnel-controls")) {
            return html;
        }
        if (html.toLowerCase(Locale.ROOT).contains("</head>")) {
            return html.replaceFirst("(?i)</head>", controls + "\n</head>");
        }
        return controls + "\n" + html;
    }

    private String resolveFacebookPixelId(Experiment experiment) {
        if (experiment == null || experiment.getNiche() == null) {
            return null;
        }
        String pixelId = experiment.getNiche().getFacebookPixelId();
        return StringUtils.hasText(pixelId) ? pixelId.trim() : null;
    }

    private String injectFacebookPixel(String html, String facebookPixelId) {
        if (!StringUtils.hasText(html) || !StringUtils.hasText(facebookPixelId)) {
            return html;
        }
        if (html.contains("data-mh-facebook-pixel")) {
            return html;
        }
        String pixelSnippet = """
                <script data-mh-facebook-pixel="true">
                  !function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
                  n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
                  n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
                  t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window, document,'script',
                  'https://connect.facebook.net/en_US/fbevents.js');
                  fbq('init', '%s');
                  fbq('track', 'PageView');
                </script>
                <noscript><img height="1" width="1" style="display:none"
                  src="https://www.facebook.com/tr?id=%s&ev=PageView&noscript=1"
                /></noscript>
                """.formatted(facebookPixelId, facebookPixelId).trim();
        if (html.toLowerCase(Locale.ROOT).contains("</head>")) {
            return html.replaceFirst("(?i)</head>", pixelSnippet + "\n</head>");
        }
        if (html.toLowerCase(Locale.ROOT).contains("<body")) {
            return html.replaceFirst("(?i)<body", pixelSnippet + "\n<body");
        }
        return pixelSnippet + "\n" + html;
    }

    private String resolveStandaloneLandingUrl(String iframeUrl) {
        if (!StringUtils.hasText(iframeUrl)) {
            return null;
        }
        try {
            URI parsed = URI.create(iframeUrl);
            String[] segments = parsed.getPath().split("/");
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

    @Transactional
    public GeraLandingStartResponse registerInitialExecution(Long experimentId, String stageCode) {
        Instant now = Instant.now();
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(stageCode)
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId("manual/start")
                .promptContent("Início manual via interface do experimento.")
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
                .build();
        GeraLandingStageExecution saved = executionRepository.save(execution);
        return new GeraLandingStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
    }

    @Transactional
    public String generateAndPersistProvisionalHtmlFromExperiment(Long experimentId, String jobId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

        if (!StringUtils.hasText(experiment.getLandingPageWireframe())) {
            throw new IllegalStateException("Não foi possível montar HTML provisório: experiment.landingPageWireframe ausente");
        }
        if (!StringUtils.hasText(experiment.getLandingPageCopy())) {
            throw new IllegalStateException("Não foi possível montar HTML provisório: experiment.landingPageCopy ausente");
        }

        String completeHtml = copyProvisionalHtmlAssembler.assembleComplete(
                experiment.getLandingPageCopy(),
                experiment.getLandingPageWireframe(),
                experiment.getLandingPageImagePlanning(),
                experiment.getLandingPageDesignPreset(),
                jobId);
        String htmlWithGeneratedImages = landingPageImageInjector.injectImages(experimentId, completeHtml);
        String enrichedImagePlanning = landingPageImageInjector.injectImageUrlsIntoPlanning(
                experimentId,
                experiment.getLandingPageImagePlanning());
        String provisionalHtml = """
                <!-- AUTO: provisional html generated manually by /geralanding/html/provisional/generate -->
                %s
                """.formatted(htmlWithGeneratedImages);

        if (StringUtils.hasText(enrichedImagePlanning)) {
            experiment.setLandingPageImagePlanning(enrichedImagePlanning);
        }
        experiment.setLandingPageHtml(provisionalHtml);
        experimentRepository.save(experiment);
        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(jobId))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + jobId));
        execution.setProvisionalHtml(provisionalHtml);
        executionRepository.save(execution);
        return provisionalHtml;
    }

    @Transactional
    public void registerWorkerPromptExecution(GeraLandingWorkerPromptRequest request) {
        Instant now = Instant.now();
        Experiment experiment = experimentRepository.findById(request.experimentId())
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + request.experimentId()));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(request.stageCode())
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId(request.jobId())
                .promptContent(request.promptContent())
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(request.jobId()))
                .build();
        executionRepository.save(execution);
    }

    @Transactional
    public void receivePrompt(String idJob, GeraLandingPromptReceiveRequest request) {
        log.info(
                "Receiving gera-landing prompt. idJob={}, experimentId={}, stageCode={}, promptLength={}",
                idJob,
                request.experimentId(),
                request.stageCode(),
                request.prompt() != null ? request.prompt().length() : 0);

        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> fallbackExecutionByExperimentAndStage(request))
                .orElseThrow(() -> {
                    log.error(
                            "GeraLanding execution not found before prompt persistence. idJob={}, experimentId={}, stageCode={}",
                            idJob,
                            request.experimentId(),
                            request.stageCode());
                    return new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob);
                });

        log.info(
                "GeraLanding execution found. idJob={}, persistedExperimentId={}, persistedStageCode={}, previousStatus={}",
                idJob,
                execution.getExperimentId(),
                execution.getStageCode(),
                execution.getStatus());

        Instant now = Instant.now();
        execution.setPrompt(request.prompt());
        execution.setOpenAiJobId(request.openAiJobId());
        execution.setOpenAiRequestBody(request.openAiRequestBody());
        execution.setOpenAiModel(request.openAiModel());
        execution.setSchemaJson(request.schemaJson());
        execution.setPromptMarkdownContent(request.promptMarkdownContent());
        execution.setProcessingStartedAt(now);
        execution.setStatus(STATUS_WAITING_OPENAI_DISPATCH);
        executionRepository.save(execution);
        log.info("GeraLanding prompt persisted. idJob={}, newStatus={}", idJob, execution.getStatus());
    }

    @Transactional
    public void markAsSentToOpenAi(String idJob, GeraLandingDispatchReceiveRequest request) {
        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(request.experimentId(), request.stageCode()))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        execution.setOpenAiJobId(request.openAiJobId());
        execution.setStatus(STATUS_PROCESSING);
        executionRepository.save(execution);
    }

    private java.util.Optional<GeraLandingStageExecution> fallbackExecutionByExperimentAndStage(
            GeraLandingPromptReceiveRequest request) {
        log.warn(
                "Primary lookup by idJob failed. Trying fallback by experiment/stage. experimentId={}, stageCode={}",
                request.experimentId(),
                request.stageCode());
        return executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                request.experimentId(),
                request.stageCode());
    }




    @Transactional
    public void receiveResult(String idJob, GeraLandingResultReceiveRequest request) {
        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(request.experimentId(), request.stageCode()))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        execution.setModelResponse(request.modelResponse());
        String provisionalHtml = resolveProvisionalHtml(idJob, request, execution);
        execution.setProvisionalHtml(provisionalHtml);
        execution.setErrorMessage(StringUtils.hasText(request.errorMessage()) ? request.errorMessage().trim() : null);
        execution.setErrorDetail(StringUtils.hasText(request.errorDetail()) ? request.errorDetail().trim() : null);
        if (request.openAiJobId() != null && !request.openAiJobId().isBlank()) {
            execution.setOpenAiJobId(request.openAiJobId());
        }
        execution.setInputTokens(request.inputTokens());
        execution.setOutputTokens(request.outputTokens());
        execution.setCostUsd(request.costUsd());
        execution.setCompletedAt(Instant.now());
        execution.setStatus(StringUtils.hasText(request.errorMessage()) ? STATUS_FAILED : STATUS_COMPLETED);
        executionRepository.save(execution);
        persistStageArtifactOnExperiment(request, execution);
    }


    private String resolveProvisionalHtml(String idJob, GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        if (StringUtils.hasText(request.provisionalHtml())) {
            return request.provisionalHtml();
        }
        if (STAGE_COPY.equalsIgnoreCase(request.stageCode())) {
            Experiment experiment = execution.getExperiment();
            if (experiment == null && request.experimentId() != null) {
                experiment = experimentRepository.findById(request.experimentId()).orElse(null);
            }
            String wireframe = experiment != null ? experiment.getLandingPageWireframe() : null;
            if (!StringUtils.hasText(wireframe)) {
                throw new IllegalStateException("Não foi possível montar HTML provisório da copy: experiment.landingPageWireframe ausente");
            }
            return copyProvisionalHtmlAssembler.assemble(request.modelResponse(), wireframe, idJob);
        }
        if (STAGE_WIREFRAME.equalsIgnoreCase(request.stageCode())) {
            return wireframeProvisionalHtmlAssembler.assemble(request.modelResponse(), idJob);
        }
        if (STAGE_IMAGE_PLANNING.equalsIgnoreCase(request.stageCode())) {
            return resolveImagePlanningProvisionalHtml(idJob, request, execution);
        }
        if (STAGE_DESIGN_PRESET.equalsIgnoreCase(request.stageCode())) {
            return resolveDesignPresetProvisionalHtml(idJob, request, execution);
        }
        return null;
    }

    private String resolveImagePlanningProvisionalHtml(String idJob, GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        Experiment experiment = execution.getExperiment();
        if (experiment == null && request.experimentId() != null) {
            experiment = experimentRepository.findById(request.experimentId()).orElse(null);
        }
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe()) || !StringUtils.hasText(experiment.getLandingPageCopy())) {
            return null;
        }
        String baseHtml = copyProvisionalHtmlAssembler.assembleComplete(
                experiment.getLandingPageCopy(),
                experiment.getLandingPageWireframe(),
                request.modelResponse(),
                experiment.getLandingPageDesignPreset(),
                idJob);
        if (!StringUtils.hasText(baseHtml)) {
            return null;
        }
        String htmlWithGeneratedImages = landingPageImageInjector.injectImages(experiment.getId(), baseHtml);
        return """
                <!-- AUTO: provisional html regenerated after landing-page-image-planning completion -->
                %s
                """.formatted(htmlWithGeneratedImages);
    }

    private String resolveDesignPresetProvisionalHtml(String idJob, GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        Experiment experiment = execution.getExperiment();
        if (experiment == null && request.experimentId() != null) {
            experiment = experimentRepository.findById(request.experimentId()).orElse(null);
        }
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe()) || !StringUtils.hasText(experiment.getLandingPageCopy())) {
            return null;
        }
        String completeHtml = designPresetProvisionalHtmlAssembler.assemble(
                experiment.getLandingPageWireframe(),
                experiment.getLandingPageCopy(),
                experiment.getLandingPageImagePlanning(),
                request.modelResponse(),
                idJob);
        if (!StringUtils.hasText(completeHtml)) {
            return null;
        }
        String htmlWithGeneratedImages = landingPageImageInjector.injectImages(experiment.getId(), completeHtml);
        return """
                <!-- AUTO: provisional html regenerated after landing-page-design-preset completion -->
                %s
                """.formatted(htmlWithGeneratedImages);
    }

    private void persistStageArtifactOnExperiment(GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        String stageCode = request.stageCode();
        if (!STAGE_WIREFRAME.equalsIgnoreCase(stageCode)
                && !STAGE_COPY.equalsIgnoreCase(stageCode)
                && !STAGE_IMAGE_PLANNING.equalsIgnoreCase(stageCode)
                && !STAGE_DESIGN_PRESET.equalsIgnoreCase(stageCode)
                && !STAGE_DELIVERABLES.equalsIgnoreCase(stageCode)) {
            return;
        }
        if (!StringUtils.hasText(request.modelResponse()) || StringUtils.hasText(request.errorMessage())) {
            return;
        }
        Experiment experiment = experimentRepository.findById(request.experimentId())
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + request.experimentId()));
        if (STAGE_WIREFRAME.equalsIgnoreCase(stageCode)) {
            experiment.setLandingPageWireframe(request.modelResponse());
            experiment.setLandingPageWireframeJobId(execution.getIdJob());
        } else if (STAGE_COPY.equalsIgnoreCase(stageCode)) {
            experiment.setLandingPageCopy(request.modelResponse());
            experiment.setLandingPageCopyJobId(execution.getIdJob());
        } else if (STAGE_IMAGE_PLANNING.equalsIgnoreCase(stageCode)) {
            experiment.setLandingPageImagePlanning(request.modelResponse());
            if (StringUtils.hasText(execution.getProvisionalHtml())) {
                experiment.setLandingPageHtml(execution.getProvisionalHtml());
            }
        } else if (STAGE_DELIVERABLES.equalsIgnoreCase(stageCode)) {
            experiment.setLandingPageDeliverables(request.modelResponse());
        } else {
            experiment.setLandingPageDesignPreset(request.modelResponse());
            String htmlFromDesignPreset = execution.getProvisionalHtml();
            if (!StringUtils.hasText(htmlFromDesignPreset)) {
                htmlFromDesignPreset = resolveDesignPresetProvisionalHtml(
                        fromDatabaseIdJob(execution.getIdJob()),
                        request,
                        execution);
            }
            if (StringUtils.hasText(htmlFromDesignPreset)) {
                experiment.setLandingPageHtml(htmlFromDesignPreset);
            }
        }
        experimentRepository.save(experiment);
    }

    @Transactional(readOnly = true)
    public List<GeraLandingExecutionSummaryResponse> listExperimentStageExecutions(
            Long experimentId,
            String stageCode,
            boolean includeCompleted) {
        List<GeraLandingStageExecution> executions = includeCompleted
                ? executionRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode)
                : executionRepository.findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
                        experimentId,
                        stageCode,
                        STATUS_COMPLETED);
        return executions
                .stream()
                .map(execution -> new GeraLandingExecutionSummaryResponse(
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStatus(),
                        execution.getExecutionRequestedAt(),
                        execution.getCostUsd()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GeraLandingStageExecutionDetailResponse getStageExecutionDetail(Long experimentId, String idJob) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(experimentId, toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));

        return new GeraLandingStageExecutionDetailResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getExperimentId(),
                execution.getStageCode(),
                execution.getExecutionRequestedAt(),
                execution.getCreatedAt(),
                execution.getProcessingStartedAt(),
                execution.getCompletedAt(),
                execution.getPromptTemplateId(),
                execution.getPromptContent(),
                execution.getPrompt(),
                execution.getOpenAiRequestBody(),
                execution.getOpenAiModel(),
                execution.getSchemaJson(),
                execution.getPromptMarkdownContent(),
                execution.getStatus(),
                execution.getOpenAiJobId(),
                execution.getModelResponse(),
                execution.getProvisionalHtml(),
                execution.getErrorMessage(),
                execution.getErrorDetail(),
                execution.getInputTokens(),
                execution.getOutputTokens(),
                execution.getCostUsd());
    }

    @Transactional(readOnly = true)
    public List<GeraLandingPendingExecutionResponse> listPendingExecutions() {
        return executionRepository.findTop20ByStatusInOrderByExecutionRequestedAtAsc(List.of(STATUS_STARTED, STATUS_WAITING_OPENAI_DISPATCH))
                .stream()
                        .map(execution -> new GeraLandingPendingExecutionResponse(
                                execution.getExperimentId(),
                                fromDatabaseIdJob(execution.getIdJob()),
                                execution.getStageCode()))
                .toList();
    }

    private byte[] toDatabaseIdJob(String idJob) {
        return idJob.getBytes(StandardCharsets.UTF_8);
    }

    private String fromDatabaseIdJob(byte[] idJob) {
        return new String(idJob, StandardCharsets.UTF_8);
    }
}
