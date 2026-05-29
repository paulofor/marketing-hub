package com.marketinghub.geralanding.wireframe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.geralanding.wireframe.service.pending.RecordWireframePending;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida as consultas de execução específicas da etapa wireframe. */
class BackendWireframeServiceTest {

    /** Deve registrar o início usando o código canônico da etapa wireframe. */
    @Test
    void startShouldRegisterInitialExecutionForWireframeStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendWireframeService service =
                new BackendWireframeService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(91L);
        when(experimentRepository.findById(91L)).thenReturn(Optional.of(experiment));
        when(executionRepository.save(any(GeraLandingStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeraLandingWireframeStartResponse response = service.start(91L);

        assertNotNull(response.idJob());
        assertEquals("INICIADO", response.status());
        verify(experimentRepository).findById(91L);
        verify(executionRepository).save(argThat(execution ->
                execution.getExperimentId().equals(91L)
                        && execution.getExperiment() == experiment
                        && execution.getStageCode().equals("landing-page-wireframe")
                        && execution.getStatus().equals("INICIADO")
                        && execution.getPromptTemplateId().equals("manual/start")
                        && execution.getIdJob() != null));
    }

    /** Deve buscar somente jobs iniciados da etapa wireframe para o endpoint interno pending. */
    @Test
    void listPendingShouldQueryStartedJobsForStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendWireframeService service =
                new BackendWireframeService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(77L);
        when(experiment.getName()).thenReturn("Experimento Wireframe");
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
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .createdAt(Instant.parse("2026-05-28T10:00:00Z"))
                .status("INICIADO")
                .idJob("job-77".getBytes(StandardCharsets.UTF_8))
                .build();
        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                "landing-page-wireframe", "INICIADO"))
                .thenReturn(List.of(execution));

        List<RecordWireframePending> response = service.listPending("landing-page-wireframe");

        assertEquals(1, response.size());
        assertEquals(77L, response.get(0).experimentId());
        assertEquals("job-77", response.get(0).jobid());
        assertEquals("landing-page-wireframe", response.get(0).stageCode());
        assertNotNull(response.get(0).experiment());
        assertEquals(77L, response.get(0).experiment().id());
        assertEquals("Experimento Wireframe", response.get(0).experiment().name());
        assertEquals("Hipótese de valor", response.get(0).experiment().hypothesis());
        assertEquals("Prompt texto", response.get(0).experiment().creativeTextPrompt());
        assertEquals("Prompt imagem", response.get(0).experiment().creativeImagePrompt());
        Map<?, ?> campaignAngle = (Map<?, ?>) response.get(0).experiment().campaignAngle();
        assertEquals("Promessa clara", ((Map<?, ?>) campaignAngle.get("campaignAngle")).get("singleMindedPromise"));
        Map<?, ?> adCopy = (Map<?, ?>) response.get(0).experiment().adCopy();
        assertEquals("Headline", ((Map<?, ?>) adCopy.get("adCopy")).get("headline"));
        assertEquals("Briefing imagem", response.get(0).experiment().adImageBriefing());
        Map<?, ?> landingPageCopy = (Map<?, ?>) response.get(0).experiment().landingPageCopy();
        assertEquals(
                "Hero",
                ((Map<?, ?>) ((Map<?, ?>) landingPageCopy.get("landingPageCopy")).get("hero")).get("headline"));
        Map<?, ?> landingPageWireframe = (Map<?, ?>) response.get(0).experiment().landingPageWireframe();
        assertEquals(List.of("hero"), ((Map<?, ?>) landingPageWireframe.get("landingPageWireframe")).get("sectionOrder"));
        assertEquals("Planejamento imagem", response.get(0).experiment().landingPageImagePlanning());
        assertEquals("Preset design", response.get(0).experiment().landingPageDesignPreset());
        assertEquals("Entregáveis landing", response.get(0).experiment().landingPageDeliverables());
        assertEquals("<html>GeraLanding</html>", response.get(0).experiment().htmlGeraLanding());
        assertNotNull(response.get(0).hypothesis());
        assertEquals(hypothesisId, response.get(0).hypothesis().id());
        assertEquals("Hipótese Framework", response.get(0).hypothesis().title());
        assertEquals("v1", response.get(0).hypothesis().framework().get("version"));
        assertEquals("Dor superficial", ((Map<?, ?>) response.get(0).hypothesis().framework().get("pain")).get("surface"));
        assertEquals("Resultado desejado", ((Map<?, ?>) response.get(0).hypothesis().framework().get("result")).get("desiredResult"));
        assertTrue(response.get(0).hypothesis().framework().containsKey("mechanism"));
        assertTrue(response.get(0).hypothesis().framework().containsKey("proof"));
        assertTrue(response.get(0).hypothesis().framework().containsKey("offer"));
        assertTrue(response.get(0).hypothesis().framework().containsKey("checklist"));
    }

    /** Deve persistir prompt, job OpenAI e status de espera quando o Worker AI informa o despacho. */
    @Test
    void markWaitingOpenAiDispatchShouldPersistPromptOpenAiJobAndWaitingStatus() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendWireframeService service =
                new BackendWireframeService(experimentRepository, executionRepository, new ObjectMapper());
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .idJob("job-ia-1".getBytes(StandardCharsets.UTF_8))
                .executionRequestedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .status("INICIADO")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(any(byte[].class)))
                .thenReturn(Optional.of(execution));

        service.markWaitingOpenAiDispatch("job-ia-1", "Prompt para IA", "openai-job-1");

        assertEquals("Prompt para IA", execution.getOpenAiRequestBody());
        assertEquals("openai-job-1", execution.getOpenAiJobId());
        assertNotNull(execution.getProcessingStartedAt());
        assertEquals("AGUARDANDO_RETORNO_OPENAI", execution.getStatus());
        verify(executionRepository).save(execution);
    }

    /** Deve concluir a execução e gravar o artefato wireframe recebido do Worker AI. */
    @Test
    void markCompletedFromResponseShouldPersistExecutionAndExperimentArtifact() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendWireframeService service =
                new BackendWireframeService(experimentRepository, executionRepository, new ObjectMapper());
        Experiment experiment = mock(Experiment.class);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(88L)
                .experiment(experiment)
                .stageCode("landing-page-wireframe")
                .idJob("job-ia-1".getBytes(StandardCharsets.UTF_8))
                .status("AGUARDANDO_RETORNO_OPENAI")
                .build();
        String modelResponse = "{\"landingPageWireframe\":{\"sectionOrder\":[\"hero\"]}}";
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(any(byte[].class)))
                .thenReturn(Optional.of(execution));

        service.markCompletedFromResponse(
                "job-ia-1",
                88L,
                "landing-page-wireframe",
                modelResponse,
                321,
                123,
                new BigDecimal("0.045600"),
                "openai-job-final");

        assertEquals(modelResponse, execution.getModelResponse());
        assertEquals("openai-job-final", execution.getOpenAiJobId());
        assertEquals(321, execution.getInputTokens());
        assertEquals(123, execution.getOutputTokens());
        assertEquals(new BigDecimal("0.045600"), execution.getCostUsd());
        assertNotNull(execution.getCompletedAt());
        assertEquals("CONCLUIDO", execution.getStatus());
        verify(executionRepository).save(execution);
        verify(experiment).setLandingPageWireframe(modelResponse);
        verify(experimentRepository).save(experiment);
        verify(experimentRepository, never()).findById(88L);
    }

}
