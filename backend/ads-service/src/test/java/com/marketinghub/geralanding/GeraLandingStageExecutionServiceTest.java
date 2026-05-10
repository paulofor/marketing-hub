package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeraLandingStageExecutionServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private GeraLandingStageExecutionRepository executionRepository;

    @Mock
    private WireframeProvisionalHtmlAssembler wireframeProvisionalHtmlAssembler;

    @Mock
    private CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler;

    @InjectMocks
    private GeraLandingStageExecutionService service;

    @Test
    void shouldFallbackToExperimentAndStageWhenLookupByIdJobFails() {
        GeraLandingPromptReceiveRequest request =
                new GeraLandingPromptReceiveRequest(19L, "landing-page-wireframe", "prompt final", null, null, null, null, null);

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(19L)
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(Instant.parse("2026-05-04T00:00:00Z"))
                .createdAt(Instant.parse("2026-05-04T00:00:00Z"))
                .status("INICIADO")
                .idJob("real-id-job".getBytes(StandardCharsets.UTF_8))
                .promptContent("prompt base")
                .build();

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("id-corrompido".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.empty());
        when(executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(19L,
                "landing-page-wireframe")).thenReturn(Optional.of(execution));

        service.receivePrompt("id-corrompido", request);

        ArgumentCaptor<GeraLandingStageExecution> captor = ArgumentCaptor.forClass(GeraLandingStageExecution.class);
        verify(executionRepository).save(captor.capture());
        assertEquals("AGUARDANDO_RETORNO_OPENAI", captor.getValue().getStatus());
        assertEquals("prompt final", captor.getValue().getPrompt());
    }

    @Test
    void shouldUseIdJobLookupWhenItExists() {
        GeraLandingPromptReceiveRequest request =
                new GeraLandingPromptReceiveRequest(19L, "landing-page-wireframe", "prompt final", null, null, null, null, null);

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(19L)
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(Instant.parse("2026-05-04T00:00:00Z"))
                .createdAt(Instant.parse("2026-05-04T00:00:00Z"))
                .status("INICIADO")
                .idJob("id-ok".getBytes(StandardCharsets.UTF_8))
                .promptContent("prompt base")
                .build();

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("id-ok".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.receivePrompt("id-ok", request);

        verify(executionRepository, never()).findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(any(), any());
        verify(executionRepository).save(any(GeraLandingStageExecution.class));
    }

    @Test
    void shouldPersistLandingPageWireframeOnExperimentWhenWireframeResultArrives() {
        GeraLandingResultReceiveRequest request = new GeraLandingResultReceiveRequest(
                31L,
                "landing-page-wireframe",
                "{\"landingPageWireframe\":{\"sectionOrder\":[]}}",
                null,
                null,
                null,
                "job-openai-1",
                100,
                200,
                null);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(31L)
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(Instant.parse("2026-05-04T00:00:00Z"))
                .createdAt(Instant.parse("2026-05-04T00:00:00Z"))
                .status("EM_PROCESSAMENTO")
                .idJob("id-ok".getBytes(StandardCharsets.UTF_8))
                .build();
        Experiment experiment = new Experiment();
        experiment.setId(31L);

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("id-ok".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));
        when(experimentRepository.findById(31L)).thenReturn(Optional.of(experiment));
        when(wireframeProvisionalHtmlAssembler.assemble(request.modelResponse(), "id-ok")).thenReturn("<html>provisorio</html>");

        service.receiveResult("id-ok", request);

        assertEquals("{\"landingPageWireframe\":{\"sectionOrder\":[]}}", experiment.getLandingPageWireframe());
        assertEquals("<html>provisorio</html>", execution.getProvisionalHtml());
        assertTrue(Arrays.equals("id-ok".getBytes(StandardCharsets.UTF_8), experiment.getLandingPageWireframeJobId()));
        verify(experimentRepository).save(experiment);
    }

    @Test
    void shouldPersistLandingPageCopyOnExperimentWhenCopyResultArrives() {
        GeraLandingResultReceiveRequest request = new GeraLandingResultReceiveRequest(
                44L,
                "landing-page-copy",
                "{\"landingPageCopy\":{\"hero\":{}}}",
                null,
                null,
                null,
                "job-openai-copy",
                90,
                140,
                null);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(44L)
                .stageCode("landing-page-copy")
                .executionRequestedAt(Instant.parse("2026-05-04T00:00:00Z"))
                .createdAt(Instant.parse("2026-05-04T00:00:00Z"))
                .status("EM_PROCESSAMENTO")
                .idJob("id-copy".getBytes(StandardCharsets.UTF_8))
                .build();
        Experiment experiment = new Experiment();
        experiment.setId(44L);

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("id-copy".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));
        when(experimentRepository.findById(44L)).thenReturn(Optional.of(experiment));
        when(copyProvisionalHtmlAssembler.assemble(request.modelResponse(), null, "id-copy")).thenReturn("<html>provisorio</html>");

        service.receiveResult("id-copy", request);

        assertEquals("{\"landingPageCopy\":{\"hero\":{}}}", experiment.getLandingPageCopy());
        assertEquals("<html>provisorio</html>", execution.getProvisionalHtml());
        assertTrue(Arrays.equals("id-copy".getBytes(StandardCharsets.UTF_8), experiment.getLandingPageCopyJobId()));
        verify(experimentRepository).save(experiment);
    }

    @Test
    void shouldMarkExecutionAsProcessingWhenDispatchIsReceived() {
        GeraLandingDispatchReceiveRequest request =
                new GeraLandingDispatchReceiveRequest(19L, "landing-page-wireframe", "openai-job-123");

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(19L)
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(Instant.parse("2026-05-04T00:00:00Z"))
                .createdAt(Instant.parse("2026-05-04T00:00:00Z"))
                .status("AGUARDANDO_RETORNO_OPENAI")
                .idJob("id-dispatch".getBytes(StandardCharsets.UTF_8))
                .build();

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("id-dispatch".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));

        service.markAsSentToOpenAi("id-dispatch", request);

        assertEquals("EM_PROCESSAMENTO", execution.getStatus());
        assertEquals("openai-job-123", execution.getOpenAiJobId());
        verify(executionRepository).save(execution);
    }

    @Test
    void shouldMarkExecutionAsFailureAndSkipExperimentPersistenceWhenErrorMessageExists() {
        GeraLandingResultReceiveRequest request = new GeraLandingResultReceiveRequest(
                31L,
                "landing-page-wireframe",
                null,
                null,
                "erro no batch",
                "detalhe tecnico",
                "job-openai-erro",
                null,
                null,
                null);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(31L)
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(Instant.parse("2026-05-04T00:00:00Z"))
                .createdAt(Instant.parse("2026-05-04T00:00:00Z"))
                .status("EM_PROCESSAMENTO")
                .idJob("id-falha".getBytes(StandardCharsets.UTF_8))
                .build();

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("id-falha".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));
        when(wireframeProvisionalHtmlAssembler.assemble(null, "id-falha")).thenReturn("<html>fallback</html>");

        service.receiveResult("id-falha", request);

        assertEquals("FALHA", execution.getStatus());
        assertEquals("erro no batch", execution.getErrorMessage());
        assertEquals("detalhe tecnico", execution.getErrorDetail());
        assertEquals("<html>fallback</html>", execution.getProvisionalHtml());
        verify(experimentRepository, times(0)).save(any(Experiment.class));
    }
}
