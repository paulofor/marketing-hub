package com.marketinghub.facebookads.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.facebookads.service.FacebookCampaignStopService;
import com.marketinghub.experiment.Experiment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FacebookCampaignStopController.class)
class FacebookCampaignStopControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    FacebookCampaignStopService stopService;

    @Test
    void listsStopRequests() throws Exception {
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-1");
        campaign.setExternalId("123");
        campaign.setAdAccountId("act_1");
        campaign.setStopReason(FacebookCampaignStopReason.FORM_ZERO_CONVERSION_RULE_OF_THREE);
        campaign.setStopRequestedAt(Instant.parse("2025-01-01T00:00:00Z"));
        Experiment experiment = new Experiment();
        experiment.setId(10L);
        campaign.setExperiment(experiment);
        when(stopService.listPendingStopRequests()).thenReturn(List.of(campaign));

        mockMvc.perform(get("/api/facebook-campaigns/stop-requests"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[{" +
                        "\"id\":\"camp-1\"," +
                        "\"externalId\":\"123\"," +
                        "\"experimentId\":10," +
                        "\"stopReason\":\"FORM_ZERO_CONVERSION_RULE_OF_THREE\"}]", false));
    }

    @Test
    void forwardsStopResult() throws Exception {
        mockMvc.perform(post("/api/facebook-campaigns/camp-1/stop-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(stopService).registerStopResult(eq("camp-1"), successCaptor.capture(), messageCaptor.capture());
        assertThat(successCaptor.getValue()).isTrue();
        assertThat(messageCaptor.getValue()).isNull();
    }
}
