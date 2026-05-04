package com.marketinghub.geralanding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeraLandingInternalController.class)
class GeraLandingInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeraLandingStageExecutionService executionService;

    @Test
    void shouldAcceptWorkerPromptPayload() throws Exception {
        String payload = """
                {
                  "experimentId": 77,
                  "stageCode": "landing-page-wireframe",
                  "executionId": "exec-2026-05-03-01",
                  "promptContent": "prompt montado"
                }
                """;

        mockMvc.perform(post("/api/internal/geralanding/stage-executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        verify(executionService).registerWorkerPromptExecution(any(GeraLandingWorkerPromptRequest.class));
    }

    @Test
    void shouldRejectWhenPromptContentIsMissing() throws Exception {
        String payload = """
                {
                  "experimentId": 77,
                  "stageCode": "landing-page-wireframe",
                  "executionId": "exec-2026-05-03-01"
                }
                """;

        mockMvc.perform(post("/api/internal/geralanding/stage-executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListPendingExecutions() throws Exception {
        UUID idJob = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(executionService.listPendingExecutions())
                .thenReturn(List.of(new GeraLandingPendingExecutionResponse(77L, idJob, "landing-page-wireframe")));

        mockMvc.perform(get("/api/internal/geralanding/stage-executions/pending"))
                .andExpect(status().isOk())
                 .andExpect(jsonPath("$[0].experimentId").value(77))
                .andExpect(jsonPath("$[0].idJob").value(idJob.toString()))
                .andExpect(jsonPath("$[0].stageCode").value("landing-page-wireframe"));
    }
}
