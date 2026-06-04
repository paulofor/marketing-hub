package com.marketinghub.geralanding.qualityreview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Valida a execução assíncrona do Quality Gate visual da landing gerada. */
class BackendQualityReviewServiceTest {

    /** Deve registrar uma execução iniciada para o Worker AI avaliar a landing com modelo de visão. */
    @Test
    void startShouldCreatePendingQualityReviewExecution() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(36L);
        when(experimentRepository.findById(36L)).thenReturn(Optional.of(experiment));
        when(executionRepository.save(any(GeraLandingStageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GeraLandingQualityReviewStartResponse response = service.start(36L);

        assertEquals("INICIADO", response.status());
        assertNull(response.qualityReview());
        verify(executionRepository).save(argThat(execution ->
                execution.getStageCode().equals("landing-page-quality-review")
                        && execution.getStatus().equals("INICIADO")
                        && execution.getIdJob() != null));
    }

    /** Deve expor jobs pendentes com artefatos canônicos para o processamento visual pelo Worker AI. */
    @Test
    void listPendingShouldExposeExperimentArtifactsForVisionWorker() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(37L);
        when(experiment.getName()).thenReturn("Experimento qualidade");
        when(experiment.getLandingPageImageAssets()).thenReturn("{\"images\":[{\"url\":\"https://cdn.test/img.png\"}]}");
        when(experiment.getHtmlGeraLanding()).thenReturn("<html><body>Landing</body></html>");
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(37L)
                .experiment(experiment)
                .stageCode("landing-page-quality-review")
                .status("INICIADO")
                .idJob("job-quality".getBytes(StandardCharsets.UTF_8))
                .build();
        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc("landing-page-quality-review", "INICIADO"))
                .thenReturn(List.of(execution));

        var pending = service.listPending();

        assertEquals(1, pending.size());
        assertEquals("job-quality", pending.get(0).jobid());
        assertEquals("<html><body>Landing</body></html>", pending.get(0).experiment().htmlGeraLanding());
    }

    /** Deve persistir o diagnóstico visual no experimento quando o Worker AI conclui a resposta. */
    @Test
    void markCompletedFromResponseShouldPersistQualityReviewOnExperiment() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(38L)
                .experiment(experiment)
                .stageCode("landing-page-quality-review")
                .status("AGUARDANDO_RETORNO_OPENAI")
                .idJob("job-quality".getBytes(StandardCharsets.UTF_8))
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-quality".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any(GeraLandingStageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markCompletedFromResponse(
                "job-quality",
                38L,
                "landing-page-quality-review",
                "{\"score\":90}",
                100,
                50,
                BigDecimal.valueOf(0.01),
                "resp_123",
                null,
                null);

        assertEquals("CONCLUIDO", execution.getStatus());
        verify(experiment).setLandingPageQualityReview("{\"score\":90}");
        verify(experimentRepository).save(experiment);
        verify(executionRepository, times(1)).save(execution);
    }



    /** Deve sinalizar quando a mesma evidência visual recebe decisões divergentes em execuções diferentes. */
    @Test
    void markCompletedFromResponseShouldFlagContradictoryDecisionForSameEvidence() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        String auditJson = "{\"landingHtmlSha256\":\"html-a\",\"screenshots\":[{\"viewport\":\"mobile\",\"sha256\":\"img-m\"},{\"viewport\":\"desktop\",\"sha256\":\"img-d\"}]}";
        GeraLandingStageExecution previous = GeraLandingStageExecution.builder()
                .experimentId(38L)
                .stageCode("landing-page-quality-review")
                .status("CONCLUIDO")
                .idJob("job-previous".getBytes(StandardCharsets.UTF_8))
                .qualityReviewAudit(auditJson)
                .modelResponse("{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\"}")
                .build();
        GeraLandingStageExecution current = GeraLandingStageExecution.builder()
                .experimentId(38L)
                .experiment(experiment)
                .stageCode("landing-page-quality-review")
                .status("AGUARDANDO_RETORNO_OPENAI")
                .idJob("job-current".getBytes(StandardCharsets.UTF_8))
                .qualityReviewAudit(auditJson)
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-current".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(current));
        when(executionRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(38L, "landing-page-quality-review"))
                .thenReturn(List.of(current, previous));
        when(executionRepository.save(any(GeraLandingStageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markCompletedFromResponse(
                "job-current",
                38L,
                "landing-page-quality-review",
                "{\"approvalRecommendation\":\"REGENERATE_BEFORE_PUBLICATION\"}",
                100,
                50,
                BigDecimal.valueOf(0.01),
                "resp_123",
                null,
                null);

        assertTrue(current.getQualityReviewAudit().contains("\"evidenceReuseDetected\":true"));
        assertTrue(current.getQualityReviewAudit().contains("\"contradictoryDecisionDetected\":true"));
        assertTrue(current.getQualityReviewAudit().contains("job-previous"));
    }


    /** Deve retornar detalhes de execução já persistidos usando o id textual do job. */
    @Test
    void getStageExecutionDetailShouldReturnPersistedQualityReviewExecution() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, new ObjectMapper());
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(39L)
                .stageCode("landing-page-quality-review")
                .status("CONCLUIDO")
                .idJob("job-quality".getBytes(StandardCharsets.UTF_8))
                .modelResponse("{\"score\":90}")
                .build();
        when(executionRepository.findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(
                39L,
                "job-quality".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        var detail = service.getStageExecutionDetail(39L, "job-quality");

        assertEquals("job-quality", detail.idJob());
        assertEquals("landing-page-quality-review", detail.stageCode());
        assertEquals("{\"score\":90}", detail.modelResponse());
    }
}
