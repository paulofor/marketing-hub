package com.marketinghub.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import com.marketinghub.test.MySqlContainerBase;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@AutoConfigureMockMvc
class ExperimentControllerTest extends MySqlContainerBase {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    MarketNicheRepository nicheRepo;
    @Autowired
    AngleRepository angleRepository;
    @Autowired
    HypothesisRepository hypothesisRepository;

    @Test
    void postExperiment() throws Exception {
        MarketNiche niche = nicheRepo.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H")
                .premiseAngle(angle)
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setHypothesisId(hyp.getId());
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
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H")
                .premiseAngle(angle)
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setStartDate(java.time.LocalDate.of(2024,2,1));
        req.setEndDate(java.time.LocalDate.of(2024,1,1));
        mockMvc.perform(post("/api/niches/" + niche.getId() + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
