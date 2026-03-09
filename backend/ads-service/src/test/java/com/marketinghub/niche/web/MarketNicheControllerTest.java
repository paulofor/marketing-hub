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
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
    void requestInterestsEndpointUpdatesQuantity() throws Exception {
        var niche = fixtures.createAndSaveNiche();
        mockMvc.perform(
                        patch("/api/niches/" + niche.getId() + "/interests-to-generate")
                                .param("quantity", "3")
                                .param("model", "gpt-4o-mini"))
                .andExpect(status().isOk());
        var updated = repository.findById(niche.getId()).orElseThrow();
        assertThat(updated.getInterestsToGenerate()).isEqualTo(3);
        assertThat(updated.getInterestModel()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void requestJobTitlesEndpointUpdatesQuantity() throws Exception {
        var niche = fixtures.createAndSaveNiche();
        mockMvc.perform(
                        patch("/api/niches/" + niche.getId() + "/job-titles-to-generate")
                                .param("quantity", "4")
                                .param("model", "gpt-4o"))
                .andExpect(status().isOk());
        var updated = repository.findById(niche.getId()).orElseThrow();
        assertThat(updated.getJobTitlesToGenerate()).isEqualTo(4);
        assertThat(updated.getJobTitleModel()).isEqualTo("gpt-4o");
    }

    @Test
    void requestBehaviorsEndpointUpdatesQuantity() throws Exception {
        var niche = fixtures.createAndSaveNiche();
        mockMvc.perform(
                        patch("/api/niches/" + niche.getId() + "/behaviors-to-generate")
                                .param("quantity", "5")
                                .param("model", "gpt-4o"))
                .andExpect(status().isOk());
        var updated = repository.findById(niche.getId()).orElseThrow();
        assertThat(updated.getBehaviorsToGenerate()).isEqualTo(5);
        assertThat(updated.getBehaviorModel()).isEqualTo("gpt-4o");
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
