package com.marketinghub.geralanding;

import com.marketinghub.experiment.pipeline.service.ExperimentPipelineGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeraLandingWireframeController.class)
class GeraLandingWireframeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExperimentPipelineGenerationService generationService;

    @MockBean
    private GeraLandingStageExecutionService executionService;

    @Test
    void shouldAcceptValidPayload() throws Exception {
        String payload = """
                {
                  "stageCode": "LANDING_PAGE_WIREFRAME",
                  "prompt": {
                    "templateId": "geralanding/wireframe/v1",
                    "content": "prompt final"
                  }
                }
                """;

        mockMvc.perform(post("/api/experiments/{experimentId}/geralanding/wireframe/start", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        verify(executionService).register(eq(99L), any(GeraLandingStageStartRequest.class));
        verify(generationService).generate(eq(99L), any(), any());
    }

    @Test
    void shouldRejectPayloadWithoutPromptContent() throws Exception {
        String payload = """
                {
                  "stageCode": "LANDING_PAGE_WIREFRAME",
                  "prompt": {
                    "templateId": "geralanding/wireframe/v1"
                  }
                }
                """;

        mockMvc.perform(post("/api/experiments/{experimentId}/geralanding/wireframe/start", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
