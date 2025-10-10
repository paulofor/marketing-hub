package com.marketinghub.experiment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.FixtureUtils;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.ads.InstagramAccountRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
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
    private AngleRepository angleRepository;
    @Autowired
    private HypothesisRepository hypothesisRepository;
    @Autowired
    private com.marketinghub.experiment.repository.MetricPresetRepository metricPresetRepository;
    @Autowired
    private com.marketinghub.creative.repository.CreativeRepository creativeRepo;
    @Autowired
    private FixtureUtils fixtures;
    @Autowired
    private JourneyTemplateRepository journeyTemplateRepository;
    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private InstagramAccountRepository instagramAccountRepository;

    Long nicheId;

    @BeforeEach
    void cleanDb() {
        creativeRepo.deleteAll();
        repository.deleteAll();
        journeyTemplateRepository.deleteAll();
        audienceRepository.deleteAll();
        instagramAccountRepository.deleteAll();
        hypothesisRepository.deleteAll();
        angleRepository.deleteAll();
        nicheRepo.deleteAll();
        MarketNiche niche = fixtures.createAndSaveNiche();
        nicheId = niche.getId();
    }

    @Test
    void createEndpointPersists() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("H")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(BigDecimal.ONE)
                .build());
        metricPresetRepository.save(com.marketinghub.experiment.MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        var instagramAccount = fixtures.createAndSaveInstagramAccount();
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setHypothesisId(hyp.getId());
        req.setHypothesis("H1");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(5));
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(instagramAccount.getId());

        mockMvc.perform(post("/api/niches/" + nicheId + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        var saved = repository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getJourneyTemplate()).isNotNull();
        assertThat(saved.get(0).getJourneyTemplate().getId()).isEqualTo(template.getId());
    }

    @Test
    void createEndpointRejectsMissingJourneyTemplate() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("H")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(BigDecimal.ONE)
                .build());
        metricPresetRepository.save(com.marketinghub.experiment.MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        var instagramAccount = fixtures.createAndSaveInstagramAccount();
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setHypothesisId(hyp.getId());
        req.setHypothesis("H1");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setInstagramAccountId(instagramAccount.getId());

        mockMvc.perform(post("/api/niches/" + nicheId + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEndpointUpdatesFields() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        JourneyTemplate startTemplate = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        JourneyTemplate newTemplate = journeyTemplateRepository.save(JourneyTemplate.builder().name("Retarget").build());
        exp.setJourneyTemplate(startTemplate);
        repository.save(exp);
        UpdateExperimentRequest req = new UpdateExperimentRequest();
        req.setName("Updated");
        req.setHypothesis("Hyp");
        req.setKpiTargetCpl(new BigDecimal("50"));
        req.setMetricPresetId("LEAN_150");
        req.setSampleSize(200);
        req.setMdePercent(new BigDecimal("30"));
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setJourneyTemplateId(newTemplate.getId());

        mockMvc.perform(patch("/api/experiments/" + exp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getSampleSize()).isEqualTo(200);
        assertThat(updated.getMdePercent()).isEqualByComparingTo("30");
        assertThat(updated.getJourneyTemplate()).isNotNull();
        assertThat(updated.getJourneyTemplate().getId()).isEqualTo(newTemplate.getId());
    }

    @Test
    void requestCreativesEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/creatives-to-generate")
                                .param("quantity", "3"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getCreativesToGenerate()).isEqualTo(3);
    }
}
