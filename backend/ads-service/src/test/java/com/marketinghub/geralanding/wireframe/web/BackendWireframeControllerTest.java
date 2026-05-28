package com.marketinghub.geralanding.wireframe.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageExecutionService;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageService;
import com.marketinghub.geralanding.wireframe.service.RecordWireframeExperiment;
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
                new RecordWireframePending(
                        12L,
                        "job-12",
                        "landing-page-wireframe",
                        new RecordWireframeExperiment(12L, "Experimento 12", "Hipótese", "PLANNED", "LANDING")));
        when(executionService.listPending("landing-page-wireframe")).thenReturn(pending);

        List<RecordWireframePending> response = controller.pending();

        assertEquals(pending, response);
        verify(executionService).listPending("landing-page-wireframe");
    }

    /** Deve serializar pending como lista com atributos experiment e jobid em cada item. */
    @Test
    void pendingShouldSerializeListItemsWithExperimentAndJobid() throws Exception {
        GeraLandingWireframeStageService stageService = mock(GeraLandingWireframeStageService.class);
        GeraLandingWireframeStageExecutionService executionService = mock(GeraLandingWireframeStageExecutionService.class);
        BackendWireframeController controller = new BackendWireframeController(stageService, executionService);
        when(executionService.listPending("landing-page-wireframe")).thenReturn(List.of(
                new RecordWireframePending(
                        33L,
                        "bbed57d0-dcc7-40ab-b936-20a19e21c7fe",
                        "landing-page-wireframe",
                        new RecordWireframeExperiment(33L, "Experimento 33", "Hipótese 33", "PLANNED", "LANDING"))));

        JsonNode json = new ObjectMapper().valueToTree(controller.pending());

        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertTrue(json.get(0).has("experiment"));
        assertTrue(json.get(0).has("jobid"));
        assertEquals("bbed57d0-dcc7-40ab-b936-20a19e21c7fe", json.get(0).get("jobid").asText());
        assertEquals(33L, json.get(0).get("experiment").get("id").asLong());
    }
}
