package com.marketinghub.geralanding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
