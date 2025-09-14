package com.marketinghub.facebookads.web;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class FacebookAdsCampaignControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    ExperimentService experimentService;
    @MockBean
    com.marketinghub.facebookads.FacebookAdsCampaignRepository campaignRepository;

    @Test
    void listExperimentsByStatus() throws Exception {
        var exp = Experiment.builder()
                .id(1L)
                .name("Exp")
                .hypothesis("Hipótese")
                .kpiTargetCpl(BigDecimal.TEN)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();
        when(experimentService.listByStatusAndPlatform(
                com.marketinghub.experiment.ExperimentStatus.PLANNED,
                com.marketinghub.experiment.ExperimentPlatform.FACEBOOK))
                .thenReturn(List.of(exp));
        mockMvc.perform(get("/api/facebook-campaigns/experiments").param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Exp"))
                .andExpect(jsonPath("$[0].hypothesis").value("Hipótese"))
                .andExpect(jsonPath("$[0].kpiTargetCpl").value(10))
                .andExpect(jsonPath("$[0].startDate").value("2024-01-01"))
                .andExpect(jsonPath("$[0].endDate").value("2024-01-31"));
    }
}
