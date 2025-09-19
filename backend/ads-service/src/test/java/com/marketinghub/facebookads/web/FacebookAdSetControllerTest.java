package com.marketinghub.facebookads.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.audience.dto.AudienceDto;
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
        "spring.datasource.url=jdbc:h2:mem:testdb",
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

        AudienceDto audience = AudienceDto.builder()
                .id(1L)
                .name("Audience")
                .description("Desc")
                .marketNicheId(7L)
                .hypothesisId(hypothesisId)
                .approved(true)
                .build();

        ExperimentReadyForAdSetDto dto = new ExperimentReadyForAdSetDto(
                experiment,
                niche,
                hypothesis,
                List.of(audience));

        when(experimentService.listExperimentsReadyForAdSets()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/facebook-adsets/experiments-ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experiment.id").value(42))
                .andExpect(jsonPath("$[0].niche.name").value("Health"))
                .andExpect(jsonPath("$[0].hypothesis.id").value(hypothesisId.toString()))
                .andExpect(jsonPath("$[0].audiences[0].name").value("Audience"));
    }
}

