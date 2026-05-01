package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStage;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest;
import com.marketinghub.experiment.pipeline.dto.LandingPagePublicationResultDto;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobCompletionRequest;
import com.marketinghub.experiment.pipeline.lhm.LandingHtmlModule;
import com.marketinghub.experiment.pipeline.repository.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.cost.CostAttributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ExperimentPipelineGenerationServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private ExperimentPipelineGenerationJobRepository jobRepository;
    @Mock
    private ExperimentMapper experimentMapper;
    @Mock
    private AiWorkerGenerationService generationService;
    @Mock
    private LeadPortalFlowRepository leadPortalFlowRepository;
    @Mock
    private LeadPortalFlowPublisher leadPortalFlowPublisher;
    @Mock
    private LandingPageImageInjector landingPageImageInjector;
    @Mock
    private LandingHtmlModule landingHtmlModule;
    @Mock
    private FrameworkImageGenerationService frameworkImageGenerationService;
    @Mock
    private OpenAiPricingService openAiPricingService;
    @Mock
    private LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;
    @Mock
    private CostAttributionService costAttributionService;

    private ExperimentPipelineGenerationService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentPipelineGenerationService(
                experimentRepository,
                jobRepository,
                experimentMapper,
                generationService,
                leadPortalFlowRepository,
                leadPortalFlowPublisher,
                new ObjectMapper(),
                landingPageImageInjector,
                landingHtmlModule,
                frameworkImageGenerationService,
                openAiPricingService,
                leadPortalPublicUrlResolver,
                costAttributionService);
        lenient().when(landingHtmlModule.buildPromptV2Inputs(any())).thenReturn("");
        lenient().when(landingHtmlModule.assembleHtmlDocument(any())).thenReturn("<!doctype html><html><body></body></html>");
        lenient().when(openAiPricingService.estimateStandardCost(any(), any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void applyLandingHtmlToLeadPortalFormCreatesFlowWhenExperimentHasNoLinkedFlow() {
        Experiment experiment = new Experiment();
        experiment.setId(10L);
        experiment.setLandingPageHtml("<form>landing</form>");
        experiment.setNiche(MarketNiche.builder().id(99L).build());

        when(experimentRepository.findById(10L)).thenReturn(Optional.of(experiment));
        when(leadPortalFlowRepository.findBySlug("exp-10-landing")).thenReturn(Optional.empty());
        when(leadPortalFlowRepository.save(any(LeadPortalFlow.class))).thenAnswer(invocation -> {
            LeadPortalFlow flow = invocation.getArgument(0);
            if (flow.getId() == null) {
                flow.setId(500L);
            }
            return flow;
        });

        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExperimentDto dto = new ExperimentDto();
        dto.setId(10L);
        when(experimentMapper.toDto(experiment)).thenReturn(dto);
        when(landingPageImageInjector.injectImages(10L, "<form>landing</form>")).thenReturn("<form>landing</form>");

        ExperimentDto result = service.applyLandingHtmlToLeadPortalForm(10L);

        assertEquals(10L, result.getId());
        assertNotNull(experiment.getLeadPortalFlow());
        assertEquals(500L, experiment.getLeadPortalFlow().getId());
        assertEquals("<form>landing</form>", experiment.getLeadPortalFlow().getCustomFormHtml());

        ArgumentCaptor<LeadPortalFlow> captor = ArgumentCaptor.forClass(LeadPortalFlow.class);
        verify(leadPortalFlowRepository, atLeastOnce()).save(captor.capture());
        LeadPortalFlow created = captor.getAllValues().get(0);
        assertEquals("Landing - Experimento 10", created.getName());
        assertEquals("exp-10-landing", created.getSlug());
        assertEquals("gpt-5.2", created.getModel());
        assertEquals("Pipeline: landing-page-html/apply-to-form", created.getPrompt());
        assertTrue(created.isApproved());
        assertNotNull(created.getApprovedAt());
        verify(leadPortalFlowPublisher).publish(any(LeadPortalFlow.class));
    }

    @Test
    void generateLandingHtmlWithLhmUsesModuleAndPersistsLandingHtml() {
        Experiment experiment = new Experiment();
        experiment.setId(31L);
        experiment.setLandingPageWireframe("{\"landingPageWireframe\":{\"formSpec\":{\"formId\":\"lead-capture-primary\",\"submitTarget\":\"/api/flows/exp-31/submissions\",\"fields\":[{\"name\":\"nome\",\"type\":\"text\",\"required\":true}],\"submitLabel\":\"Enviar\"},\"sectionOrder\":[{\"sectionId\":\"hero\",\"sectionName\":\"Hero\",\"contentType\":\"form\",\"surfaceSpec\":{\"surfaceToken\":\"surface-base\",\"style\":\"band\",\"contrastMode\":\"normal\"}}]}}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":{\"headline\":\"Headline\"}}");
        experiment.setLandingPageDesignPreset("{\"landingPageDesignPreset\":{\"presetId\":\"preset-31\"}}");
        experiment.setLandingPageImagePlanning("{\"landingPageImagePlanning\":{\"images\":[]}}");

        when(experimentRepository.findById(31L)).thenReturn(Optional.of(experiment));
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landingHtmlModule.assembleHtmlDocument(experiment)).thenReturn("""
                <!doctype html><html><body>
                <section data-section-id="hero" data-surface-token="surface-base" data-surface-style="band" data-surface-contrast="normal">
                  <form id="lead-capture-primary" method="post" action="/api/flows/exp-31/submissions">
                    <input type="text" name="nome" required />
                    <button type="submit">Enviar</button>
                    <p id="success-message">success</p>
                  </form>
                </section>
                <script>
                form.addEventListener('submit', async (event) => { event.preventDefault(); form.checkValidity(); form.reportValidity(); submitButton.disabled = true; await fetch(form.action, { method: form.method.toUpperCase(), body: new FormData(form) }); submitButton.disabled = false; });
                </script>
                </body></html>
                """);
        when(experimentMapper.toDto(experiment)).thenReturn(new ExperimentDto());

        service.generateLandingHtmlWithLhm(31L);

        verify(landingHtmlModule).assembleHtmlDocument(experiment);
        assertTrue(experiment.getLandingPageHtml().contains("<!doctype html>"));
        verify(jobRepository).save(any(ExperimentPipelineGenerationJob.class));
        verify(generationService).recordGeneration(any(AiWorkerGenerationRequest.class));

        ArgumentCaptor<ExperimentPipelineGenerationJob> jobCaptor = ArgumentCaptor.forClass(ExperimentPipelineGenerationJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        ExperimentPipelineGenerationJob job = jobCaptor.getValue();
        assertEquals(ExperimentPipelineGenerationJobStatus.COMPLETED, job.getStatus());
        assertEquals(ExperimentPipelineGenerationJobStage.COMPLETED, job.getStage());
        assertEquals("LHM", job.getModel());
        assertEquals("lhm-inline", job.getWorkerId());
        assertNotNull(job.getRequestBodyJson());
        assertTrue(job.getRequestBodyJson().contains("\"designPresetPresent\":true"));
        assertNotNull(job.getResponseContent());
        assertNotNull(job.getFinishedAt());
    }

    @Test
    void generateLandingHtmlWithLhmFailsWhenWireframeMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(32L);
        experiment.setLandingPageCopy("{\"landingPageCopy\":{\"headline\":\"ok\"}}");
        when(experimentRepository.findById(32L)).thenReturn(Optional.of(experiment));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generateLandingHtmlWithLhm(32L));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getReason().contains("Wireframe"));
    }

    @Test
    void generateLandingHtmlWithLhmInjectsCanonicalFormWhenHtmlHasNoForm() {
        Experiment experiment = new Experiment();
        experiment.setId(35L);
        experiment.setLandingPageWireframe("{\"landingPageWireframe\":{\"formSpec\":{\"formId\":\"lead-capture-primary\",\"submitTarget\":\"/api/flows/exp-35/submissions\",\"fields\":[{\"name\":\"nome\",\"type\":\"text\",\"required\":true},{\"name\":\"email\",\"type\":\"email\",\"required\":true}],\"submitLabel\":\"Enviar\"},\"sectionOrder\":[{\"sectionId\":\"hero\",\"sectionName\":\"Hero\",\"contentType\":\"form\",\"surfaceSpec\":{\"surfaceToken\":\"surface-base\",\"style\":\"band\",\"contrastMode\":\"normal\"}}]}}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":{\"headline\":\"Headline\"}}");
        experiment.setLandingPageDesignPreset("{\"landingPageDesignPreset\":{\"presetId\":\"preset-35\"}}");
        experiment.setLandingPageImagePlanning("{\"landingPageImagePlanning\":{\"images\":[]}}");

        when(experimentRepository.findById(35L)).thenReturn(Optional.of(experiment));
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landingHtmlModule.assembleHtmlDocument(experiment)).thenReturn("""
                <!doctype html><html><body>
                <main>
                  <section data-section-id="hero" data-surface-token="surface-base" data-surface-style="band" data-surface-contrast="normal">
                    <h1>Hero</h1>
                  </section>
                </main>
                </body></html>
                """);
        when(experimentMapper.toDto(experiment)).thenReturn(new ExperimentDto());

        service.generateLandingHtmlWithLhm(35L);

        assertTrue(experiment.getLandingPageHtml().contains("form id=\"lead-capture-primary\""));
        assertTrue(experiment.getLandingPageHtml().contains("name=\"nome\""));
        assertTrue(experiment.getLandingPageHtml().contains("name=\"email\""));
        verify(generationService).recordGeneration(any(AiWorkerGenerationRequest.class));
    }

    @Test
    void generateLandingHtmlWithLhmTracksFailedAttemptWhenPromptPreparationFails() {
        Experiment experiment = new Experiment();
        experiment.setId(34L);
        experiment.setLandingPageWireframe("{\"landingPageWireframe\":{\"formSpec\":{\"formId\":\"lead-capture-primary\"}}}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":{\"headline\":\"Headline\"}}");
        experiment.setLandingPageDesignPreset("{\"landingPageDesignPreset\":{\"presetId\":\"preset-34\"}}");

        when(experimentRepository.findById(34L)).thenReturn(Optional.of(experiment));
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landingHtmlModule.buildPromptV2Inputs(experiment))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Validação canônica falhou"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generateLandingHtmlWithLhm(34L));

        assertEquals(422, error.getStatusCode().value());
        verify(jobRepository).save(any(ExperimentPipelineGenerationJob.class));
        verify(landingHtmlModule, never()).assembleHtmlDocument(experiment);

        ArgumentCaptor<ExperimentPipelineGenerationJob> jobCaptor = ArgumentCaptor.forClass(ExperimentPipelineGenerationJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        ExperimentPipelineGenerationJob job = jobCaptor.getValue();
        assertEquals(ExperimentPipelineGenerationJobStatus.FAILED, job.getStatus());
        assertEquals(ExperimentPipelineGenerationJobStage.FAILED, job.getStage());
        assertEquals("LHM", job.getModel());
        assertTrue(job.getErrorMessage().contains("Validação canônica falhou"));
        assertNotNull(job.getFinishedAt());
    }

    @Test
    void generateLandingHtmlWithLhmTracksFailedAttemptWhenValidationFails() {
        Experiment experiment = new Experiment();
        experiment.setId(33L);
        experiment.setLandingPageWireframe("{\"landingPageWireframe\":{\"formSpec\":{\"formId\":\"lead-capture-primary\",\"submitTarget\":\"/api/flows/exp-33/submissions\",\"fields\":[{\"name\":\"nome\",\"type\":\"text\",\"required\":true}],\"submitLabel\":\"Enviar\"},\"sectionOrder\":[{\"sectionId\":\"hero\",\"sectionName\":\"Hero\",\"contentType\":\"form\",\"surfaceSpec\":{\"surfaceToken\":\"surface-base\",\"style\":\"band\",\"contrastMode\":\"normal\"}}]}}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":{\"headline\":\"Headline\"}}");
        experiment.setLandingPageDesignPreset("{\"landingPageDesignPreset\":{\"presetId\":\"preset-33\"}}");
        when(experimentRepository.findById(33L)).thenReturn(Optional.of(experiment));
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landingHtmlModule.assembleHtmlDocument(experiment))
                .thenReturn("<!doctype html><html><body><form id=\"lead-capture-primary\" method=\"post\" action=\"/api/flows/exp-33/submissions\"></form></body></html>");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generateLandingHtmlWithLhm(33L));

        assertEquals(422, error.getStatusCode().value());
        verify(jobRepository).save(any(ExperimentPipelineGenerationJob.class));
        verify(generationService, never()).recordGeneration(any(AiWorkerGenerationRequest.class));

        ArgumentCaptor<ExperimentPipelineGenerationJob> jobCaptor = ArgumentCaptor.forClass(ExperimentPipelineGenerationJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        ExperimentPipelineGenerationJob job = jobCaptor.getValue();
        assertEquals(ExperimentPipelineGenerationJobStatus.FAILED, job.getStatus());
        assertEquals(ExperimentPipelineGenerationJobStage.FAILED, job.getStage());
        assertTrue(job.getErrorMessage().contains("Divergência"));
        assertNotNull(job.getFinishedAt());
    }

    @Test
    void generateLandingHtmlWithLhmFailsWhenDesignPresetMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(36L);
        experiment.setLandingPageWireframe("{\"landingPageWireframe\":{\"formSpec\":{\"formId\":\"lead-capture-primary\"}}}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":{\"headline\":\"Headline\"}}");
        when(experimentRepository.findById(36L)).thenReturn(Optional.of(experiment));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generateLandingHtmlWithLhm(36L));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getReason().contains("Preset de design"));
    }

    @Test
    void applyLandingHtmlToLeadPortalFormUpdatesExistingFlowWithoutCreatingAnother() {
        LeadPortalFlow linkedFlow = new LeadPortalFlow();
        linkedFlow.setId(900L);
        linkedFlow.setSlug("existing-flow");

        Experiment experiment = new Experiment();
        experiment.setId(11L);
        experiment.setLandingPageHtml("<section>ok</section>");
        experiment.setLeadPortalFlow(linkedFlow);

        when(experimentRepository.findById(11L)).thenReturn(Optional.of(experiment));
        when(leadPortalFlowRepository.findById(900L)).thenReturn(Optional.of(linkedFlow));
        when(leadPortalFlowRepository.save(any(LeadPortalFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentMapper.toDto(experiment)).thenReturn(new ExperimentDto());
        when(landingPageImageInjector.injectImages(11L, "<section>ok</section>")).thenReturn("<section>ok</section>");

        service.applyLandingHtmlToLeadPortalForm(11L);

        verify(leadPortalFlowRepository, never()).findBySlug(any());
        verify(experimentRepository, never()).save(any());
        verify(leadPortalFlowPublisher, never()).publish(any());
        assertEquals("<section>ok</section>", linkedFlow.getCustomFormHtml());
    }

    @Test
    void applyLandingHtmlToLeadPortalFormReturnsRootCauseWhenPublicationFails() {
        LeadPortalFlow linkedFlow = new LeadPortalFlow();
        linkedFlow.setId(901L);
        linkedFlow.setSlug("existing-flow");
        linkedFlow.setApproved(true);

        Experiment experiment = new Experiment();
        experiment.setId(15L);
        experiment.setLandingPageHtml("<section>ok</section>");
        experiment.setLeadPortalFlow(linkedFlow);

        when(experimentRepository.findById(15L)).thenReturn(Optional.of(experiment));
        when(leadPortalFlowRepository.findById(901L)).thenReturn(Optional.of(linkedFlow));
        when(leadPortalFlowRepository.save(any(LeadPortalFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landingPageImageInjector.injectImages(15L, "<section>ok</section>")).thenReturn("<section>ok</section>");
        doThrow(new LeadPortalPublicationException("failed", new RuntimeException("Connection refused")))
                .when(leadPortalFlowPublisher).publish(any(LeadPortalFlow.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.applyLandingHtmlToLeadPortalForm(15L));

        assertEquals(502, error.getStatusCode().value());
        assertTrue(error.getReason().contains("Connection refused"));
    }

    @Test
    void approveAndPublishLandingPageReturnsPublicationFeedback() {
        LeadPortalFlow iaFlow = new LeadPortalFlow();
        iaFlow.setId(902L);
        iaFlow.setSlug("exp-22-landing-gerar-com-ia");
        iaFlow.setApproved(false);

        LeadPortalFlow lhmFlow = new LeadPortalFlow();
        lhmFlow.setId(903L);
        lhmFlow.setSlug("exp-22-landing-lhm");
        lhmFlow.setApproved(false);

        MarketNiche niche = new MarketNiche();
        niche.setFacebookPixelId("pixel-abc");
        iaFlow.setMarketNiche(niche);
        lhmFlow.setMarketNiche(niche);

        Experiment experiment = new Experiment();
        experiment.setId(22L);
        experiment.setNiche(niche);
        experiment.setLandingPageHtml("<section>ia</section>");

        ExperimentPipelineGenerationJob lhmJob = new ExperimentPipelineGenerationJob();
        lhmJob.setResponseContent("<section>lhm</section>");
        when(experimentRepository.findById(22L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findTopByExperimentIdAndSectionAndStatusAndModelOrderByCreatedAtDesc(
                22L,
                ExperimentPipelineSection.LANDING_PAGE_HTML,
                ExperimentPipelineGenerationJobStatus.COMPLETED,
                "LHM")).thenReturn(Optional.of(lhmJob));
        when(leadPortalFlowRepository.findBySlug("exp-22-landing-gerar-com-ia")).thenReturn(Optional.of(iaFlow));
        when(leadPortalFlowRepository.findBySlug("exp-22-landing-lhm")).thenReturn(Optional.of(lhmFlow));
        when(leadPortalFlowRepository.save(any(LeadPortalFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landingPageImageInjector.injectImages(22L, "<section>ia</section>")).thenReturn("<section>ia</section>");
        when(landingPageImageInjector.injectImages(22L, "<section>lhm</section>")).thenReturn("<section>lhm</section>");
        when(leadPortalPublicUrlResolver.resolve(iaFlow)).thenReturn("https://lead.portal/flows/exp-22-landing-gerar-com-ia");
        when(leadPortalPublicUrlResolver.resolve(lhmFlow)).thenReturn("https://lead.portal/flows/exp-22-landing-lhm");

        LandingPagePublicationResultDto result = service.approveAndPublishLandingPage(22L);

        assertEquals(22L, result.experimentId());
        assertTrue(result.approved());
        assertTrue(result.published());
        assertTrue(result.pixelAppliedAutomatically());
        assertEquals("pixel-abc", result.facebookPixelId());
        assertEquals("https://lead.portal/flows/exp-22-landing-gerar-com-ia", result.publicUrl());
        assertEquals(2, result.variantLinks().size());
        assertEquals("Gerar com IA", result.variantLinks().get(0).variant());
        assertEquals("https://lead.portal/api/flows/exp-22-landing-gerar-com-ia/page", result.variantLinks().get(0).standaloneUrl());
        assertEquals("LHM", result.variantLinks().get(1).variant());
        assertEquals("https://lead.portal/api/flows/exp-22-landing-lhm/page", result.variantLinks().get(1).standaloneUrl());
        verify(leadPortalFlowPublisher, times(2)).publish(any(LeadPortalFlow.class));
    }

    @Test
    void generateWireframeKeepsPromptScopedToPredecessorChain() {
        Experiment experiment = new Experiment();
        experiment.setId(12L);
        experiment.setName("Experimento 12");
        experiment.setAdImageBriefing("{\"adImageBriefing\":\"predecessor-ready\"}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":\"predecessor-ready\"}");

        when(experimentRepository.findById(12L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdAndStatusInOrderByCreatedAtDesc(
                12L,
                Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING)))
                .thenReturn(List.of());
        when(jobRepository.findLatestCompletedPerSectionByExperimentId(12L, null))
                .thenReturn(List.of(
                        completedJob(experiment, ExperimentPipelineSection.CAMPAIGN_ANGLE, "{\"campaignAngle\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.AD_COPY, "{\"adCopy\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.AD_IMAGE_BRIEFING, "{\"adImageBriefing\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_COPY, "{\"landingPageCopy\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME, "{\"landingPageWireframe\":\"legacy\"}"),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING, "{\"landingPageImagePlanning\":\"legacy\"}")));
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentMapper.toDto(experiment)).thenReturn(new ExperimentDto());

        service.generate(12L, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME, new ExperimentPipelineGenerationRequest());

        verify(jobRepository).save(argThat(job -> {
            String prompt = job.getPrompt();
            return prompt != null
                    && prompt.contains("Ângulo da campanha:")
                    && prompt.contains("Texto do anúncio:")
                    && prompt.contains("Briefing da imagem:")
                    && !prompt.contains("Textos da landing:")
                    && !prompt.contains("Wireframe da landing:")
                    && !prompt.contains("Planejamento de imagens da landing:");
        }));
    }

    @Test
    void generateWireframeDoesNotReusePersistedExperimentArtifactsWithoutCompletedJobs() {
        Experiment experiment = new Experiment();
        experiment.setId(13L);
        experiment.setName("Experimento 13");
        experiment.setAdImageBriefing("{\"adImageBriefing\":\"persisted\"}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":\"persisted\"}");
        experiment.setLandingPageWireframe("{\"landingPageWireframe\":\"persisted\"}");
        experiment.setLandingPageImagePlanning("{\"landingPageImagePlanning\":\"persisted\"}");

        when(experimentRepository.findById(13L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdAndStatusInOrderByCreatedAtDesc(
                13L,
                Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING)))
                .thenReturn(List.of());
        when(jobRepository.findLatestCompletedPerSectionByExperimentId(13L, null)).thenReturn(List.of());
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentMapper.toDto(experiment)).thenReturn(new ExperimentDto());

        service.generate(13L, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME, new ExperimentPipelineGenerationRequest());

        verify(jobRepository).save(argThat(job -> {
            String prompt = job.getPrompt();
            return prompt != null
                    && !prompt.contains("Textos da landing:")
                    && !prompt.contains("Wireframe da landing:")
                    && !prompt.contains("Planejamento de imagens da landing:");
        }));
    }

    @Test
    void generateImagePlanningUsesWorkerAiTemplateWithoutHardcodedChecklistInPrompt() {
        Experiment experiment = new Experiment();
        experiment.setId(131L);
        experiment.setName("Experimento 131");
        experiment.setCampaignAngle("{\"campaignAngle\":\"angulo\"}");
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {"sectionId":"hero"},
                      {"sectionId":"objection-anti-preco-pratica"},
                      {"sectionId":"faq-objections"}
                    ]
                  }
                }
                """);

        when(experimentRepository.findById(131L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdAndStatusInOrderByCreatedAtDesc(
                131L,
                Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING)))
                .thenReturn(List.of());
        when(jobRepository.findLatestCompletedPerSectionByExperimentId(131L, null))
                .thenReturn(List.of(
                        completedJob(experiment, ExperimentPipelineSection.CAMPAIGN_ANGLE, "{\"campaignAngle\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.AD_COPY, "{\"adCopy\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.AD_IMAGE_BRIEFING, "{\"adImageBriefing\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_COPY, "{\"landingPageCopy\":true}"),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME, experiment.getLandingPageWireframe())));
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentMapper.toDto(experiment)).thenReturn(new ExperimentDto());

        service.generate(131L, ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING, new ExperimentPipelineGenerationRequest());

        verify(jobRepository).save(argThat(job -> {
            String prompt = job.getPrompt();
            return prompt != null
                    && prompt.contains("Template do prompt desta etapa é mantido exclusivamente no módulo Worker AI.")
                    && prompt.contains("Não gerar instruções hard-coded no backend para seção: landing-page-image-planning.")
                    && !prompt.contains("Checklist de cobertura obrigatório");
        }));
    }

    @Test
    void generateLandingDesignPresetIncludesMinimumCanonicalTokensInJsonSchema() {
        Experiment experiment = new Experiment();
        experiment.setId(132L);
        experiment.setName("Experimento 132");
        experiment.setCampaignAngle("{\"campaignAngle\":true}");
        experiment.setAdCopy("{\"adCopy\":true}");
        experiment.setAdImageBriefing("{\"adImageBriefing\":true}");
        experiment.setLandingPageCopy("{\"landingPageCopy\":true}");
        experiment.setLandingPageWireframe("{\"landingPageWireframe\":{\"sectionOrder\":[{\"sectionId\":\"hero\"}]}}");
        experiment.setLandingPageImagePlanning("{\"landingPageImagePlanning\":{\"images\":[{\"sectionId\":\"hero\",\"imagePrompt\":\"x\"}]}}");

        when(experimentRepository.findById(132L)).thenReturn(Optional.of(experiment));
        when(frameworkImageGenerationService.allPlanningImagesCompleted(132L)).thenReturn(true);
        when(jobRepository.findByExperimentIdAndStatusInOrderByCreatedAtDesc(
                132L,
                Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING)))
                .thenReturn(List.of());
        when(jobRepository.findLatestCompletedPerSectionByExperimentId(132L, null))
                .thenReturn(List.of(
                        completedJob(experiment, ExperimentPipelineSection.CAMPAIGN_ANGLE, experiment.getCampaignAngle()),
                        completedJob(experiment, ExperimentPipelineSection.AD_COPY, experiment.getAdCopy()),
                        completedJob(experiment, ExperimentPipelineSection.AD_IMAGE_BRIEFING, experiment.getAdImageBriefing()),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_COPY, experiment.getLandingPageCopy()),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME, experiment.getLandingPageWireframe()),
                        completedJob(experiment, ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING, experiment.getLandingPageImagePlanning())));
        when(jobRepository.save(any(ExperimentPipelineGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentMapper.toDto(experiment)).thenReturn(new ExperimentDto());

        service.generate(132L, ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET, new ExperimentPipelineGenerationRequest());

        verify(jobRepository).save(argThat(job -> {
            String requestBodyJson = job.getRequestBodyJson();
            return requestBodyJson != null
                    && requestBodyJson.contains("\"sectionGapMobile\"")
                    && requestBodyJson.contains("\"textContrastBody\"")
                    && requestBodyJson.contains("\"showIdentity\"")
                    && requestBodyJson.contains("\"showLegalFooter\"")
                    && requestBodyJson.contains("\"stickyMobile\"")
                    && requestBodyJson.contains("\"ctaPrimary\"");
        }));
    }

    @Test
    void generateRejectsNewStageWhenAnotherStageIsAlreadyActiveForExperiment() {
        Experiment experiment = new Experiment();
        experiment.setId(14L);
        experiment.setCampaignAngle("{\"campaignAngle\":\"ok\"}");

        ExperimentPipelineGenerationJob activeJob = ExperimentPipelineGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(experiment)
                .section(ExperimentPipelineSection.CAMPAIGN_ANGLE)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI)
                .build();

        when(experimentRepository.findById(14L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdAndStatusInOrderByCreatedAtDesc(
                14L,
                Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING)))
                .thenReturn(List.of(activeJob));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(14L, ExperimentPipelineSection.AD_COPY, new ExperimentPipelineGenerationRequest()));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getReason().contains("ordem sequencial"));
        verify(jobRepository, never()).save(any(ExperimentPipelineGenerationJob.class));
    }

    @Test
    void generateLandingHtmlFailsEarlyWhenWireframeAndDesignPresetAreInconsistent() {
        Experiment experiment = new Experiment();
        experiment.setId(88L);
        experiment.setLandingPageDesignPreset("""
                {
                  "landingPageDesignPreset": {
                    "presetId": "preset-88",
                    "lhmRuntime": {"baseCss":".x{}","cssVersion":"v1","cssNotes":"baseline"},
                    "theme": {
                      "palette": {
                        "background":"#ffffff","surface":"#ffffff","textPrimary":"#111827","textMuted":"#6b7280","brandPrimary":"#2563eb","brandSecondary":"#1d4ed8","border":"#e5e7eb"
                      },
                      "typography": {"maxLineLength":"65ch","lineHeightBody":"1.6"},
                      "spacing": {"sectionGapMobile":"56px"},
                      "accessibility": {"textContrastBody":"4.5:1"}
                    },
                    "sectionPresets": [
                      {"sectionId":"hero","surfaceStyle":"solid","contrastMode":"high","layoutPreset":"hero-focus","emphasis":"primary","notes":"hero"}
                    ],
                    "componentPresets": {
                      "cta": {"stickyMobile":true},
                      "trust": {"showLegalFooter":true},
                      "proof": {"showIdentity": true}
                    },
                    "motion": {"enabled": false, "intensity": "none"},
                    "consistencyChecks": [
                      {"check":"THEME_CONTRAST","status":"PASS"},
                      {"check":"CTA_VISUAL_HIERARCHY","status":"PASS"},
                      {"check":"MOBILE_READABILITY","status":"PASS"}
                    ]
                  }
                }
                """);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "readingFlowSpec": {"maxParagraphLinesMobile": 4, "bulletDensityPerSection": 3},
                    "conversionPathSpec": {"primaryAction": "Desbloquear o Kit", "ctaLabelCanonical": "Desbloquear o Kit"},
                    "proofPlan": {"requiredProofTypes": ["social-proof","metric-proof"], "proofSectionIds": ["proof"]},
                    "trustSignalsSpec": {"brandIdentityRequired": true, "privacyNoticeNearForm": true, "privacyPolicyUrl": "https://example.com/privacy", "legalFooterItems": ["empresa","contato","política de privacidade"]},
                    "accessibilitySpec": {"minTextContrast":"4.5:1","minTouchTargetPx":44,"formFieldMinHeightPx":44},
                    "sectionOrder": [
                      {"sectionId":"hero","dropOffRisk":"medio","mobilePriorityScore":8,"surfaceSpec":{"surfaceToken":"surface-base","notes":"hero"}},
                      {"sectionId":"proof","dropOffRisk":"medio","mobilePriorityScore":7,"surfaceSpec":{"surfaceToken":"surface-alt-1","notes":"proof"}}
                    ]
                  }
                }
                """);

        when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
        when(frameworkImageGenerationService.allPlanningImagesCompleted(88L)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(88L, ExperimentPipelineSection.LANDING_PAGE_HTML, new ExperimentPipelineGenerationRequest()));

        assertEquals(409, error.getStatusCode().value());
        verify(jobRepository, never()).save(any(ExperimentPipelineGenerationJob.class));
    }

    @Test
    void listPendingJobsReturnsOnlyEligibleExperimentStatuses() {
        Experiment plannedExperiment = new Experiment();
        plannedExperiment.setId(31L);
        plannedExperiment.setStatus(ExperimentStatus.PLANNED);
        ExperimentPipelineGenerationJob pendingFromPlanned = ExperimentPipelineGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(plannedExperiment)
                .section(ExperimentPipelineSection.CAMPAIGN_ANGLE)
                .status(ExperimentPipelineGenerationJobStatus.PENDING)
                .build();

        when(jobRepository.findByStatusAndExperimentStatusInOrderByCreatedAtAsc(
                ExperimentPipelineGenerationJobStatus.PENDING,
                Set.of(ExperimentStatus.PLANNED, ExperimentStatus.RUNNING, ExperimentStatus.PAUSED),
                org.springframework.data.domain.PageRequest.of(0, 50)))
                .thenReturn(List.of(pendingFromPlanned));

        List<com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto> result =
                service.listPendingJobs(99);

        assertEquals(1, result.size());
        assertEquals(pendingFromPlanned.getId(), result.get(0).id());
    }

    @Test
    void closeOpenJobsMarksPendingAndProcessingAsFailed() {
        Experiment experiment = new Experiment();
        experiment.setId(55L);
        experiment.setStatus(ExperimentStatus.FINISHED);

        ExperimentPipelineGenerationJob pendingJob = ExperimentPipelineGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(experiment)
                .section(ExperimentPipelineSection.CAMPAIGN_ANGLE)
                .status(ExperimentPipelineGenerationJobStatus.PENDING)
                .stage(ExperimentPipelineGenerationJobStage.WAITING_AI_WORKER)
                .build();
        ExperimentPipelineGenerationJob processingJob = ExperimentPipelineGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(experiment)
                .section(ExperimentPipelineSection.LANDING_PAGE_HTML)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.WAITING_OPENAI)
                .build();

        when(experimentRepository.findById(55L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdAndStatusInOrderByCreatedAtDesc(
                55L,
                Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING)))
                .thenReturn(List.of(pendingJob, processingJob));

        int closed = service.closeOpenJobs(55L, "Encerrado manualmente");

        assertEquals(2, closed);
        assertEquals(ExperimentPipelineGenerationJobStatus.FAILED, pendingJob.getStatus());
        assertEquals(ExperimentPipelineGenerationJobStage.FAILED, pendingJob.getStage());
        assertEquals("Encerrado manualmente", pendingJob.getErrorMessage());
        assertNotNull(pendingJob.getFinishedAt());

        assertEquals(ExperimentPipelineGenerationJobStatus.FAILED, processingJob.getStatus());
        assertEquals(ExperimentPipelineGenerationJobStage.FAILED, processingJob.getStage());
        assertEquals("Encerrado manualmente", processingJob.getErrorMessage());
        assertNotNull(processingJob.getFinishedAt());
        verify(jobRepository, times(1)).findByExperimentIdAndStatusInOrderByCreatedAtDesc(
                55L,
                Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING));
    }

    private ExperimentPipelineGenerationJob completedJob(Experiment experiment,
                                                         ExperimentPipelineSection section,
                                                         String responseContent) {
        return ExperimentPipelineGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(experiment)
                .section(section)
                .status(ExperimentPipelineGenerationJobStatus.COMPLETED)
                .stage(ExperimentPipelineGenerationJobStage.COMPLETED)
                .responseContent(responseContent)
                .build();
    }

    @Test
    void completeJobNormalizesStringifiedLandingCopyBeforePersistingOnExperiment() throws Exception {
        Experiment experiment = new Experiment();
        experiment.setId(42L);

        UUID jobId = UUID.randomUUID();
        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .id(jobId)
                .experiment(experiment)
                .section(ExperimentPipelineSection.LANDING_PAGE_COPY)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI)
                .model("gpt-5.2")
                .prompt("prompt")
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ExperimentPipelineGenerationJobCompletionRequest request = new ExperimentPipelineGenerationJobCompletionRequest(
                "{\"landingPageCopy\":\"{\\\"pageGoal\\\":\\\"Gerar lead\\\",\\\"primaryCTA\\\":\\\"Quero minha prévia\\\"}\"}",
                "{\"id\":\"resp_1\"}",
                "{\"model\":\"gpt-5.2\"}",
                100,
                50,
                null);
        when(openAiPricingService.estimateStandardCost(any(), any())).thenReturn(new BigDecimal("0.0750"));

        service.completeJob(jobId, request);
        verify(costAttributionService).addUsdCostToExperimentHierarchy(experiment, new BigDecimal("0.0750"));

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> persisted = mapper.readValue(experiment.getLandingPageCopy(), java.util.Map.class);
        assertTrue(persisted.get("landingPageCopy") instanceof java.util.Map);
    }

    @Test
    void completeJobNormalizesLandingPageHtmlFormContractAndAvoidsPrevious422Scenario() throws Exception {
        Experiment experiment = new Experiment();
        experiment.setId(77L);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "sectionId": "hero",
                        "surfaceSpec": {
                          "surfaceToken": "surface-base",
                          "style": "band",
                          "contrastMode": "normal",
                          "notes": "hero"
                        }
                      }
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "title": "Receber a prévia do Kit (IA)",
                      "submitLabel": "Desbloquear o Kit (receber a prévia gerada por IA)",
                      "submitTarget": "#desbloquear",
                      "fields": [
                        {"name": "nome", "type": "text", "required": true},
                        {"name": "email", "type": "email", "required": true},
                        {"name": "whatsapp", "type": "tel", "required": false}
                      ],
                      "consent": {"enabled": true, "required": false, "label": "ok"},
                      "successState": {"title": "ok", "message": "ok"}
                    }
                  }
                }
                """);

        UUID jobId = UUID.randomUUID();
        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .id(jobId)
                .experiment(experiment)
                .section(ExperimentPipelineSection.LANDING_PAGE_HTML)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI)
                .model("gpt-5.2")
                .prompt("prompt")
                .build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ExperimentPipelineGenerationJobCompletionRequest request = new ExperimentPipelineGenerationJobCompletionRequest(
                """
                        <!doctype html><html><body><section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'><form id='lead-capture-primary'><div class='field'><input type='text' name='nome' required /></div><div class='field'><select name='objetivo'><option>a</option></select></div></form><button type='submit'>Desbloquear o Kit (receber a prévia gerada por IA)</button></section></body></html>
                        """,
                "{\"id\":\"resp_77\"}",
                "{\"model\":\"gpt-5.2\"}",
                120,
                80,
                null);

        service.completeJob(jobId, request);

        String html = experiment.getLandingPageHtml().toLowerCase();

        assertTrue(html.contains("name=\"nome\""));
        assertTrue(html.contains("name=\"email\""));
        assertTrue(html.contains("name=\"whatsapp\""));
        assertTrue(html.contains("button type=\"submit\" form=\"lead-capture-primary\""));
        assertFalse(html.contains("name=\"objetivo\""));
        assertFalse(html.contains("objetivo principal"));
    }

    @Test
    void completeJobRejectsLandingHtmlWhenSectionMatchesButBindingKeyIsWrong() {
        Experiment experiment = buildLandingHtmlValidationExperiment(201L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(createLandingHtmlJob(experiment).getId(), landingHtmlCompletionRequest("""
                        <!doctype html><html><body>
                        <section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'>
                          <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                               data-image-section-id='hero'
                               data-image-binding-key='hero_wrong_key'
                               data-image-role='Hero Pain Anchor'
                               data-conversion-role='grab-attention'
                               data-attention-priority='high'
                               data-visual-weight='primary'
                               data-distance-to-cta='near'
                               data-supports-form-conversion='true' />
                          <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                        </section></body></html>
                        """)));
        assertTrue(exception.getReason().contains("sectionId/imageBindingKey"));
    }

    @Test
    void completeJobRejectsLandingHtmlWhenUsingApproximateTextualRoleInsteadOfCanonicalBinding() {
        Experiment experiment = buildLandingHtmlValidationExperiment(202L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(createLandingHtmlJob(experiment).getId(), landingHtmlCompletionRequest("""
                        <!doctype html><html><body>
                        <section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'>
                          <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                               data-image-section-id='hero'
                               data-image-role='Hero Pain Anchor Approx'
                               data-conversion-role='grab-attention'
                               data-attention-priority='high'
                               data-visual-weight='primary'
                               data-distance-to-cta='near'
                               data-supports-form-conversion='true' />
                          <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                        </section></body></html>
                        """)));
        assertTrue(exception.getReason().contains("sectionId/imageBindingKey"));
    }

    @Test
    void completeJobAcceptsHtmlWhenCanonicalSectionIdAndBindingKeyMatchEvenIfOtherImageMetadataDiffers() {
        Experiment experiment = buildLandingHtmlValidationExperiment(203L);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-binding-key='hero_pain_anchor'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='wrong-conversion'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeJobSucceedsWhenCanonicalImageBindingMatchesPlanningExactly() {
        Experiment experiment = buildLandingHtmlValidationExperiment(204L);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-binding-key='hero_pain_anchor'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='grab-attention'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeJobUsesDesignPresetAsCanonicalSourceForSurfaceStyleAndContrast() {
        Experiment experiment = buildLandingHtmlValidationExperiment(240L);
        experiment.setLandingPageDesignPreset("""
                {
                  "landingPageDesignPreset": {
                    "sectionPresets": [
                      {
                        "sectionId": "hero",
                        "surfaceStyle": "solid",
                        "contrastMode": "high"
                      }
                    ]
                  }
                }
                """);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id='hero' data-surface-token='surface-base' data-surface-style='solid' data-surface-contrast='high'>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-binding-key='hero_pain_anchor'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='grab-attention'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeJobRejectsWireframeWhenSurfaceTokenIsMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(410L);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageWireframe": {
                                    "pageGoal": "goal",
                                    "variantLayoutId": "form-first",
                                    "sectionOrder": [
                                      {
                                        "sectionId": "hero",
                                        "sectionName": "Hero",
                                        "objective": "obj",
                                        "contentType": "hero",
                                        "mobilePriorityScore": 10,
                                        "dropOffRisk": "baixo",
                                        "surfaceSpec": {"notes": "sem token"}
                                      }
                                    ],
                                    "consistencyChecks": [{"check":"CTA_MATCH","status":"PASS"}],
                                    "formSpec": {
                                      "formId": "lead-capture-primary",
                                      "title": "Receber a prévia",
                                      "submitLabel": "Enviar",
                                      "submitTarget": "/api/flows/submissions",
                                      "fields": [{"name":"nome","type":"text","label":"Nome","required":true,"placeholder":"Seu nome"}],
                                      "consent": {"enabled": true, "required": false, "label": "ok"},
                                      "successState": {"title":"ok","message":"ok"}
                                    }
                                  },
                                  "experimentMetadata": {
                                    "primary_variable":"pv","variant_id":"v1","stage":"AD","control_or_treatment":"treatment","asset_role":"landing-page-wireframe"
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("slotDefs estruturado"));
    }

    @Test
    void completeJobRejectsWireframeWhenSurfaceStyleIsMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(422L);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageWireframe": {
                                    "readingFlowSpec": {"maxParagraphLinesMobile": 4, "bulletDensityPerSection": 3},
                                    "conversionPathSpec": {"primaryAction": "Desbloquear", "ctaLabelCanonical": "Desbloquear"},
                                    "proofPlan": {"requiredProofTypes": ["social-proof","metric-proof"], "proofSectionIds": ["hero"]},
                                    "trustSignalsSpec": {"brandIdentityRequired": true, "privacyNoticeNearForm": true, "privacyPolicyUrl": "https://example.com/privacy", "legalFooterItems": ["empresa","contato","política de privacidade"]},
                                    "accessibilitySpec": {"minTextContrast":"4.5:1","minTouchTargetPx":44,"formFieldMinHeightPx":44},
                                    "sectionOrder": [
                                      {
                                        "sectionId": "hero",
                                        "sectionName": "Hero",
                                        "objective": "obj",
                                        "contentType": "hero",
                                        "mobilePriorityScore": 10,
                                        "dropOffRisk": "baixo",
                                        "surfaceSpec": {"surfaceToken": "surface-base", "notes": "ok"}
                                      }
                                    ]
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("slotDefs estruturado"));
    }

    @Test
    void completeJobRejectsWireframeWhenCanonicalBlocksAreMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(420L);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_WIREFRAME);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageWireframe": {
                                    "sectionOrder": [
                                      {
                                        "sectionId": "hero",
                                        "sectionName": "Hero",
                                        "objective": "obj",
                                        "contentType": "hero",
                                        "mobilePriorityScore": 9,
                                        "dropOffRisk": "alto",
                                        "surfaceSpec": {"surfaceToken": "surface-base", "notes": "ok"}
                                      }
                                    ],
                                    "consistencyChecks": [{"check":"CTA_MATCH","status":"PASS"}],
                                    "formSpec": {
                                      "formId": "lead-capture-primary",
                                      "title": "Receber a prévia",
                                      "submitLabel": "Enviar",
                                      "submitTarget": "/api/flows/submissions",
                                      "fields": [{"name":"nome","type":"text","label":"Nome","required":true,"placeholder":"Seu nome"}],
                                      "consent": {"enabled": true, "required": false, "label": "ok"},
                                      "successState": {"title":"ok","message":"ok"}
                                    }
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("slotDefs estruturado"));
    }

    @Test
    void completeJobRejectsDesignPresetWhenSectionPresetsDoesNotCoverWireframeSections() {
        Experiment experiment = new Experiment();
        experiment.setId(411L);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "readingFlowSpec": {"maxParagraphLinesMobile": 4, "bulletDensityPerSection": 3},
                    "conversionPathSpec": {"primaryAction": "Desbloquear", "ctaLabelCanonical": "Desbloquear"},
                    "proofPlan": {"requiredProofTypes": ["social-proof","metric-proof"], "proofSectionIds": ["proof"]},
                    "trustSignalsSpec": {"brandIdentityRequired": true, "privacyNoticeNearForm": true, "privacyPolicyUrl": "https://example.com/privacy", "legalFooterItems": ["empresa","contato","política de privacidade"]},
                    "accessibilitySpec": {"minTextContrast":"4.5:1","minTouchTargetPx":44,"formFieldMinHeightPx":44},
                    "sectionOrder": [
                      {
                        "sectionId": "hero",
                        "dropOffRisk": "medio",
                        "mobilePriorityScore": 8,
                        "surfaceSpec": {"surfaceToken": "surface-base", "notes": "hero"}
                      },
                      {
                        "sectionId": "proof",
                        "dropOffRisk": "medio",
                        "mobilePriorityScore": 7,
                        "surfaceSpec": {"surfaceToken": "surface-alt-1", "notes": "proof"}
                      }
                    ]
                  }
                }
                """);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageDesignPreset": {
                                    "presetId": "preset-1",
                                    "lhmRuntime": {"baseCss":".x{}","cssVersion":"v1","cssNotes":"baseline"},
                                    "theme": {
                                      "palette": {
                                        "background":"#fff","surface":"#fff","textPrimary":"#111","textMuted":"#666","brandPrimary":"#1d4ed8","brandSecondary":"#1e40af","border":"#e2e8f0"
                                      },
                                      "typography": {"maxLineLength":"65ch","lineHeightBody":"1.6"},
                                      "spacing": {"sectionGapMobile":"56px"},
                                      "accessibility": {"textContrastBody":"4.5:1","focusRing":"0 0 0 3px rgba(37,99,235,0.45)"}
                                    },
                                    "sectionPresets": [
                                      {"sectionId":"hero","surfaceStyle":"solid","contrastMode":"high","layoutPreset":"hero-focus","emphasis":"primary","notes":"hero"}
                                    ],
                                    "componentPresets": {
                                      "cta": {"stickyMobile":true},
                                      "trust": {"showLegalFooter":true},
                                      "proof": {"showIdentity": true},
                                      "primitives": [{"key":"hero-title","className":"lhm-hero-title","notes":"ok"},{"key":"section-title","className":"lhm-section-title","notes":"ok"},{"key":"body","className":"lhm-body","notes":"ok"},{"key":"btn-primary","className":"lhm-btn-primary","notes":"ok"},{"key":"btn-secondary","className":"lhm-btn-secondary","notes":"ok"},{"key":"field","className":"lhm-field","notes":"ok"},{"key":"card","className":"lhm-card","notes":"ok"},{"key":"faq-item","className":"lhm-faq-item","notes":"ok"}],
                                      "registry": [{"componentKey":"hero-form-split","templatePartial":"hero-form-split","notes":"ok"},{"componentKey":"proof","templatePartial":"proof","notes":"ok"},{"componentKey":"offer-cards","templatePartial":"offer-cards","notes":"ok"},{"componentKey":"faq","templatePartial":"faq","notes":"ok"}]
                                    },
                                    "motion": {"enabled": false, "intensity": "none"},
                                    "consistencyChecks": [
                                      {"check":"THEME_CONTRAST","status":"PASS"},
                                      {"check":"CTA_VISUAL_HIERARCHY","status":"PASS"},
                                      {"check":"MOBILE_READABILITY","status":"PASS"}
                                    ]
                                  },
                                  "experimentMetadata": {
                                    "primary_variable":"pv","variant_id":"v1","stage":"AD","control_or_treatment":"treatment","asset_role":"landing-page-design-preset"
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("Preset de design incompleto"));
        assertTrue(exception.getReason().contains("proof"));
    }

    @Test
    void completeJobRejectsDesignPresetWhenMandatoryChecksAreMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(421L);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "readingFlowSpec": {"maxParagraphLinesMobile": 4, "bulletDensityPerSection": 3},
                    "conversionPathSpec": {"primaryAction": "Desbloquear", "ctaLabelCanonical": "Desbloquear"},
                    "proofPlan": {"requiredProofTypes": ["social-proof","metric-proof"], "proofSectionIds": ["hero"]},
                    "trustSignalsSpec": {"brandIdentityRequired": true, "privacyNoticeNearForm": true, "privacyPolicyUrl": "https://example.com/privacy", "legalFooterItems": ["empresa","contato","política de privacidade"]},
                    "accessibilitySpec": {"minTextContrast":"4.5:1","minTouchTargetPx":44,"formFieldMinHeightPx":44},
                    "sectionOrder": [
                      {
                        "sectionId": "hero",
                        "dropOffRisk": "medio",
                        "mobilePriorityScore": 8,
                        "surfaceSpec": {"surfaceToken": "surface-base", "notes": "hero"}
                      }
                    ]
                  }
                }
                """);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageDesignPreset": {
                                    "presetId": "preset-421",
                                    "lhmRuntime": {"baseCss":".x{}","cssVersion":"v1","cssNotes":"baseline"},
                                    "theme": {
                                      "palette": {
                                        "background":"#fff","surface":"#fff","textPrimary":"#111","textMuted":"#666","brandPrimary":"#1d4ed8","brandSecondary":"#1e40af","border":"#e2e8f0"
                                      },
                                      "typography": {"maxLineLength":"65ch","lineHeightBody":"1.6"},
                                      "spacing": {"sectionGapMobile":"56px"},
                                      "accessibility": {"textContrastBody":"4.5:1","focusRing":"0 0 0 3px rgba(37,99,235,0.45)"}
                                    },
                                    "sectionPresets": [
                                      {"sectionId":"hero","surfaceStyle":"solid","contrastMode":"high","layoutPreset":"hero-focus","emphasis":"primary","notes":"hero"}
                                    ],
                                    "componentPresets": {
                                      "cta": {"stickyMobile":true},
                                      "trust": {"showLegalFooter":true},
                                      "proof": {"showIdentity": true},
                                      "primitives": [{"key":"hero-title","className":"lhm-hero-title","notes":"ok"},{"key":"section-title","className":"lhm-section-title","notes":"ok"},{"key":"body","className":"lhm-body","notes":"ok"},{"key":"btn-primary","className":"lhm-btn-primary","notes":"ok"},{"key":"btn-secondary","className":"lhm-btn-secondary","notes":"ok"},{"key":"field","className":"lhm-field","notes":"ok"},{"key":"card","className":"lhm-card","notes":"ok"},{"key":"faq-item","className":"lhm-faq-item","notes":"ok"}],
                                      "registry": [{"componentKey":"hero-form-split","templatePartial":"hero-form-split","notes":"ok"},{"componentKey":"proof","templatePartial":"proof","notes":"ok"},{"componentKey":"offer-cards","templatePartial":"offer-cards","notes":"ok"},{"componentKey":"faq","templatePartial":"faq","notes":"ok"}]
                                    },
                                    "motion": {"enabled": false, "intensity": "none"},
                                    "consistencyChecks": [{"check":"THEME_CONTRAST","status":"PASS"}]
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("CTA_VISUAL_HIERARCHY"));
        assertTrue(exception.getReason().contains("MOBILE_READABILITY"));
    }

    @Test
    void completeJobRejectsDesignPresetWhenLhmRuntimeIsMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(423L);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "readingFlowSpec": {"maxParagraphLinesMobile": 4, "bulletDensityPerSection": 3},
                    "conversionPathSpec": {"primaryAction": "Desbloquear", "ctaLabelCanonical": "Desbloquear"},
                    "proofPlan": {"requiredProofTypes": ["social-proof","metric-proof"], "proofSectionIds": ["hero"]},
                    "trustSignalsSpec": {"brandIdentityRequired": true, "privacyNoticeNearForm": true, "privacyPolicyUrl": "https://example.com/privacy", "legalFooterItems": ["empresa","contato","política de privacidade"]},
                    "accessibilitySpec": {"minTextContrast":"4.5:1","minTouchTargetPx":44,"formFieldMinHeightPx":44},
                    "sectionOrder": [
                      {"sectionId":"hero","dropOffRisk":"medio","mobilePriorityScore":8,"surfaceSpec":{"surfaceToken":"surface-base","style":"solid","contrastMode":"high","notes":"hero"}}
                    ]
                  }
                }
                """);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageDesignPreset": {
                                    "presetId": "preset-423",
                                    "theme": {
                                      "palette": {
                                        "background":"#fff","surface":"#fff","textPrimary":"#111","textMuted":"#666","brandPrimary":"#1d4ed8","brandSecondary":"#1e40af","border":"#e2e8f0"
                                      },
                                      "typography": {"maxLineLength":"65ch","lineHeightBody":"1.6"},
                                      "spacing": {"sectionGapMobile":"56px"},
                                      "accessibility": {"textContrastBody":"4.5:1"}
                                    },
                                    "sectionPresets": [
                                      {"sectionId":"hero","surfaceStyle":"solid","contrastMode":"high","layoutPreset":"hero-focus","emphasis":"primary","notes":"hero"}
                                    ],
                                    "componentPresets": {
                                      "cta": {"stickyMobile":true},
                                      "trust": {"showLegalFooter":true},
                                      "proof": {"showIdentity": true}
                                    },
                                    "motion": {"enabled": false, "intensity": "none"},
                                    "consistencyChecks": [
                                      {"check":"THEME_CONTRAST","status":"PASS"},
                                      {"check":"CTA_VISUAL_HIERARCHY","status":"PASS"},
                                      {"check":"MOBILE_READABILITY","status":"PASS"}
                                    ]
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("lhmRuntime"));
    }

    @Test
    void completeJobRejectsLandingCopyWhenCanonicalChecksAreMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(412L);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_COPY);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageCopy": {
                                    "hero": {
                                      "headline": "Headline",
                                      "promise": "Promise",
                                      "ctaLabel": "CTA"
                                    },
                                    "bodySections": [
                                      {"sectionId": "hero", "summary": "Resumo"}
                                    ],
                                    "consistencyChecks": [
                                      {"check":"GOOGLE_LANDING_BEST_PRACTICES","status":"PASS"}
                                    ]
                                  },
                                  "experimentMetadata": {
                                    "primary_variable":"pv","variant_id":"v1","stage":"AD","control_or_treatment":"treatment","asset_role":"landing-page-copy"
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("CTA_MATCH"));
        assertTrue(exception.getReason().contains("PROMISE_MATCH"));
    }

    @Test
    void completeJobRejectsImagePlanningWhenGenerationPromptIsMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(413L);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {"sectionId": "hero", "surfaceSpec": {"surfaceToken": "surface-base", "notes": "hero"}}
                    ]
                  }
                }
                """);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                        """
                                {
                                  "landingPageImagePlanning": {
                                    "images": [
                                      {"sectionId":"hero","imageBindingKey":"hero-anchor"}
                                    ],
                                    "consistencyChecks": [
                                      {"check":"VISUAL_HIERARCHY","status":"PASS"}
                                    ]
                                  },
                                  "experimentMetadata": {
                                    "primary_variable":"pv","variant_id":"v1","stage":"AD","control_or_treatment":"treatment","asset_role":"landing-page-image-planning"
                                  }
                                }
                                """,
                        null, null, null, null, null)));

        assertTrue(exception.getReason().contains("generationPrompt é obrigatório"));
    }

    @Test
    void completeJobSucceedsWhenBindingKeyContainsEscapedQuoteArtifacts() {
        Experiment experiment = buildLandingHtmlValidationExperiment(205L);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-binding-key='\\&quot;hero_pain_anchor\\&quot;\\n'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='grab-attention'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeJobAutomaticallyEnqueuesSuccessorSection() {
        Experiment experiment = new Experiment();
        experiment.setId(301L);

        UUID jobId = UUID.randomUUID();
        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .id(jobId)
                .experiment(experiment)
                .section(ExperimentPipelineSection.AD_COPY)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI)
                .model("gpt-5.2")
                .prompt("prompt")
                .build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service.completeJob(jobId, new ExperimentPipelineGenerationJobCompletionRequest(
                "{\"adCopy\":{\"headline\":\"teste\"}}",
                "{\"id\":\"resp_copy\"}",
                "{\"model\":\"gpt-5.2\"}",
                40,
                20,
                null));

        verify(jobRepository).save(argThat(saved ->
                saved.getSection() == ExperimentPipelineSection.AD_IMAGE_BRIEFING
                        && saved.getExperiment() == experiment
                        && saved.getStatus() == ExperimentPipelineGenerationJobStatus.PENDING));
    }

    @Test
    void completeJobDoesNotAutomaticallyEnqueueLandingHtmlAfterDesignPreset() {
        Experiment experiment = new Experiment();
        experiment.setId(314L);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "readingFlowSpec": {"maxParagraphLinesMobile": 4, "bulletDensityPerSection": 3},
                    "conversionPathSpec": {"primaryAction": "Desbloquear", "ctaLabelCanonical": "Desbloquear"},
                    "proofPlan": {"requiredProofTypes": ["social-proof","metric-proof"], "proofSectionIds": ["hero"]},
                    "trustSignalsSpec": {"brandIdentityRequired": true, "privacyNoticeNearForm": true, "privacyPolicyUrl": "https://example.com/privacy", "legalFooterItems": ["empresa","contato","política de privacidade"]},
                    "accessibilitySpec": {"minTextContrast":"4.5:1","minTouchTargetPx":44,"formFieldMinHeightPx":44},
                    "sectionOrder": [
                      {
                        "sectionId": "hero",
                        "dropOffRisk": "medio",
                        "mobilePriorityScore": 8,
                        "surfaceSpec": {"surfaceToken": "surface-base", "notes": "hero"}
                      }
                    ]
                  }
                }
                """);
        ExperimentPipelineGenerationJob job = createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET);
        String designPresetPayload = """
                {
                  "landingPageDesignPreset": {
                    "presetId": "preset-manual-html",
                    "lhmRuntime": {"baseCss":".x{}","cssVersion":"v1","cssNotes":"baseline"},
                    "theme": {
                      "palette": {
                        "background":"#ffffff",
                        "surface":"#ffffff",
                        "textPrimary":"#111827",
                        "textMuted":"#6b7280",
                        "brandPrimary":"#2563eb",
                        "brandSecondary":"#1d4ed8",
                        "border":"#e5e7eb"
                      },
                      "typography": {"maxLineLength":"64ch","lineHeightBody":"1.6"},
                      "spacing": {"sectionGapMobile":"56px"},
                      "accessibility": {"textContrastBody":"4.5:1","focusRing":"0 0 0 3px rgba(37,99,235,0.45)"}
                    },
                    "sectionPresets": [
                      {
                        "sectionId":"hero",
                        "surfaceStyle":"band",
                        "contrastMode":"normal",
                        "layoutPreset":"hero-focus",
                        "emphasis":"primary",
                        "notes":"hero"
                      }
                    ],
                    "componentPresets": {
                      "cta": {"stickyMobile": true},
                      "trust": {"showLegalFooter": true},
                      "proof": {"showIdentity": true},
                      "primitives": [{"key":"hero-title","className":"lhm-hero-title","notes":"ok"},{"key":"section-title","className":"lhm-section-title","notes":"ok"},{"key":"body","className":"lhm-body","notes":"ok"},{"key":"btn-primary","className":"lhm-btn-primary","notes":"ok"},{"key":"btn-secondary","className":"lhm-btn-secondary","notes":"ok"},{"key":"field","className":"lhm-field","notes":"ok"},{"key":"card","className":"lhm-card","notes":"ok"},{"key":"faq-item","className":"lhm-faq-item","notes":"ok"}],
                      "registry": [{"componentKey":"hero-form-split","templatePartial":"hero-form-split","notes":"ok"},{"componentKey":"proof","templatePartial":"proof","notes":"ok"},{"componentKey":"offer-cards","templatePartial":"offer-cards","notes":"ok"},{"componentKey":"faq","templatePartial":"faq","notes":"ok"}]
                    },
                    "motion": {"enabled": false, "intensity": "none"},
                    "consistencyChecks": [
                      {"check":"THEME_CONTRAST","status":"PASS"},
                      {"check":"CTA_VISUAL_HIERARCHY","status":"PASS"},
                      {"check":"MOBILE_READABILITY","status":"PASS"}
                    ]
                  },
                  "experimentMetadata": {
                    "primary_variable":"pv",
                    "variant_id":"v1",
                    "stage":"AD",
                    "control_or_treatment":"treatment",
                    "asset_role":"landing-page-design-preset"
                  }
                }
                """;

        service.completeJob(job.getId(), new ExperimentPipelineGenerationJobCompletionRequest(
                designPresetPayload,
                "{\"id\":\"resp_design\"}",
                "{\"model\":\"gpt-5.2\"}",
                50,
                30,
                null));

        verify(jobRepository, never()).save(argThat(saved ->
                saved.getSection() == ExperimentPipelineSection.LANDING_PAGE_HTML
                        && saved.getStatus() == ExperimentPipelineGenerationJobStatus.PENDING));
    }

    @Test
    void generateLandingDesignPresetFailsWhenPlannedImagesAreNotFullyGenerated() {
        Experiment experiment = new Experiment();
        experiment.setId(302L);
        experiment.setLandingPageImagePlanning("{\"landingPageImagePlanning\":{\"images\":[{\"sectionId\":\"hero\",\"imagePrompt\":\"x\"}]}}");

        when(experimentRepository.findById(302L)).thenReturn(Optional.of(experiment));
        when(frameworkImageGenerationService.allPlanningImagesCompleted(302L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.generate(302L, ExperimentPipelineSection.LANDING_PAGE_DESIGN_PRESET, new com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest()));

        assertTrue(ex.getReason().contains("geração completa das imagens"));
    }

    @Test
    void completeJobAcceptsHtmlWhenSurfaceAttributesAreUnquoted() {
        Experiment experiment = buildLandingHtmlValidationExperiment(206L);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id=hero data-surface-token=surface-base data-surface-style=band data-surface-contrast=normal>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-binding-key='hero_pain_anchor'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='grab-attention'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeJobAcceptsHtmlWhenSurfaceAttributesContainEncodedQuotes() {
        Experiment experiment = buildLandingHtmlValidationExperiment(207L);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id='&quot;hero&quot;'
                         data-surface-token='&quot;surface-base&quot;'
                         data-surface-style='&#34;band&#34;'
                         data-surface-contrast='&#x22;normal&#x22;'>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-binding-key='hero_pain_anchor'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='grab-attention'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeJobAcceptsHtmlWhenSurfaceAttributesContainEscapedEncodedQuotesAndNewlineTokens() {
        Experiment experiment = buildLandingHtmlValidationExperiment(208L);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id='\\&quot;hero\\&quot;\\n'
                         data-surface-token='\\&quot;surface-base\\&quot;\\n'
                         data-surface-style='\\&#34;band\\&#34;\\n'
                         data-surface-contrast='\\&#x22;normal\\&#x22;\\n'>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-binding-key='hero_pain_anchor'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='grab-attention'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeJobSupportsLegacyFallbackWhenPlanningAndHtmlDoNotSendBindingKey() {
        Experiment experiment = buildLandingHtmlValidationExperiment(205L);
        experiment.setLandingPageImagePlanning("""
                {
                  "landingPageImagePlanning": {
                    "consistencyChecks": [
                      {"check": "IMAGE_MESSAGE_MATCH", "status": "PASS"},
                      {"check": "CTA_CONTINUITY", "status": "PASS"}
                    ],
                    "images": [
                      {
                        "sectionId": "hero",
                        "imageRole": "Hero Pain Anchor",
                        "conversionRole": "grab-attention",
                        "attentionPriority": "high",
                        "visualWeight": "primary",
                        "distanceToCTA": "near",
                        "supportsFormConversion": true
                      }
                    ]
                  }
                }
                """);
        ExperimentPipelineGenerationJob job = createLandingHtmlJob(experiment);

        service.completeJob(job.getId(), landingHtmlCompletionRequest("""
                <!doctype html><html><body>
                <section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'>
                  <img src='https://cdn.example.com/hero.jpg' alt='Hero'
                       data-image-section-id='hero'
                       data-image-role='Hero Pain Anchor'
                       data-conversion-role='grab-attention'
                       data-attention-priority='high'
                       data-visual-weight='primary'
                       data-distance-to-cta='near'
                       data-supports-form-conversion='true' />
                  <form id='lead-capture-primary'><input type='text' name='nome' required /><input type='email' name='email' required /><input type='tel' name='whatsapp' /></form>
                </section></body></html>
                """));

        assertNotNull(experiment.getLandingPageHtml());
    }

    @Test
    void completeLandingImagePlanningNormalizesBindingKeysWhenMissing() throws Exception {
        Experiment experiment = new Experiment();
        experiment.setId(42L);
        ExperimentPipelineGenerationJob job = createLandingImagePlanningJob(experiment);

        String payload = """
                {
                  "landingPageImagePlanning": {
                    "generationPrompt": "Gerar imagem hero com foco em dor e continuidade com o anúncio",
                    "consistencyChecks": [
                      {"check": "IMAGE_MESSAGE_MATCH", "status": "PASS"},
                      {"check": "CTA_CONTINUITY", "status": "PASS"}
                    ],
                    "images": [
                      {
                        "sectionId": "s0-hero-variant",
                        "sectionName": "Hero",
                        "imageRole": "Hero Pain Anchor",
                        "conversionRole": "grab-attention",
                        "emotionalJob": "urgencia",
                        "sectionVisualGoal": "Mostrar continuidade com o anúncio",
                        "placement": "hero",
                        "hierarchyLevel": "primary",
                        "objective": "Garantir message match",
                        "imagePrompt": "Foto realista",
                        "negativePrompt": "none",
                        "visualStyle": "photo",
                        "composition": "rule of thirds",
                        "focalPoint": "subject",
                        "supportingElements": ["element"],
                        "mood": "direct",
                        "layoutBinding": {
                          "preferredDesktopPlacement": "right",
                          "preferredMobilePlacement": "above-copy",
                          "desktopAspectRatio": "16:9",
                          "mobileAspectRatio": "4:5",
                          "allowCrop": true,
                          "safeCropZones": {
                            "top": 0.1,
                            "right": 0.1,
                            "bottom": 0.1,
                            "left": 0.1
                          }
                        },
                        "attentionPriority": "high",
                        "visualWeight": "primary",
                        "distanceToCTA": "near",
                        "supportsFormConversion": true,
                        "formRelationNotes": "notes",
                        "dimensions": {
                          "desktop": "1600x900",
                          "mobile": "1080x1350"
                        },
                        "safeMargins": "10%",
                        "textOverlayGuidance": "none",
                        "generationHints": ["hint"],
                        "messageMatchNotes": "notes",
                        "complianceNotes": "notes"
                      }
                    ]
                  }
                }
                """;

        service.completeJob(job.getId(), landingImagePlanningCompletionRequest(payload));

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> stored = mapper.readValue(experiment.getLandingPageImagePlanning(), Map.class);
        Map<String, Object> planning = (Map<String, Object>) stored.get("landingPageImagePlanning");
        List<Map<String, Object>> images = (List<Map<String, Object>>) planning.get("images");
        assertEquals("hero-pain-anchor", images.get(0).get("imageBindingKey"));
    }

    private Experiment buildLandingHtmlValidationExperiment(long experimentId) {
        Experiment experiment = new Experiment();
        experiment.setId(experimentId);
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "sectionId": "hero",
                        "surfaceSpec": {
                          "surfaceToken": "surface-base",
                          "style": "band",
                          "contrastMode": "normal",
                          "notes": "hero"
                        }
                      }
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "title": "Receber a prévia do Kit (IA)",
                      "submitLabel": "Desbloquear o Kit (receber a prévia gerada por IA)",
                      "submitTarget": "#desbloquear",
                      "fields": [
                        {"name": "nome", "type": "text", "required": true},
                        {"name": "email", "type": "email", "required": true},
                        {"name": "whatsapp", "type": "tel", "required": false}
                      ],
                      "consent": {"enabled": true, "required": false, "label": "ok"},
                      "successState": {"title": "ok", "message": "ok"}
                    }
                  }
                }
                """);
        experiment.setLandingPageImagePlanning("""
                {
                  "landingPageImagePlanning": {
                    "images": [
                      {
                        "sectionId": "hero",
                        "imageBindingKey": "hero_pain_anchor",
                        "imageRole": "Hero Pain Anchor",
                        "conversionRole": "grab-attention",
                        "attentionPriority": "high",
                        "visualWeight": "primary",
                        "distanceToCTA": "near",
                        "supportsFormConversion": true
                      }
                    ]
                  }
                }
                """);
        return experiment;
    }

    private ExperimentPipelineGenerationJob createLandingHtmlJob(Experiment experiment) {
        return createJobForSection(experiment, ExperimentPipelineSection.LANDING_PAGE_HTML);
    }

    private ExperimentPipelineGenerationJob createJobForSection(Experiment experiment, ExperimentPipelineSection section) {
        UUID jobId = UUID.randomUUID();
        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .id(jobId)
                .experiment(experiment)
                .section(section)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI)
                .model("gpt-5.2")
                .prompt("prompt")
                .build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        return job;
    }

    private ExperimentPipelineGenerationJob createLandingImagePlanningJob(Experiment experiment) {
        UUID jobId = UUID.randomUUID();
        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .id(jobId)
                .experiment(experiment)
                .section(ExperimentPipelineSection.LANDING_PAGE_IMAGE_PLANNING)
                .status(ExperimentPipelineGenerationJobStatus.PROCESSING)
                .stage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI)
                .model("gpt-5.2")
                .prompt("prompt")
                .build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        return job;
    }

    private ExperimentPipelineGenerationJobCompletionRequest landingImagePlanningCompletionRequest(String payload) {
        return new ExperimentPipelineGenerationJobCompletionRequest(
                payload,
                "{\"id\":\"resp_planning\"}",
                "{\"model\":\"gpt-5.2\"}",
                90,
                60,
                null);
    }

    private ExperimentPipelineGenerationJobCompletionRequest landingHtmlCompletionRequest(String htmlDocument) {
        return new ExperimentPipelineGenerationJobCompletionRequest(
                htmlDocument,
                "{\"id\":\"resp_html\"}",
                "{\"model\":\"gpt-5.2\"}",
                120,
                80,
                null);
    }
}
