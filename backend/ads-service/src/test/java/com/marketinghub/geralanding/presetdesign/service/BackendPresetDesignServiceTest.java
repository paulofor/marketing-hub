package com.marketinghub.geralanding.presetdesign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.geralanding.presetdesign.provisorio.DesignPresetProvisionalHtmlAssembler;
import com.marketinghub.geralanding.presetdesign.service.detailStageExecution.RecordBackendPresetDesignDetalheDto;
import com.marketinghub.geralanding.presetdesign.service.listStageExecutions.GeraLandingPresetDesignExecutionSummaryResponse;
import com.marketinghub.geralanding.presetdesign.service.pending.RecordPresetDesignPending;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida as consultas de execução específicas da etapa design preset. */
class BackendPresetDesignServiceTest {

    /** Deve registrar o início usando o código canônico da etapa design preset. */
    @Test
    void startShouldRegisterInitialExecutionForDesignPresetStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendPresetDesignService service =
                new BackendPresetDesignService(experimentRepository, executionRepository, new ObjectMapper(), mock(DesignPresetProvisionalHtmlAssembler.class));
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(91L);
        when(experimentRepository.findById(91L)).thenReturn(Optional.of(experiment));
        when(executionRepository.save(any(GeraLandingStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeraLandingPresetDesignStartResponse response = service.start(91L);

        assertNotNull(response.idJob());
        assertEquals("INICIADO", response.status());
        verify(experimentRepository).findById(91L);
        verify(executionRepository).save(argThat(execution ->
                execution.getExperimentId().equals(91L)
                        && execution.getExperiment() == experiment
                        && execution.getStageCode().equals("landing-page-design-preset")
                        && execution.getStatus().equals("INICIADO")
                        && execution.getPromptTemplateId().equals("manual/start")
                        && execution.getIdJob() != null));
    }

    /** Deve buscar somente jobs iniciados da etapa design preset para o endpoint interno pending. */
    @Test
    void listPendingShouldQueryStartedJobsForStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendPresetDesignService service =
                new BackendPresetDesignService(experimentRepository, executionRepository, new ObjectMapper(), mock(DesignPresetProvisionalHtmlAssembler.class));
        Experiment experiment = mock(Experiment.class);
        when(experiment.getId()).thenReturn(77L);
        when(experiment.getName()).thenReturn("Experimento DesignPreset");
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
                .stageCode("landing-page-design-preset")
                .executionRequestedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .createdAt(Instant.parse("2026-05-28T10:00:00Z"))
                .status("INICIADO")
                .idJob("job-77".getBytes(StandardCharsets.UTF_8))
                .build();
        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                "landing-page-design-preset", "INICIADO"))
                .thenReturn(List.of(execution));

        List<RecordPresetDesignPending> pending = service.listPending("landing-page-design-preset");

