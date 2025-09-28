package com.marketinghub.facebookads.web;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.funnel.SalesFunnel;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
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
        "spring.datasource.password=",
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
        var niche = MarketNiche.builder()
                .id(10L)
                .name("Test Nicho")
                .build();
        var hypothesis = Hypothesis.builder()
                .id(java.util.UUID.randomUUID())
                .title("Hipótese do Nicho")
                .build();
        var funnel = SalesFunnel.builder()
                .id(java.util.UUID.randomUUID())
                .name("Funil de Conversão")
                .build();
        var exp = Experiment.builder()
                .id(1L)
                .niche(niche)
                .name("Exp")
                .hypothesis("Hipótese")
                .hypothesisRef(hypothesis)
                .kpiTargetCpl(BigDecimal.TEN)
                .stopLossCpl(BigDecimal.valueOf(20))
                .sampleSize(1200)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .creativeApproved(true)
                .salesFunnel(funnel)
                .pageId("84")
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
                .andExpect(jsonPath("$[0].pageId").value("84"))
                .andExpect(jsonPath("$[0].startDate").value("2024-01-01"))
                .andExpect(jsonPath("$[0].endDate").value("2024-01-31"))
                .andExpect(jsonPath("$[0].nicheName").value("Test Nicho"))
                .andExpect(jsonPath("$[0].hypothesisTitle").value("Hipótese do Nicho"))
                .andExpect(jsonPath("$[0].missingConfiguration").isArray())
                .andExpect(jsonPath("$[0].missingConfiguration").isEmpty());
    }
}
