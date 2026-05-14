package com.marketinghub.geralanding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeraLandingContoller.class)
class GeraLandingContollerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeraLandingStageExecutionService executionService;

    @Test
    void shouldCreateExecutionAndReturnCodeAndStatus() throws Exception {
        when(executionService.registerInitialExecution(99L, "landing-page-wireframe"))
                .thenReturn(new GeraLandingStartResponse("job-123", "INICIADO"));

        mockMvc.perform(post("/api/experiments/{experimentId}/geralanding/wireframe/start", 99L))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idJob").value("job-123"))
                .andExpect(jsonPath("$.status").value("INICIADO"));

        verify(executionService).registerInitialExecution(eq(99L), eq("landing-page-wireframe"));
    }


    @Test
    void shouldCreateCopyExecutionAndReturnCodeAndStatus() throws Exception {
        when(executionService.registerInitialExecution(99L, "landing-page-copy"))
                .thenReturn(new GeraLandingStartResponse("job-copy-1", "INICIADO"));

        mockMvc.perform(post("/api/experiments/{experimentId}/geralanding/copy/start", 99L))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idJob").value("job-copy-1"))
                .andExpect(jsonPath("$.status").value("INICIADO"));

        verify(executionService).registerInitialExecution(eq(99L), eq("landing-page-copy"));
    }


    @Test
    void shouldCreateImagePromptsExecutionAndReturnCodeAndStatus() throws Exception {
        when(executionService.registerInitialExecution(99L, "landing-page-image-planning"))
                .thenReturn(new GeraLandingStartResponse("job-image-1", "INICIADO"));

        mockMvc.perform(post("/api/experiments/{experimentId}/geralanding/image-prompts/start", 99L))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idJob").value("job-image-1"))
                .andExpect(jsonPath("$.status").value("INICIADO"));

        verify(executionService).registerInitialExecution(eq(99L), eq("landing-page-image-planning"));
    }

    @Test
    void shouldGenerateAndPersistProvisionalHtml() throws Exception {
        when(executionService.generateAndPersistProvisionalHtmlFromExperiment(99L))
                .thenReturn("<html>preview</html>");

        mockMvc.perform(post("/api/experiments/{experimentId}/geralanding/html/provisional/generate", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisionalHtml").value("<html>preview</html>"));

        verify(executionService).generateAndPersistProvisionalHtmlFromExperiment(eq(99L));
    }
    @Test
    void shouldListStageExecutionsOrderedByMostRecentFirst() throws Exception {
        when(executionService.listExperimentStageExecutions(99L, "landing-page-wireframe", true))
                .thenReturn(List.of(
                        new GeraLandingExecutionSummaryResponse("job-2", "EM_PROCESSAMENTO", Instant.parse("2026-05-05T00:40:00Z"), new BigDecimal("0.1265")),
                        new GeraLandingExecutionSummaryResponse("job-1", "INICIADO", Instant.parse("2026-05-05T00:35:00Z"), null)));

        mockMvc.perform(get("/api/experiments/{experimentId}/geralanding/stage-executions", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idJob").value("job-2"))
                .andExpect(jsonPath("$[0].status").value("EM_PROCESSAMENTO"))
                .andExpect(jsonPath("$[0].costUsd").value(0.1265))
                .andExpect(jsonPath("$[1].idJob").value("job-1"));

        verify(executionService).listExperimentStageExecutions(eq(99L), eq("landing-page-wireframe"), eq(true));
    }

    @Test
    void shouldListOnlyNonCompletedExecutionsWhenRequested() throws Exception {
        when(executionService.listExperimentStageExecutions(99L, "landing-page-wireframe", false))
                .thenReturn(List.of(
                        new GeraLandingExecutionSummaryResponse("job-2", "EM_PROCESSAMENTO", Instant.parse("2026-05-05T00:40:00Z"), new BigDecimal("0.0089"))));

        mockMvc.perform(get("/api/experiments/{experimentId}/geralanding/stage-executions", 99L)
                        .param("includeCompleted", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idJob").value("job-2"))
                .andExpect(jsonPath("$[0].status").value("EM_PROCESSAMENTO"))
                .andExpect(jsonPath("$[0].costUsd").value(0.0089));

        verify(executionService).listExperimentStageExecutions(eq(99L), eq("landing-page-wireframe"), eq(false));
    }
}
