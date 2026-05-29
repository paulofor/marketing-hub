package com.marketinghub.worker.geralanding.wireframe.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.worker.geralanding.wireframe.backend.GeraLandingWireframeBackendClient;
import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Responsável por validar o consumo da fila pending estruturada da etapa wireframe.
 */
@ExtendWith(MockitoExtension.class)
class WireframePendingJobsServiceTest {
    @Mock
    private GeraLandingWireframeBackendClient backendClient;

    /**
     * Deve usar apenas a lista pending estruturada e filtrar a etapa wireframe sem chamadas adicionais de detalhe.
     */
    @Test
    void listPendingWireframeJobsShouldUseOnlyStructuredPendingList() {
        GeraLandingStageExecutionDetailDto wireframe = new GeraLandingStageExecutionDetailDto(
                34L,
                "landing-page-wireframe",
                "job-wireframe",
                "INICIADO",
                null,
                null,
                null,
                null,
                Map.of("campaignAngle", Map.of("primaryPromise", "Agenda cheia")));
        GeraLandingStageExecutionDetailDto copy = new GeraLandingStageExecutionDetailDto(
                35L,
                "landing-page-copy",
                "job-copy",
                "INICIADO",
                null,
                null,
                null,
                null,
                Map.of());
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(wireframe, copy));
        WireframePendingJobsService service = new WireframePendingJobsService(backendClient);

        List<GeraLandingStageExecutionDetailDto> jobs = service.listPendingWireframeJobs(20);

        assertThat(jobs).containsExactly(wireframe);
        assertThat(jobs.getFirst().promptData()).containsKey("campaignAngle");
        verify(backendClient).listPendingExecutions(20);
        verifyNoMoreInteractions(backendClient);
    }
}
