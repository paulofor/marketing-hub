package com.marketinghub.geralanding.wireframe.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageExecutionService;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageService;
import com.marketinghub.geralanding.wireframe.service.RecordWireframePending;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida o contrato do controller de backend da etapa wireframe. */
class BackendWireframeControllerTest {

    /** Deve delegar a listagem pendente para a etapa canônica de wireframe. */
    @Test
    void pendingShouldReturnStartedWireframeJobs() {
        GeraLandingWireframeStageService stageService = mock(GeraLandingWireframeStageService.class);
        GeraLandingWireframeStageExecutionService executionService = mock(GeraLandingWireframeStageExecutionService.class);
        BackendWireframeController controller = new BackendWireframeController(stageService, executionService);
        List<RecordWireframePending> pending = List.of(
                new RecordWireframePending(12L, "job-12", "landing-page-wireframe"));
        when(executionService.listPending("landing-page-wireframe")).thenReturn(pending);

        List<RecordWireframePending> response = controller.pending();

        assertEquals(pending, response);
        verify(executionService).listPending("landing-page-wireframe");
    }
}
