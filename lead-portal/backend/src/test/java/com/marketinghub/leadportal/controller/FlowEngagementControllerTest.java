package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient.TrackingResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlowEngagementController.class)
class FlowEngagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExperimentFunnelTrackingClient trackingClient;

    @Test
    void registerRenderCompleteAcceptsPayload() throws Exception {
        when(trackingClient.registerRenderComplete("flow-slug", "visitor-123", "camp-1"))
                .thenReturn(TrackingResult.FORWARDED);

        mockMvc.perform(post("/api/flows/flow-slug/render-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"visitor-123\",\"campaignCode\":\"camp-1\"}"))
                .andExpect(status().isAccepted());

        verify(trackingClient).registerRenderComplete("flow-slug", "visitor-123", "camp-1");
    }

    @Test
    void registerRenderCompleteIgnoresMalformedPayload() throws Exception {
        when(trackingClient.registerRenderComplete("flow-slug", null, null))
                .thenReturn(TrackingResult.FORWARDED);

        mockMvc.perform(post("/api/flows/flow-slug/render-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"visitor-123\""))
                .andExpect(status().isAccepted());

        verify(trackingClient).registerRenderComplete("flow-slug", null, null);
    }
}
