package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.pipeline.service.LandingPageImageInjector;
import com.marketinghub.geralanding.copy.CopyProvisionalHtmlAssembler;
import com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlAssembler;
import com.marketinghub.geralanding.imageplanning.ImagePlanningProvisionalHtmlAssembler;
import com.marketinghub.geralanding.wireframe.WireframeProvisionalHtmlAssembler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
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
    private final ImagePlanningProvisionalHtmlAssembler imagePlanningProvisionalHtmlAssembler;
    private final DesignPresetProvisionalHtmlAssembler designPresetProvisionalHtmlAssembler;
    private final LandingPageImageInjector landingPageImageInjector;
    private final RestTemplate restTemplate;
    private final String leadPortalBaseUrl;

    public GeraLandingStageExecutionService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            WireframeProvisionalHtmlAssembler wireframeProvisionalHtmlAssembler,
            CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler,
            ImagePlanningProvisionalHtmlAssembler imagePlanningProvisionalHtmlAssembler,
            DesignPresetProvisionalHtmlAssembler designPresetProvisionalHtmlAssembler,
            LandingPageImageInjector landingPageImageInjector,
            RestTemplate restTemplate,
            @Value("${integrations.lead-portal.base-url:}") String leadPortalBaseUrl) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.wireframeProvisionalHtmlAssembler = wireframeProvisionalHtmlAssembler;
        this.copyProvisionalHtmlAssembler = copyProvisionalHtmlAssembler;
        this.imagePlanningProvisionalHtmlAssembler = imagePlanningProvisionalHtmlAssembler;
        this.designPresetProvisionalHtmlAssembler = designPresetProvisionalHtmlAssembler;
        this.landingPageImageInjector = landingPageImageInjector;
        this.restTemplate = restTemplate;
        this.leadPortalBaseUrl = leadPortalBaseUrl;
    }

    @Transactional
    public GeraLandingPublishResponse approveAndPublishLanding(Long experimentId) {
        log.info("GeraLanding publish approval started (experimentId={})", experimentId);
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        log.info("GeraLanding publish approval loaded experiment (experimentId={}, experimentName={})",
                experimentId, experiment.getName());

        String landingPageHtml = experiment.getHtmlGeraLanding();
        if (!StringUtils.hasText(landingPageHtml)) {
            landingPageHtml = experiment.getLandingPageHtml();
        }
        if (!StringUtils.hasText(landingPageHtml)) {
            log.warn("GeraLanding publish approval blocked because landing HTML is missing (experimentId={})",
                    experimentId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Landing HTML ainda não foi gerado para este experimento");
        }
        log.info("GeraLanding publish approval found landing HTML (experimentId={}, htmlLength={})",
                experimentId, landingPageHtml.length());

        String slug = "exp-" + experimentId + "-landing-geralanding";
        log.info("GeraLanding publish approval resolved slug (experimentId={}, slug={})", experimentId, slug);

        String htmlWithTracking = injectBehaviorTrackingAttributesAndScript(landingPageHtml);
        String htmlWithFunnelControls = injectFunnelControls(htmlWithTracking);
        log.info("GeraLanding publish approval injected funnel controls (experimentId={}, htmlLengthBefore={}, htmlLengthAfter={})",
                experimentId, landingPageHtml.length(), htmlWithFunnelControls.length());

        String facebookPixelId = resolveFacebookPixelId(experiment);
        log.info("GeraLanding publish approval resolved Facebook Pixel (experimentId={}, hasPixelId={})",
                experimentId, StringUtils.hasText(facebookPixelId));
        String htmlWithFacebookPixel = injectFacebookPixel(htmlWithFunnelControls, facebookPixelId);
        log.info("GeraLanding publish approval injected Facebook Pixel (experimentId={}, htmlLengthBefore={}, htmlLengthAfter={})",
                experimentId, htmlWithFunnelControls.length(), htmlWithFacebookPixel.length());

        log.info("GeraLanding publish approval sending flow to Lead Portal (experimentId={}, slug={}, htmlLength={})",
                experimentId, slug, htmlWithFacebookPixel.trim().length());
        publishToLeadPortal(slug, "Landing GeraLanding - Experimento " + experimentId, htmlWithFacebookPixel.trim());
        log.info("GeraLanding publish approval sent flow to Lead Portal successfully (experimentId={}, slug={})",
                experimentId, slug);
        log.info("GeraLanding publish approval resolving publication URLs (experimentId={}, slug={})", experimentId, slug);
        String iframeUrl = resolveIframeUrl(slug);
        String standaloneUrl = resolveStandaloneLandingUrl(iframeUrl);
        log.info("GeraLanding publish approval resolved publication URLs (experimentId={}, iframeUrl={}, standaloneUrl={})",
                experimentId, iframeUrl, standaloneUrl);

        experiment.setFollowUpActionUrl(standaloneUrl);
        experimentRepository.save(experiment);
        log.info("GeraLanding publish approval saved follow-up URL (experimentId={}, followUpActionUrl={})",
                experimentId, standaloneUrl);
        return new GeraLandingPublishResponse(experimentId, null, iframeUrl, standaloneUrl,
                "Landing publicada com sucesso pelo GeraLanding.");
    }

    private void publishToLeadPortal(String slug, String name, String html) {
        if (!StringUtils.hasText(leadPortalBaseUrl)) {
            log.warn("GeraLanding Lead Portal publication blocked because base URL is not configured (slug={})", slug);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lead Portal base URL não configurada");
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(leadPortalBaseUrl).path("/api/flows/{slug}").buildAndExpand(slug).toUri();
        log.info("GeraLanding Lead Portal publication request prepared (slug={}, uri={}, name={}, htmlLength={})",
                slug, uri, name, html == null ? 0 : html.length());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        GeraLandingLeadPortalPublishRequest payload = new GeraLandingLeadPortalPublishRequest(
                slug, name, "Fluxo publicado pelo módulo GeraLanding", html);
        log.info("GeraLanding Lead Portal publication request parameters (slug={}, uri={}, headers={}, payload={})",
                slug, uri, headers, payload);
        try {
            restTemplate.put(uri, new HttpEntity<>(payload, headers));
            log.info("GeraLanding Lead Portal publication request completed (slug={}, uri={})", slug, uri);
        } catch (RestClientException ex) {
            String rootCauseMessage = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
            log.error("GeraLanding Lead Portal publication request failed (slug={}, uri={}, rootCause={})",
                    slug, uri, rootCauseMessage, ex);
            throw new GeraLandingContractViolationException(
                    "publishToLeadPortal",
                    uri.toString(),
                    "Esperado: payload JSON contendo apenas slug, name, description e customFormHtml (HTML puro).",
                    payload.toString(),
                    rootCauseMessage,
                    ex);
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

        String copyStageHtml = imagePlanningProvisionalHtmlAssembler.assemble(
                experimentId,
                experiment.getLandingPageCopy(),
                experiment.getLandingPageWireframe(),
                jobId);
        String htmlWithGeneratedImages = copyStageHtml;
        String enrichedImagePlanning = landingPageImageInjector.injectImageUrlsIntoPlanning(
                experimentId,
                experiment.getLandingPageImagePlanning());
        String provisionalHtml = htmlWithGeneratedImages;

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
        try {
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
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao processar receive-result do GeraLanding (idJob={}, experimentId={}, stageCode={}, openAiJobId={}, modelResponseLength={}, modelResponsePreview={}, provisionalHtmlLength={}, provisionalHtmlPreview={})",
                    idJob,
                    request.experimentId(),
                    request.stageCode(),
                    request.openAiJobId(),
                    safeLength(request.modelResponse()),
                    safePreview(request.modelResponse()),
                    safeLength(request.provisionalHtml()),
                    safePreview(request.provisionalHtml()),
                    ex);
            throw ex;
        }
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
            return designPresetProvisionalHtmlAssembler.assemble(request.modelResponse(), idJob);
        }
        return null;
    }

    /**
     * Retorna preview curto para diagnóstico sem poluir logs com payload completo.
     */
    private String safePreview(String payload) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }
        return payload.substring(0, Math.min(payload.length(), 200)).replaceAll("\\s+", " ");
    }

    /**
     * Retorna o comprimento do texto para investigação rápida de truncamento/ausência.
     */
    private int safeLength(String payload) {
        return payload == null ? 0 : payload.length();
    }

    private String resolveImagePlanningProvisionalHtml(String idJob, GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        Experiment experiment = execution.getExperiment();
        if (experiment == null && request.experimentId() != null) {
            experiment = experimentRepository.findById(request.experimentId()).orElse(null);
        }
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe()) || !StringUtils.hasText(experiment.getLandingPageCopy())) {
            return null;
        }
        String baseHtml = imagePlanningProvisionalHtmlAssembler.assemble(
                experiment.getId(),
                experiment.getLandingPageCopy(),
                experiment.getLandingPageWireframe(),
                idJob);
        if (!StringUtils.hasText(baseHtml)) {
            return null;
        }
        return baseHtml;
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
            String htmlFromDesignPreset = execution.getProvisionalHtml();
            if (!StringUtils.hasText(htmlFromDesignPreset)) {
                htmlFromDesignPreset = designPresetProvisionalHtmlAssembler.assemble(
                        request.modelResponse(),
                        fromDatabaseIdJob(execution.getIdJob()));
            }
            experiment.setLandingPageDesignPreset(request.modelResponse());
            if (StringUtils.hasText(htmlFromDesignPreset)) {
                experiment.setHtmlGeraLanding(htmlFromDesignPreset);
                experiment.setLandingPageHtml(htmlFromDesignPreset);
            }
        }
        experimentRepository.save(experiment);
    }

    private String injectBehaviorTrackingAttributesAndScript(String html) {
        if (!StringUtils.hasText(html) || html.contains("data-mh-funnel-tracking")) {
            return html;
        }
        Document document = Jsoup.parse(html, "", Parser.htmlParser());
        document.outputSettings().prettyPrint(false);

        for (Element section : document.select("section[data-section-id], section[id], [data-section-id]")) {
            String sectionId = section.hasAttr("data-section-id") ? section.attr("data-section-id") : section.id();
            if (!StringUtils.hasText(sectionId)) {
                continue;
            }
            section.attr("data-track-section", sectionId.trim());
        }

        String script = """
                <script data-mh-funnel-tracking="true">
                (function(){
                  if (window.__mhFunnelTrackingInstalled) return;
                  window.__mhFunnelTrackingInstalled = true;
                  var debugPrefix = '[MH funnel tracking]';
                  window.dataLayer = window.dataLayer || [];

                  function emit(name, payload){
                    var eventPayload = Object.assign({event:name, source:'landing-page-design-preset'}, payload||{});
                    console.debug(debugPrefix, 'emit', eventPayload);
                    window.dataLayer.push(eventPayload);
                  }

                  function initTracking(){
                    console.debug(debugPrefix, 'bootstrap');
                    emit('page_view', {ts: Date.now()});
                    var sections = Array.prototype.slice.call(document.querySelectorAll('[data-track-section]'));
                    console.debug(debugPrefix, 'sections-found', {count: sections.length});
                    var stats = {};
                    sections.forEach(function(node){
                      var id = node.getAttribute('data-track-section');
                      stats[id] = {visibleSince:null, elapsedMs:0};
                    });
                    function flushSection(id, reason){
                      var s = stats[id];
                      if (!s || s.visibleSince === null) return;
                      s.elapsedMs += Date.now() - s.visibleSince;
                      s.visibleSince = null;
                      console.debug(debugPrefix, 'section-flush', {sectionId:id, elapsedMs:s.elapsedMs, reason: reason || 'hidden'});
                      emit('section_view_time', {sectionId:id, elapsedMs:s.elapsedMs, reason: reason || 'hidden'});
                    }
                    var observer = new IntersectionObserver(function(entries){
                      entries.forEach(function(entry){
                        var id = entry.target.getAttribute('data-track-section');
                        if (!id || !stats[id]) return;
                        if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
                          if (stats[id].visibleSince === null) {
                            stats[id].visibleSince = Date.now();
                            console.debug(debugPrefix, 'section-visible', {sectionId:id, ts:stats[id].visibleSince});
                            emit('section_view_start', {sectionId:id});
                          }
                        } else {
                          flushSection(id, 'intersection-change');
                        }
                      });
                    }, {threshold:[0,0.5,1]});
                    sections.forEach(function(node){ observer.observe(node); });
                    document.addEventListener('visibilitychange', function(){
                      if (document.hidden) {
                        console.debug(debugPrefix, 'visibility-hidden');
                        Object.keys(stats).forEach(function(id){ flushSection(id, 'tab-hidden'); });
                      }
                    });
                    window.addEventListener('beforeunload', function(){
                      console.debug(debugPrefix, 'beforeunload-flush');
                      Object.keys(stats).forEach(function(id){ flushSection(id, 'before-unload'); });
                    });
                  }

                  if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', initTracking);
                  } else {
                    initTracking();
                  }
                })();
                </script>
                """;
        if (document.head() != null) {
            document.head().append(script);
        } else {
            document.prepend(script);
        }
        return document.outerHtml();
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
