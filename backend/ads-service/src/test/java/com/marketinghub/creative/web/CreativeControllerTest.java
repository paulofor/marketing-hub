package com.marketinghub.creative.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CreativeController.
 */
@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create"
})
class CreativeControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    CreativeRepository repository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    MarketNicheRepository marketNicheRepository;

    Long expId;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        experimentRepository.deleteAll();
        marketNicheRepository.deleteAll();
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder()
                .name("Digital Marketing").build());
        Experiment exp = experimentRepository.save(Experiment.builder()
                .name("E").niche(niche).build());
        expId = exp.getId();
    }

    @Test
    void createEndpointPersists() throws Exception {
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("H");
        req.setPrimaryText("P");
        req.setImageUrl("img");
        req.setStatus(CreativeStatus.DRAFT);
        mockMvc.perform(post("/api/experiments/" + expId + "/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        assertThat(repository.count()).isEqualTo(1);
    }
}
