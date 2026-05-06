package com.marketinghub.geralanding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeraLandingWireframeController.class)
class GeraLandingWireframeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeraLandingStageExecutionService executionService;

    @Test
    void shouldCreateExecutionAndReturnCodeAndStatus() throws Exception {
        when(executionService.registerInitialExecution(99L))
                .thenReturn(new GeraLandingStartResponse("job-123", "INICIADO"));

        mockMvc.perform(post("/api/experiments/{experimentId}/geralanding/wireframe/start", 99L))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idJob").value("job-123"))
                .andExpect(jsonPath("$.status").value("INICIADO"));

        verify(executionService).registerInitialExecution(eq(99L));
    }

    @Test
    void shouldListStageExecutionsOrderedByMostRecentFirst() throws Exception {
        when(executionService.listExperimentStageExecutions(99L, "landing-page-wireframe", true))
                .thenReturn(List.of(
                        new GeraLandingExecutionSummaryResponse("job-2", "EM_PROCESSAMENTO", Instant.parse("2026-05-05T00:40:00Z")),
                        new GeraLandingExecutionSummaryResponse("job-1", "INICIADO", Instant.parse("2026-05-05T00:35:00Z"))));

        mockMvc.perform(get("/api/experiments/{experimentId}/geralanding/stage-executions", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idJob").value("job-2"))
                .andExpect(jsonPath("$[0].status").value("EM_PROCESSAMENTO"))
                .andExpect(jsonPath("$[1].idJob").value("job-1"));

        verify(executionService).listExperimentStageExecutions(eq(99L), eq("landing-page-wireframe"), eq(true));
    }

    @Test
    void shouldListOnlyNonCompletedExecutionsWhenRequested() throws Exception {
        when(executionService.listExperimentStageExecutions(99L, "landing-page-wireframe", false))
                .thenReturn(List.of(
                        new GeraLandingExecutionSummaryResponse("job-2", "EM_PROCESSAMENTO", Instant.parse("2026-05-05T00:40:00Z"))));

        mockMvc.perform(get("/api/experiments/{experimentId}/geralanding/stage-executions", 99L)
                        .param("includeCompleted", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idJob").value("job-2"))
                .andExpect(jsonPath("$[0].status").value("EM_PROCESSAMENTO"));

        verify(executionService).listExperimentStageExecutions(eq(99L), eq("landing-page-wireframe"), eq(false));
    }
}
