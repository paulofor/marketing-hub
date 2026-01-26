package com.marketinghub.niche.web;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.FixtureUtils;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class MarketNicheControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    FixtureUtils fixtures;
    @Autowired
    MarketNicheRepository repository;

    @Test
    void requestAudiencesEndpointUpdatesQuantity() throws Exception {
        var niche = fixtures.createAndSaveNiche();
        mockMvc.perform(
                        patch("/api/niches/" + niche.getId() + "/audiences-to-generate")
                                .param("quantity", "3")
                                .param("model", "gpt-4o-mini"))
                .andExpect(status().isOk());
        var updated = repository.findById(niche.getId()).orElseThrow();
        assertThat(updated.getAudiencesToGenerate()).isEqualTo(3);
        assertThat(updated.getAudienceModel()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void requestHypothesesEndpointUpdatesQuantity() throws Exception {
        var niche = fixtures.createAndSaveNiche();
        mockMvc.perform(
                        patch("/api/niches/" + niche.getId() + "/hypotheses-to-generate")
                                .param("quantity", "2")
                                .param("model", "gpt-4o"))
                .andExpect(status().isOk());
        var updated = repository.findById(niche.getId()).orElseThrow();
        assertThat(updated.getHypothesesToGenerate()).isEqualTo(2);
        assertThat(updated.getHypothesisModel()).isEqualTo("gpt-4o");
    }
}
