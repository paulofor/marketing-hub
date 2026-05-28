package com.marketinghub.geralanding.wireframe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.GeraLandingStageExecutionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida as consultas de execução específicas da etapa wireframe. */
class GeraLandingWireframeStageExecutionServiceTest {

    /** Deve buscar somente jobs iniciados da etapa wireframe para o endpoint interno pending. */
    @Test
    void listPendingShouldQueryStartedJobsForStage() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        GeraLandingStageExecutionRepository executionRepository = mock(GeraLandingStageExecutionRepository.class);
        GeraLandingWireframeStageExecutionService service =
                new GeraLandingWireframeStageExecutionService(experimentRepository, executionRepository);
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(77L)
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .createdAt(Instant.parse("2026-05-28T10:00:00Z"))
                .status("INICIADO")
                .idJob("job-77".getBytes(StandardCharsets.UTF_8))
                .build();
        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                "landing-page-wireframe", "INICIADO"))
                .thenReturn(List.of(execution));

        List<GeraLandingWireframePendingExecutionResponse> response = service.listPending("landing-page-wireframe");

        assertEquals(1, response.size());
        assertEquals(77L, response.get(0).experimentId());
        assertEquals("job-77", response.get(0).idJob());
        assertEquals("landing-page-wireframe", response.get(0).stageCode());
    }
}
