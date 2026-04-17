package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStage;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobCompletionRequest;
import com.marketinghub.experiment.pipeline.repository.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;

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
                landingPageImageInjector);
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

        service.completeJob(jobId, request);

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
                        {
                          "landingPageHtml": {
                            "htmlDocument": "<!doctype html><html><body><section data-section-id='hero' data-surface-token='surface-base' data-surface-style='band' data-surface-contrast='normal'><form id='lead-capture-primary'><div class='field'><input type='text' name='nome' required /></div><div class='field'><select name='objetivo'><option>a</option></select></div></form><button type='submit'>Desbloquear o Kit (receber a prévia gerada por IA)</button></section></body></html>",
                            "summary": "form antigo com objetivo principal",
                            "consistencyChecks": [
                              {"check":"FORM_USABILITY","status":"PASS","details":"objetivo principal obrigatório"}
                            ]
                          }
                        }
                        """,
                "{\"id\":\"resp_77\"}",
                "{\"model\":\"gpt-5.2\"}",
                120,
                80,
                null);

        service.completeJob(jobId, request);

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> persisted = mapper.readValue(experiment.getLandingPageHtml(), java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> landingPageHtml = (java.util.Map<String, Object>) persisted.get("landingPageHtml");
        String html = String.valueOf(landingPageHtml.get("htmlDocument")).toLowerCase();
        String summary = String.valueOf(landingPageHtml.get("summary")).toLowerCase();
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> checks = (java.util.List<java.util.Map<String, Object>>) landingPageHtml.get("consistencyChecks");

        assertTrue(html.contains("name=\"nome\""));
        assertTrue(html.contains("name=\"email\""));
        assertTrue(html.contains("name=\"whatsapp\""));
        assertTrue(html.contains("button type=\"submit\" form=\"lead-capture-primary\""));
        assertFalse(html.contains("name=\"objetivo\""));
        assertFalse(html.contains("objetivo principal"));
        assertTrue(summary.contains("wireframe.formspec"));
        assertTrue(checks.stream().anyMatch(check -> "FORM_USABILITY".equals(check.get("check"))));
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
    void completeJobSupportsLegacyFallbackWhenPlanningAndHtmlDoNotSendBindingKey() {
        Experiment experiment = buildLandingHtmlValidationExperiment(205L);
        experiment.setLandingPageImagePlanning("""
                {
                  "landingPageImagePlanning": {
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
                """
                        {
                          "landingPageHtml": {
                            "htmlDocument": %s,
                            "summary": "ok",
                            "consistencyChecks": [
                              {"check":"FORM_USABILITY","status":"PASS","details":"ok"}
                            ]
                          }
                        }
                        """.formatted(quoteJsonString(htmlDocument)),
                "{\"id\":\"resp_html\"}",
                "{\"model\":\"gpt-5.2\"}",
                120,
                80,
                null);
    }

    private String quoteJsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}
