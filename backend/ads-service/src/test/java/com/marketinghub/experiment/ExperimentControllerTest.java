package com.marketinghub.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create"
})
class ExperimentControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    MarketNicheRepository nicheRepo;

    @Test
    void postExperiment() throws Exception {
        MarketNiche niche = nicheRepo.save(MarketNiche.builder().name("Teste").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setHypothesis("h");
        req.setKpiTarget(new BigDecimal("11"));
        mockMvc.perform(post("/api/niches/" + niche.getId() + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void validationFail() throws Exception {
        MarketNiche niche = nicheRepo.save(MarketNiche.builder().name("Teste").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setStartDate(java.time.LocalDate.of(2024,2,1));
        req.setEndDate(java.time.LocalDate.of(2024,1,1));
        mockMvc.perform(post("/api/niches/" + niche.getId() + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
