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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
