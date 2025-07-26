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
import com.marketinghub.FixtureUtils;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
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
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=true"
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
    @Autowired
    HypothesisRepository hypothesisRepository;
    @Autowired
    FixtureUtils fixtures;
    @Autowired
    com.marketinghub.creative.label.repository.AngleRepository angleRepository;
    @Autowired
    com.marketinghub.creative.label.repository.VisualProofRepository visualProofRepository;
    @Autowired
    com.marketinghub.creative.label.repository.EmotionalTriggerRepository emotionalTriggerRepository;

    Long expId;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        experimentRepository.deleteAll();
        hypothesisRepository.deleteAll();
        marketNicheRepository.deleteAll();
        MarketNiche niche = fixtures.createAndSaveNiche();
        Experiment exp = fixtures.createAndSaveExperiment(niche);
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

    @Test
    void patchLabelsAssignsSingleLabels() throws Exception {
        // create creative
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("H");
        req.setPrimaryText("P");
        req.setImageUrl("img");
        req.setStatus(CreativeStatus.DRAFT);
        String resp = mockMvc.perform(post("/api/experiments/" + expId + "/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        com.marketinghub.creative.dto.CreativeDto created =
                mapper.readValue(resp, com.marketinghub.creative.dto.CreativeDto.class);

        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var proof = visualProofRepository.save(com.marketinghub.creative.label.VisualProof.builder().name("V").build());
        var trigger = emotionalTriggerRepository.save(com.marketinghub.creative.label.EmotionalTrigger.builder().name("T").build());

        var labels = new com.marketinghub.creative.dto.UpdateCreativeLabelsRequest();
        labels.setAngleId(angle.getId());
        labels.setVisualProofId(proof.getId());
        labels.setEmotionalTriggerId(trigger.getId());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/creatives/" + created.getId() + "/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(labels)))
                .andExpect(status().isOk());

        var found = repository.findById(created.getId()).orElseThrow();
        assertThat(found.getAngles()).hasSize(1);
        assertThat(found.getVisualProofs()).hasSize(1);
        assertThat(found.getEmotionalTriggers()).hasSize(1);
    }
}
