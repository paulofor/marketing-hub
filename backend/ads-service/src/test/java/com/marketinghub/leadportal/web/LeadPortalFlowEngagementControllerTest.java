package com.marketinghub.leadportal.web;

import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

        verify(experimentFunnelService).registerFormRenderCompleted(eq("flow-slug"), eq("visitor-123"), isNull());
    }

    @Test
    void registerRenderCompleteAcceptsEmptyBody() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/render-complete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerFormRenderCompleted(eq("flow-slug"), isNull(), isNull());
    }

    @Test
    void registerSubmissionForwardsPayload() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionId\":\"abc-123\",\"submittedAt\":\"2024-05-01T12:34:56Z\"}"))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerFormSubmission(eq("flow-slug"), any());
    }

}
