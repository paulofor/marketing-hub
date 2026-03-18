package com.marketinghub.leadportal.web;

import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeadPortalFlowEngagementController.class)
class LeadPortalFlowEngagementControllerTest {

    @SpringBootApplication
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExperimentFunnelService experimentFunnelService;

    @Test
    void registerRenderCompleteAcceptsPayload() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/render-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"visitor-123\"}"))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerFormRenderCompleted("flow-slug", "visitor-123");
    }

    @Test
    void registerRenderCompleteAcceptsEmptyBody() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/render-complete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerFormRenderCompleted("flow-slug", null);
    }
}
