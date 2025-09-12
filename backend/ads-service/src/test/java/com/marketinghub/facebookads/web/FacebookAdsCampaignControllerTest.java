package com.marketinghub.facebookads.web;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FacebookAdsCampaignController.class)
class FacebookAdsCampaignControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    ExperimentService experimentService;
    @MockBean
    com.marketinghub.facebookads.FacebookAdsCampaignRepository campaignRepository;

    @Test
    void listExperimentsByStatus() throws Exception {
        var exp = Experiment.builder().id(1L).name("Exp").build();
        when(experimentService.listByStatusAndPlatform(
                com.marketinghub.experiment.ExperimentStatus.PLANNED,
                com.marketinghub.experiment.ExperimentPlatform.FACEBOOK))
                .thenReturn(List.of(exp));
        mockMvc.perform(get("/facebook-campaigns/experiments").param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Exp"));
    }
}
