package com.marketinghub.geralanding.copy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.geralanding.copy.service.pending.RecordCopyPending;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida as consultas de execução específicas da etapa copy. */
class GeraLandingCopyStageExecutionServiceTest {

    /** Deve registrar o início usando o código canônico da etapa copy. */
    @Test
    void startShouldRegisterInitialExecutionForCopyStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        GeraLandingCopyStageExecutionService service =
                new GeraLandingCopyStageExecutionService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(91L);
        when(experimentRepository.findById(91L)).thenReturn(Optional.of(experiment));
        when(executionRepository.save(any(GeraLandingStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeraLandingCopyStartResponse response = service.start(91L);

        assertNotNull(response.idJob());
        assertEquals("INICIADO", response.status());
        verify(experimentRepository).findById(91L);
        verify(executionRepository).save(argThat(execution ->
                execution.getExperimentId().equals(91L)
                        && execution.getExperiment() == experiment
                        && execution.getStageCode().equals("landing-page-copy")
                        && execution.getStatus().equals("INICIADO")
                        && execution.getPromptTemplateId().equals("manual/start")
                        && execution.getIdJob() != null));
    }

    /** Deve buscar somente jobs iniciados da etapa copy para o endpoint interno pending. */
    @Test
    void listPendingShouldQueryStartedJobsForStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        GeraLandingCopyStageExecutionService service =
                new GeraLandingCopyStageExecutionService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(77L);
        when(experiment.getName()).thenReturn("Experimento Copy");
        when(experiment.getHypothesis()).thenReturn("Hipótese de valor");
        when(experiment.getCreativeTextPrompt()).thenReturn("Prompt texto");
        when(experiment.getCreativeImagePrompt()).thenReturn("Prompt imagem");
        when(experiment.getCampaignAngle()).thenReturn("{\"campaignAngle\":{\"singleMindedPromise\":\"Promessa clara\"}}");
        when(experiment.getAdCopy()).thenReturn("{\"adCopy\":{\"headline\":\"Headline\"}}");
        when(experiment.getAdImageBriefing()).thenReturn("Briefing imagem");
        when(experiment.getLandingPageCopy()).thenReturn("{\"landingPageCopy\":{\"hero\":{\"headline\":\"Hero\"}}}");
        when(experiment.getLandingPageWireframe()).thenReturn("{\"landingPageWireframe\":{\"sectionOrder\":[\"hero\"]}}");
        when(experiment.getLandingPageImagePlanning()).thenReturn("Planejamento imagem");
        when(experiment.getLandingPageDesignPreset()).thenReturn("Preset design");
        when(experiment.getLandingPageDeliverables()).thenReturn("Entregáveis landing");
        when(experiment.getHtmlGeraLanding()).thenReturn("<html>GeraLanding</html>");
        UUID hypothesisId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(experiment.getHypothesisRefIdForPending()).thenReturn(hypothesisId);
        when(experiment.getHypothesisRefTitleForPending()).thenReturn("Hipótese Framework");
        when(experiment.getHypothesisFrameworkJsonForPending()).thenReturn("""
                {
                  "version": "v1",
                  "pain": {"surface": "Dor superficial", "root": "Dor raiz"},
                  "result": {"desiredResult": "Resultado desejado"},
                  "mechanism": {"core": "Mecanismo central"},
                  "proof": {"type": "Prova"},
                  "offer": {"name": "Oferta"},
                  "checklist": {"painReady": true}
                }
                """);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(77L)
                .experiment(experiment)
                .stageCode("landing-page-copy")
                .executionRequestedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .createdAt(Instant.parse("2026-05-28T10:00:00Z"))
                .status("INICIADO")
                .idJob("job-77".getBytes(StandardCharsets.UTF_8))
                .build();
        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                "landing-page-copy", "INICIADO"))
                .thenReturn(List.of(execution));

        List<RecordCopyPending> pending = service.listPending("landing-page-copy");

        assertEquals(1, pending.size());
        assertEquals(77L, pending.get(0).experimentId());
        assertEquals("job-77", pending.get(0).jobid());
        assertEquals("job-77", pending.get(0).idJob());
        assertEquals("landing-page-copy", pending.get(0).stageCode());
        assertEquals("INICIADO", pending.get(0).status());
        assertEquals("Experimento Copy", pending.get(0).experiment().name());
        assertTrue(pending.get(0).experiment().campaignAngle() instanceof java.util.Map<?, ?>);
        assertEquals(hypothesisId, pending.get(0).hypothesis().id());
        assertEquals("Dor superficial", ((java.util.Map<?, ?>) pending.get(0).hypothesis().framework().get("pain")).get("surface"));
    }

    /** Deve persistir prompt, schema e request cru recebidos do worker. */
    @Test
    void markPromptReceivedShouldPersistPromptPayload() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        GeraLandingCopyStageExecutionService service =
                new GeraLandingCopyStageExecutionService(experimentRepository, executionRepository, new ObjectMapper());
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .idJob("job-copy".getBytes(StandardCharsets.UTF_8))
                .status("INICIADO")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-copy".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.markPromptReceived("job-copy", "prompt renderizado", "markdown bruto", "{}", "{\"model\":\"gpt\"}", "gpt-5.2", null);

        assertEquals("prompt renderizado", execution.getPrompt());
        assertEquals("markdown bruto", execution.getPromptMarkdownContent());
        assertEquals("{}", execution.getSchemaJson());
        assertEquals("{\"model\":\"gpt\"}", execution.getOpenAiRequestBody());
        assertEquals("gpt-5.2", execution.getOpenAiModel());
        assertEquals("INICIADO", execution.getStatus());
        verify(executionRepository).save(execution);
    }

    /** Deve concluir com sucesso, gravar métricas e persistir o artefato final de copy no experimento. */
    @Test
    void markCompletedFromResponseShouldPersistCopyArtifactOnSuccess() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        GeraLandingCopyStageExecutionService service =
                new GeraLandingCopyStageExecutionService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(88L)
                .experiment(experiment)
                .idJob("job-copy".getBytes(StandardCharsets.UTF_8))
                .stageCode("landing-page-copy")
                .status("AGUARDANDO_RETORNO_OPENAI")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-copy".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.markCompletedFromResponse(
                "job-copy", 88L, "landing-page-copy", "{\"landingPageCopy\":{}}", 12, 34,
                new BigDecimal("0.123456"), "openai-1", null, null);

        assertEquals("CONCLUIDO", execution.getStatus());
        verify(experiment).setLandingPageCopy("{\"landingPageCopy\":{}}");
        verify(experimentRepository).save(experiment);
    }

    /** Deve marcar falha sem sobrescrever o artefato final de copy no experimento. */
    @Test
    void markCompletedFromResponseShouldNotPersistCopyArtifactOnFailure() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        GeraLandingCopyStageExecutionService service =
                new GeraLandingCopyStageExecutionService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(88L)
                .experiment(experiment)
                .idJob("job-copy".getBytes(StandardCharsets.UTF_8))
                .stageCode("landing-page-copy")
                .status("AGUARDANDO_RETORNO_OPENAI")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-copy".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.markCompletedFromResponse("job-copy", 88L, "landing-page-copy", null, null, null, null, null, null, "erro técnico");

        assertEquals("FALHA", execution.getStatus());
        assertEquals("Falha ao processar etapa copy", execution.getErrorMessage());
        verify(experiment, never()).setLandingPageCopy(any());
        verify(experimentRepository, never()).save(experiment);
    }
}
