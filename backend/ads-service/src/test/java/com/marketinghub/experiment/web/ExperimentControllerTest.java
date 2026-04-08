package com.marketinghub.experiment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.targeting.repository.TargetingElementRepository;
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
import com.marketinghub.ads.CampaignRepository;
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
import java.util.UUID;

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
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
    private TargetingElementRepository targetingElementRepository;
    @Autowired
    private InstagramAccountRepository instagramAccountRepository;
    @Autowired
    private com.marketinghub.leadportal.repository.LeadPortalFlowRepository leadPortalFlowRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    Long nicheId;

    private Long createLeadPortalFlow() {
        MarketNiche niche = nicheRepo.findById(nicheId).orElseThrow();
        String slug = "flow-" + UUID.randomUUID();
        return leadPortalFlowRepository.save(
                com.marketinghub.leadportal.LeadPortalFlow.builder()
                        .name("Fluxo " + slug)
                        .slug(slug)
                        .marketNiche(niche)
                        .build()).getId();
    }

    private void applyStageDefaults(CreateExperimentRequest request) {
        request.setStage(com.marketinghub.experiment.ExperimentStage.AD);
        request.setPrimaryVariable("Ângulo de dor");
        request.setPrimaryMetric("CTR de link (%)");
    }

    private void applyStageDefaults(UpdateExperimentRequest request) {
        request.setStage(com.marketinghub.experiment.ExperimentStage.AD);
        request.setPrimaryVariable("Ângulo de dor");
        request.setPrimaryMetric("CTR de link (%)");
    }

    @BeforeEach
    void cleanDb() {
        creativeRepo.deleteAll();
        campaignRepository.deleteAll();

        var experiments = repository.findAll();
        experiments.forEach(experiment -> experiment.setLeadPortalFlow(null));
        repository.saveAll(experiments);

        var leadPortalFlows = leadPortalFlowRepository.findAll();
        leadPortalFlows.forEach(flow -> flow.setExperiment(null));
        leadPortalFlowRepository.saveAll(leadPortalFlows);

        repository.deleteAll();
        leadPortalFlowRepository.deleteAll();
        journeyTemplateRepository.deleteAll();
        targetingElementRepository.deleteAll();
        instagramAccountRepository.deleteAll();
        hypothesisRepository.deleteAll();
        metricPresetRepository.deleteAll();
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
        applyStageDefaults(req);
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
        req.setLeadPortalFlowId(createLeadPortalFlow());

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
        applyStageDefaults(req);
        req.setName("Exp1");
        req.setHypothesisId(hyp.getId());
        req.setHypothesis("H1");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setInstagramAccountId(instagramAccount.getId());
        req.setLeadPortalFlowId(createLeadPortalFlow());

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
        applyStageDefaults(req);
        req.setName("Updated");
        req.setHypothesis("Hyp");
        req.setKpiTargetCpl(new BigDecimal("50"));
        req.setMetricPresetId("LEAN_150");
        req.setSampleSize(200);
        req.setMdePercent(new BigDecimal("30"));
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setJourneyTemplateId(newTemplate.getId());
        req.setLeadPortalFlowId(exp.getLeadPortalFlow().getId());

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

    @Test
    void requestInstantFormsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/instant-forms-to-generate")
                                .param("quantity", "2"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getInstantFormsToGenerate()).isEqualTo(2);
    }

    @Test
    void requestEmailsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/emails-to-generate")
                                .param("quantity", "4"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getEmailsToGenerate()).isEqualTo(4);
    }

    @Test
    void requestSampleEmailsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/sample-emails-to-generate")
                                .param("quantity", "2"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getSampleEmailsToGenerate()).isEqualTo(2);
    }

    @Test
    void requestLeadPortalFlowsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/lead-portal-flows-to-generate")
                                .param("quantity", "2"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getLeadPortalFlowsToGenerate()).isEqualTo(2);
    }
}
