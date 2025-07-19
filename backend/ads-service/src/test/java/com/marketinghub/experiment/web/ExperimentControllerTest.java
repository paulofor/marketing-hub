package com.marketinghub.experiment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.FixtureUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ExperimentController}.
 */
@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create"
})
class ExperimentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ExperimentRepository repository;
    @Autowired
    private MarketNicheRepository nicheRepo;
    @Autowired
    private com.marketinghub.creative.repository.CreativeRepository creativeRepo;
    @Autowired
    private FixtureUtils fixtures;

    Long nicheId;

    @BeforeEach
    void cleanDb() {
        creativeRepo.deleteAll();
        repository.deleteAll();
        nicheRepo.deleteAll();
        MarketNiche niche = fixtures.createAndSaveNiche();
        nicheId = niche.getId();
    }

    @Test
    void createEndpointPersists() throws Exception {
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setHypothesis("H1");
        req.setKpiTarget(BigDecimal.TEN);
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/niches/" + nicheId + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertThat(repository.count()).isEqualTo(1);
    }
}
