package com.marketinghub.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.MetricPreset;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class ExperimentControllerTest {
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
    @Autowired
    com.marketinghub.experiment.repository.MetricPresetRepository metricPresetRepository;
    @Autowired
    JourneyTemplateRepository journeyTemplateRepository;
    @Autowired
    com.marketinghub.leadportal.repository.LeadPortalFlowRepository leadPortalFlowRepository;

    private Long createLeadPortalFlow(MarketNiche niche) {
        String slug = "flow-" + UUID.randomUUID();
        return leadPortalFlowRepository.save(
                com.marketinghub.leadportal.LeadPortalFlow.builder()
                        .name("Fluxo " + slug)
                        .slug(slug)
                        .marketNiche(niche)
                        .build()).getId();
    }

    @Test
    void postExperiment() throws Exception {
        MarketNiche niche = nicheRepo.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder()
                .name("Lifecycle")
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setHypothesisId(hyp.getId());
        req.setHypothesis("h");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setJourneyTemplateId(template.getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
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
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder()
                .name("Lifecycle")
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setMetricPresetId("LEAN_150");
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setStartDate(java.time.LocalDate.of(2024,2,1));
        req.setEndDate(java.time.LocalDate.of(2024,1,1));
        req.setJourneyTemplateId(template.getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        mockMvc.perform(post("/api/niches/" + niche.getId() + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
