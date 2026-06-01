package com.marketinghub.geralanding.imageplanning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.geralanding.imageplanning.service.pending.RecordImagePlanningPending;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida as consultas de execução específicas da etapa image planning. */
class BackendImagePlanningServiceTest {

    /** Deve registrar o início usando o código canônico da etapa image planning. */
    @Test
    void startShouldRegisterInitialExecutionForImagePlanningStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendImagePlanningService service =
                new BackendImagePlanningService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(91L);
        when(experimentRepository.findById(91L)).thenReturn(Optional.of(experiment));
        when(executionRepository.save(any(GeraLandingStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeraLandingImagePlanningStartResponse response = service.start(91L);

        assertNotNull(response.idJob());
        assertEquals("INICIADO", response.status());
        verify(executionRepository).save(argThat(execution ->
                execution.getExperimentId().equals(91L)
                        && execution.getExperiment() == experiment
                        && execution.getStageCode().equals("landing-page-image-planning")
                        && execution.getStatus().equals("INICIADO")
                        && execution.getPromptTemplateId().equals("manual/start")
                        && execution.getIdJob() != null));
    }

    /** Deve buscar somente jobs iniciados da etapa image planning para o endpoint interno pending. */
    @Test
    void listPendingShouldQueryStartedJobsForStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendImagePlanningService service =
                new BackendImagePlanningService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(77L);
        when(experiment.getName()).thenReturn("Experimento Image Planning");
        when(experiment.getHypothesis()).thenReturn("Hipótese de valor");
        when(experiment.getCampaignAngle()).thenReturn("{\"campaignAngle\":{\"singleMindedPromise\":\"Promessa clara\"}}");
        when(experiment.getHypothesisRefIdForPending()).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(experiment.getHypothesisRefTitleForPending()).thenReturn("Hipótese Framework");
        when(experiment.getHypothesisFrameworkJsonForPending()).thenReturn("{\"pain\":{\"surface\":\"Dor\"}}");
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(77L)
                .experiment(experiment)
                .stageCode("landing-page-image-planning")
                .executionRequestedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .createdAt(Instant.parse("2026-05-28T10:00:00Z"))
                .status("INICIADO")
                .idJob("job-77".getBytes(StandardCharsets.UTF_8))
                .build();
        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                "landing-page-image-planning", "INICIADO"))
                .thenReturn(List.of(execution));

        List<RecordImagePlanningPending> pending = service.listPending("landing-page-image-planning");

        assertEquals(1, pending.size());
        assertEquals("job-77", pending.get(0).jobid());
        assertEquals("Experimento Image Planning", pending.get(0).experiment().name());
        assertTrue(pending.get(0).hypothesis().framework().containsKey("checklist"));
    }

    /** Deve concluir resposta com sucesso e persistir o artefato de image planning no experimento. */
    @Test
    void markCompletedFromResponseShouldPersistImagePlanningArtifact() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendImagePlanningService service =
                new BackendImagePlanningService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = new Experiment();
        experiment.setId(44L);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(44L)
                .experiment(experiment)
                .stageCode("landing-page-image-planning")
                .idJob("job-44".getBytes(StandardCharsets.UTF_8))
                .status("AGUARDANDO_RETORNO_OPENAI")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-44".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any(GeraLandingStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.markCompletedFromResponse(
                "job-44",
                44L,
                "landing-page-image-planning",
                "{\"landingPageImagePlanning\":{}}",
                "<html>preview</html>",
                120,
                80,
                new BigDecimal("0.012300"),
                "openai-job-1",
                null,
                null);

        assertEquals("CONCLUIDO", execution.getStatus());
        assertEquals("{\"landingPageImagePlanning\":{}}", experiment.getLandingPageImagePlanning());
        assertEquals("<html>preview</html>", experiment.getLandingPageHtml());
        verify(experimentRepository).save(experiment);
    }
}
