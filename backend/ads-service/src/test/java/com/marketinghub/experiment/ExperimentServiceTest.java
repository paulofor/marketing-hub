package com.marketinghub.experiment;

import com.marketinghub.targeting.repository.TargetingElementRepository;
import com.marketinghub.experiment.MetricPreset;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.InstagramAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class ExperimentServiceTest {
    @Autowired
    ExperimentService service;
    @Autowired
    MarketNicheRepository nicheRepository;
    @Autowired
    com.marketinghub.hypothesis.repository.HypothesisRepository hypothesisRepository;
    @Autowired
    com.marketinghub.creative.label.repository.AngleRepository angleRepository;
    @Autowired
    com.marketinghub.experiment.repository.MetricPresetRepository metricPresetRepository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    JourneyTemplateRepository journeyTemplateRepository;
    @Autowired
    TargetingElementRepository targetingElementRepository;
    @Autowired
    InstagramAccountRepository instagramAccountRepository;
    @Autowired
    com.marketinghub.leadportal.repository.LeadPortalFlowRepository leadPortalFlowRepository;

    private InstagramAccount createInstagramAccount() {
        return instagramAccountRepository.save(
                InstagramAccount.builder()
                        .name("Conta Teste")
                        .handle("@contateste")
                        .code("IG-1")
                        .build());
    }

    private JourneyTemplate createJourneyTemplate() {
        return journeyTemplateRepository.save(
                JourneyTemplate.builder()
                        .name("Lifecycle")
                        .build());
    }

    private Long createLeadPortalFlow(MarketNiche niche) {
        String slug = "flow-" + java.util.UUID.randomUUID();
        return leadPortalFlowRepository.save(
                com.marketinghub.leadportal.LeadPortalFlow.builder()
                        .name("Fluxo " + slug)
                        .slug(slug)
                        .marketNiche(niche)
                        .build()).getId();
    }

    @Test
    void createNewExperimentWithExistingNiche() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        var exp = service.create(req);
        assertThat(exp.getId()).isNotNull();
        assertThat(exp.getPlatform()).isEqualTo(ExperimentPlatform.FACEBOOK);
    }

    @Test
    void createAllowsSampleSizeBelowOneHundred() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(5);
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        Experiment experiment = service.create(req);

        assertThat(experiment.getSampleSize()).isEqualTo(5);
    }

    @Test
    void createRejectsZeroSampleSize() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(0);
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("sampleSize must be at least 1");
    }

    @Test
    void validateDates() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setStartDate(java.time.LocalDate.of(2024,2,1));
        req.setEndDate(java.time.LocalDate.of(2024,1,1));
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void hypothesisAndNicheMustMatch() {
        MarketNiche niche1 = nicheRepository.save(MarketNiche.builder().name("N1").build());
        MarketNiche niche2 = nicheRepository.save(MarketNiche.builder().name("N2").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche1)
                .title("T")
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
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche2.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche2));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void listReadyForCampaignRequiresApprovals() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H1")
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

        CreateExperimentRequest req1 = new CreateExperimentRequest();
        req1.setMarketNicheId(niche.getId());
        req1.setHypothesisId(hyp.getId());
        req1.setName("ExpA");
        req1.setHypothesis("H");
        req1.setKpiTargetCpl(new BigDecimal("45"));
        req1.setMetricPresetId("LEAN_150");
        req1.setJourneyTemplateId(createJourneyTemplate().getId());
        req1.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req1.setInstagramAccountId(createInstagramAccount().getId());
        var expApproved = service.create(req1);
        expApproved.setCreativeApproved(true);
        experimentRepository.save(expApproved);

        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.INTEREST)
                .status(com.marketinghub.targeting.TargetingElementStatus.APPROVED)
                .term("Interest")
                .build());
        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.JOB_TITLE)
                .status(com.marketinghub.targeting.TargetingElementStatus.APPROVED)
                .term("CMO")
                .build());
        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.BEHAVIOR)
                .status(com.marketinghub.targeting.TargetingElementStatus.APPROVED)
                .term("Engaged")
                .build());

        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.INTEREST)
                .status(com.marketinghub.targeting.TargetingElementStatus.DRAFT)
                .term("Pending")
                .build());

        CreateExperimentRequest req2 = new CreateExperimentRequest();
        req2.setMarketNicheId(niche.getId());
        req2.setHypothesisId(hyp.getId());
        req2.setName("ExpB");
        req2.setHypothesis("H");
        req2.setKpiTargetCpl(new BigDecimal("45"));
        req2.setMetricPresetId("LEAN_150");
        req2.setJourneyTemplateId(createJourneyTemplate().getId());
        req2.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req2.setInstagramAccountId(createInstagramAccount().getId());
        var expNotApproved = service.create(req2);
        experimentRepository.save(expNotApproved);

        var result = service.listReadyForCampaign();
        assertThat(result).extracting(Experiment::getId).containsExactly(expApproved.getId());
    }

    @Test
    void listByStatusAndPlatform() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche2").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A2").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T2")
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
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("ExpRun");
        req.setHypothesis("H");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setInstagramAccountId(createInstagramAccount().getId());
        var exp = service.create(req);
        exp.setStatus(ExperimentStatus.RUNNING);
        experimentRepository.save(exp);

        var result = service.listByStatusAndPlatform(ExperimentStatus.RUNNING, ExperimentPlatform.FACEBOOK);
        assertThat(result).extracting(Experiment::getId).containsExactly(exp.getId());
    }

    @Test
    void createAssociatesJourneyTemplateById() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        Experiment exp = service.create(req);

        assertThat(exp.getJourneyTemplate()).isNotNull();
        assertThat(exp.getJourneyTemplate().getId()).isEqualTo(template.getId());
    }

    @Test
    void createRequiresJourneyTemplateId() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("journeyTemplateId required");
    }

    @Test
    void updateChangesJourneyTemplateWhenProvided() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        JourneyTemplate first = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        JourneyTemplate second = journeyTemplateRepository.save(JourneyTemplate.builder().name("Retarget").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(first.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(second.getId());

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getJourneyTemplate()).isNotNull();
        assertThat(updated.getJourneyTemplate().getId()).isEqualTo(second.getId());
    }

    @Test
    void updateAllowsSampleSizeBelowOneHundred() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(template.getId());
        updateReq.setSampleSize(5);

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getSampleSize()).isEqualTo(5);
    }

    @Test
    void updateRejectsNullJourneyTemplate() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(null);

        assertThatThrownBy(() -> service.update(exp.getId(), updateReq))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("journeyTemplateId required");
    }

    @Test
    void updateChangesLeadPortalFlowWhenProvided() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
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
        Long firstFlow = createLeadPortalFlow(niche);
        Long secondFlow = createLeadPortalFlow(niche);
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(firstFlow);
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(template.getId());
        updateReq.setLeadPortalFlowId(secondFlow);

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getLeadPortalFlow()).isNotNull();
        assertThat(updated.getLeadPortalFlow().getId()).isEqualTo(secondFlow);
    }
}
