package com.marketinghub.geralanding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
