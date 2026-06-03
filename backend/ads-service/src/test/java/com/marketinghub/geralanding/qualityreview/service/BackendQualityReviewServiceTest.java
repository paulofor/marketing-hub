package com.marketinghub.geralanding.qualityreview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Valida a execução determinística do Quality Gate da landing gerada. */
class BackendQualityReviewServiceTest {

    /** Deve aprovar uma landing com sinais comerciais completos e persistir o diagnóstico no experimento. */
    @Test
    void startShouldPersistApprovedQualityReviewWhenLandingHasCommercialSignals() throws Exception {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, objectMapper);
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(36L);
        when(experiment.getHtmlGeraLanding()).thenReturn("""
                <html><body><section>Para profissional MEI, remova a dor da rotina e conquiste resultado com transformação clara.</section>
                <section>Nosso método em passo a passo entrega diagnóstico, plano e checklist com preview antes e depois.</section>
                <form><input type=\"email\" name=\"email\"><button>Quero receber meu plano</button></form></body></html>
                """);
        when(experiment.getLandingPageCopy()).thenReturn("Dor, resultado, mecanismo, prova e oferta para cliente específico.");
        when(experimentRepository.findById(36L)).thenReturn(Optional.of(experiment));
        when(executionRepository.save(any(GeraLandingStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeraLandingQualityReviewStartResponse response = service.start(36L);

        JsonNode review = objectMapper.readTree(response.qualityReview());
        assertEquals("CONCLUIDO", response.status());
        assertEquals("APPROVE_FOR_PUBLICATION", review.get("approvalRecommendation").asText());
        assertTrue(review.get("score").asInt() >= 80);
        verify(experiment).setLandingPageQualityReview(response.qualityReview());
        verify(executionRepository, times(2)).save(argThat(execution ->
                execution.getStageCode().equals("landing-page-quality-review")
                        && execution.getStatus().equals("CONCLUIDO")
                        && execution.getIdJob() != null));
    }

    /** Deve bloquear uma landing fraca e recomendar explicitamente as etapas de regeneração necessárias. */
    @Test
    void reviewAfterHtmlGenerationShouldRecommendRegenerationForWeakLanding() throws Exception {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, objectMapper);
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(37L);
        when(experiment.getHtmlGeraLanding()).thenReturn("<html><body><h1>Material digital</h1><!-- AUTO: debug --></body></html>");
        when(executionRepository.save(any(GeraLandingStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String qualityReview = service.reviewAfterHtmlGeneration(experiment);

        JsonNode review = objectMapper.readTree(qualityReview);
        assertEquals("REGENERATE_BEFORE_PUBLICATION", review.get("approvalRecommendation").asText());
        assertTrue(review.get("score").asInt() < 80);
        assertTrue(review.get("blockingIssues").toString().contains("metadado técnico"));
        assertTrue(review.get("recommendedRegeneration").toString().contains("LANDING_PAGE_COPY"));
        assertTrue(review.get("recommendedRegeneration").toString().contains("LANDING_PAGE_HTML"));
        verify(experiment).setLandingPageQualityReview(qualityReview);
    }

    /** Deve retornar detalhes de execução já persistidos usando o id textual do job. */
    @Test
    void getStageExecutionDetailShouldReturnPersistedQualityReviewExecution() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendQualityReviewService service = new BackendQualityReviewService(experimentRepository, executionRepository, new ObjectMapper());
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(38L)
                .stageCode("landing-page-quality-review")
                .status("CONCLUIDO")
                .idJob("job-quality".getBytes(StandardCharsets.UTF_8))
                .modelResponse("{\"score\":90}")
                .build();
        when(executionRepository.findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(
                38L,
                "job-quality".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        var detail = service.getStageExecutionDetail(38L, "job-quality");

        assertEquals("job-quality", detail.idJob());
        assertEquals("landing-page-quality-review", detail.stageCode());
        assertEquals("{\"score\":90}", detail.modelResponse());
    }
}
