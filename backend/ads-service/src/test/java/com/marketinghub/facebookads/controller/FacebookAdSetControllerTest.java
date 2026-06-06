package com.marketinghub.facebookads.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.facebookads.dto.TargetingPackageDto;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.TargetingElementDto;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.facebookads.service.FacebookAdSetExperimentService;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.niche.dto.MarketNicheDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class FacebookAdSetControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    FacebookAdSetExperimentService experimentService;

    @Test
    void experimentsReadyReturnsAggregatedPayload() throws Exception {
        ExperimentDto experiment = new ExperimentDto();
        experiment.setId(42L);
        experiment.setName("Experiment");

        MarketNicheDto niche = new MarketNicheDto();
        niche.setId(7L);
        niche.setName("Health");

        HypothesisDto hypothesis = new HypothesisDto();
        UUID hypothesisId = UUID.randomUUID();
        hypothesis.setId(hypothesisId);
        hypothesis.setTitle("Title");

        TargetingElementDto interest = TargetingElementDto.builder()
                .id(1L)
                .type(TargetingElementType.INTEREST)
                .term("Remarketing")
                .marketNicheId(7L)
                .hypothesisId(hypothesisId)
                .build();
        TargetingElementDto jobTitle = TargetingElementDto.builder()
                .id(2L)
                .type(TargetingElementType.JOB_TITLE)
                .term("CMO")
                .marketNicheId(7L)
                .hypothesisId(hypothesisId)
                .build();
        TargetingElementDto behavior = TargetingElementDto.builder()
                .id(3L)
                .type(TargetingElementType.BEHAVIOR)
                .term("Engaged Shoppers")
                .marketNicheId(7L)
                .hypothesisId(hypothesisId)
                .build();

        ExperimentReadyForAdSetDto dto = new ExperimentReadyForAdSetDto(
                experiment,
                niche,
                hypothesis,
                new TargetingPackageDto(List.of(interest), List.of(jobTitle), List.of(behavior)));

        when(experimentService.listExperimentsReadyForAdSets()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/facebook-adsets/experiments-ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experiment.id").value(42))
                .andExpect(jsonPath("$[0].niche.name").value("Health"))
                .andExpect(jsonPath("$[0].hypothesis.id").value(hypothesisId.toString()))
                .andExpect(jsonPath("$[0].targeting.interests[0].term").value("Remarketing"));
    }

    @Test
    void experimentsReadySupportsPayloadWithOnlyJobTitleTargeting() throws Exception {
        ExperimentDto experiment = new ExperimentDto();
        experiment.setId(11L);
        experiment.setName("Job title only");

        MarketNicheDto niche = new MarketNicheDto();
        niche.setId(7L);
        niche.setName("Health");

        HypothesisDto hypothesis = new HypothesisDto();
        UUID hypothesisId = UUID.randomUUID();
        hypothesis.setId(hypothesisId);
        hypothesis.setTitle("Title");

        TargetingElementDto jobTitle = TargetingElementDto.builder()
                .id(2L)
                .type(TargetingElementType.JOB_TITLE)
                .term("CMO")
                .marketNicheId(7L)
                .hypothesisId(hypothesisId)
                .build();

        ExperimentReadyForAdSetDto dto = new ExperimentReadyForAdSetDto(
                experiment,
                niche,
                hypothesis,
                new TargetingPackageDto(List.of(), List.of(jobTitle), List.of()));

        when(experimentService.listExperimentsReadyForAdSets()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/facebook-adsets/experiments-ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experiment.id").value(11))
                .andExpect(jsonPath("$[0].targeting.interests").isEmpty())
                .andExpect(jsonPath("$[0].targeting.jobTitles[0].term").value("CMO"))
                .andExpect(jsonPath("$[0].targeting.behaviors").isEmpty());
    }
}
