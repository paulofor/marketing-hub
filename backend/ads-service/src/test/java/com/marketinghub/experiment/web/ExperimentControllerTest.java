package com.marketinghub.experiment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link ExperimentController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExperimentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ExperimentRepository repository;

    @Test
    void createEndpointPersists() throws Exception {
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setHypothesis("H1");
        req.setKpiGoal(BigDecimal.TEN);
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assert(repository.count() == 1);
    }
}