        assertEquals(1, pending.size());
        assertEquals(77L, pending.get(0).experimentId());
        assertEquals("job-77", pending.get(0).jobid());
        assertEquals("landing-page-design-preset", pending.get(0).stageCode());
        assertEquals("Experimento DesignPreset", pending.get(0).experiment().name());
        assertTrue(pending.get(0).experiment().campaignAngle() instanceof java.util.Map<?, ?>);
        assertEquals(hypothesisId, pending.get(0).hypothesis().id());
        assertEquals("Dor superficial", ((java.util.Map<?, ?>) pending.get(0).hypothesis().framework().get("pain")).get("surface"));
    }

    /** Deve persistir prompt, schema e request cru recebidos do worker. */
    @Test
    void markPromptReceivedShouldPersistPromptPayload() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendPresetDesignService service =
                new BackendPresetDesignService(experimentRepository, executionRepository, new ObjectMapper(), mock(DesignPresetProvisionalHtmlAssembler.class));
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .idJob("job-design-preset".getBytes(StandardCharsets.UTF_8))
                .status("INICIADO")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-design-preset".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.markPromptReceived("job-design-preset", "prompt renderizado", "markdown bruto", "{}", "{\"model\":\"gpt\"}", "gpt-5.2", null);

        assertEquals("prompt renderizado", execution.getPrompt());
        assertEquals("markdown bruto", execution.getPromptMarkdownContent());
        assertEquals("{}", execution.getSchemaJson());
        assertEquals("{\"model\":\"gpt\"}", execution.getOpenAiRequestBody());
        assertEquals("gpt-5.2", execution.getOpenAiModel());
        assertEquals("INICIADO", execution.getStatus());
        verify(executionRepository).save(execution);
    }

    /** Deve concluir com sucesso, gravar métricas e persistir o artefato final de design preset no experimento. */
    @Test
    void markCompletedFromResponseShouldPersistDesignPresetArtifactOnSuccess() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        DesignPresetProvisionalHtmlAssembler htmlAssembler = mock(DesignPresetProvisionalHtmlAssembler.class);
        BackendPresetDesignService service =
                new BackendPresetDesignService(experimentRepository, executionRepository, new ObjectMapper(), htmlAssembler);
        Experiment experiment = mock(Experiment.class);
        when(experiment.getLandingPageWireframe()).thenReturn("{\"landingPageWireframe\":{}}");
        when(experiment.getLandingPageCopy()).thenReturn("{\"landingPageCopy\":{}}");
        when(experiment.getLandingPageImagePlanning()).thenReturn("{\"landingPageImagePlanning\":{}}");
        when(htmlAssembler.assemble(
                "{\"landingPageWireframe\":{}}",
                "{\"landingPageCopy\":{}}",
                "{\"landingPageImagePlanning\":{}}",
                "{\"landingPageDesignPreset\":{}}",
                "job-design-preset"))
                .thenReturn("<html>GeraLanding Design Preset</html>");
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(88L)
                .experiment(experiment)
                .idJob("job-design-preset".getBytes(StandardCharsets.UTF_8))
                .stageCode("landing-page-design-preset")
                .status("AGUARDANDO_RETORNO_OPENAI")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-design-preset".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.markCompletedFromResponse(
                "job-design-preset", 88L, "landing-page-design-preset", "{\"landingPageDesignPreset\":{}}", 12, 34,
                new BigDecimal("0.123456"), "openai-1", null, null);

        assertEquals("CONCLUIDO", execution.getStatus());
        verify(experiment).setLandingPageDesignPreset("{\"landingPageDesignPreset\":{}}");
        verify(htmlAssembler).assemble(
                "{\"landingPageWireframe\":{}}",
                "{\"landingPageCopy\":{}}",
                "{\"landingPageImagePlanning\":{}}",
                "{\"landingPageDesignPreset\":{}}",
                "job-design-preset");
        verify(experiment).setHtmlGeraLanding("<html>GeraLanding Design Preset</html>");
        assertEquals("<html>GeraLanding Design Preset</html>", execution.getProvisionalHtml());
        verify(experimentRepository, times(2)).save(experiment);
    }

    /** Deve marcar falha sem sobrescrever o artefato final de design preset no experimento. */
    @Test
    void markCompletedFromResponseShouldNotPersistDesignPresetArtifactOnFailure() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        BackendPresetDesignService service =
                new BackendPresetDesignService(experimentRepository, executionRepository, new ObjectMapper(), mock(DesignPresetProvisionalHtmlAssembler.class));
        Experiment experiment = mock(Experiment.class);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(88L)
                .experiment(experiment)
                .idJob("job-design-preset".getBytes(StandardCharsets.UTF_8))
                .stageCode("landing-page-design-preset")
                .status("AGUARDANDO_RETORNO_OPENAI")
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("job-design-preset".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.markCompletedFromResponse("job-design-preset", 88L, "landing-page-design-preset", null, null, null, null, null, null, "erro técnico");

        assertEquals("FALHA", execution.getStatus());
        assertEquals("Falha ao processar etapa design preset", execution.getErrorMessage());
        verify(experiment, never()).setLandingPageDesignPreset(any());
        verify(experimentRepository, never()).save(experiment);
    }
}
