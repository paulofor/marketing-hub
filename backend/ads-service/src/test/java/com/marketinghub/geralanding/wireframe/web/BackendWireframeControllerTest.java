package com.marketinghub.geralanding.wireframe.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframePendingExecutionResponse;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageExecutionService;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Valida o contrato do controller de backend da etapa wireframe. */
class BackendWireframeControllerTest {

    /** Deve delegar a listagem pendente para a etapa canônica de wireframe. */
    @Test
    void pendingShouldReturnStartedWireframeJobs() {
        GeraLandingWireframeStageService stageService = mock(GeraLandingWireframeStageService.class);
        GeraLandingWireframeStageExecutionService executionService = mock(GeraLandingWireframeStageExecutionService.class);
        BackendWireframeController controller = new BackendWireframeController(stageService, executionService);
        List<GeraLandingWireframePendingExecutionResponse> pending = List.of(
                new GeraLandingWireframePendingExecutionResponse(12L, "job-12", "landing-page-wireframe"));
        when(executionService.listPending("landing-page-wireframe")).thenReturn(pending);

        ResponseEntity<List<GeraLandingWireframePendingExecutionResponse>> response = controller.pending();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(pending, response.getBody());
        verify(executionService).listPending("landing-page-wireframe");
    }
}
