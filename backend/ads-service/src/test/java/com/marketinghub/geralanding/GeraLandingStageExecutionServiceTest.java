package com.marketinghub.geralanding;

import com.marketinghub.experiment.repository.ExperimentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeraLandingStageExecutionServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private GeraLandingStageExecutionRepository executionRepository;

    @InjectMocks
    private GeraLandingStageExecutionService service;

    @Test
    void shouldFallbackToExperimentAndStageWhenLookupByIdJobFails() {
        GeraLandingPromptReceiveRequest request =
                new GeraLandingPromptReceiveRequest(19L, "landing-page-wireframe", "prompt final");

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
        assertEquals("EM_PROCESSAMENTO", captor.getValue().getStatus());
        assertEquals("prompt final", captor.getValue().getPrompt());
    }

    @Test
    void shouldUseIdJobLookupWhenItExists() {
        GeraLandingPromptReceiveRequest request =
                new GeraLandingPromptReceiveRequest(19L, "landing-page-wireframe", "prompt final");

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
}
