package com.marketinghub.hypothesis.web;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.FixtureUtils;
import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class HypothesisControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    FixtureUtils fixtures;
    @Autowired
    HypothesisRepository repository;

    @Test
    void listByNicheAcceptsAllKeyword() throws Exception {
        var niche = fixtures.createAndSaveNiche();
        fixtures.createAndSaveHypothesis(niche);
        var h2 = fixtures.createAndSaveHypothesis(niche);
        h2.setStatus(HypothesisStatus.TESTING);
        repository.save(h2);

        mockMvc.perform(get("/api/niches/" + niche.getId() + "/hypotheses")
                        .param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }
}
