package com.marketinghub.experiment;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.experiment.MetricPreset;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.funnel.SalesFunnel;
import com.marketinghub.funnel.SalesFunnelRepository;
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
    SalesFunnelRepository salesFunnelRepository;
    @Autowired
    AudienceRepository audienceRepository;

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
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        var exp = service.create(req);
        assertThat(exp.getId()).isNotNull();
        assertThat(exp.getPlatform()).isEqualTo(ExperimentPlatform.FACEBOOK);
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
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setStartDate(java.time.LocalDate.of(2024,2,1));
        req.setEndDate(java.time.LocalDate.of(2024,1,1));
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
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
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
        var hyp2 = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H2")
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
        var expApproved = service.create(req1);
        expApproved.setCreativeApproved(true);
        experimentRepository.save(expApproved);

        CreateExperimentRequest req2 = new CreateExperimentRequest();
        req2.setMarketNicheId(niche.getId());
        req2.setHypothesisId(hyp2.getId());
        req2.setName("ExpB");
        req2.setHypothesis("H");
        req2.setKpiTargetCpl(new BigDecimal("45"));
        req2.setMetricPresetId("LEAN_150");
        var expNotApproved = service.create(req2);
        expNotApproved.setCreativeApproved(true);
        experimentRepository.save(expNotApproved);

        audienceRepository.save(Audience.builder()
                .name("Approved audience")
                .niche(niche)
                .hypothesis(hyp)
                .approved(true)
                .build());
        audienceRepository.save(Audience.builder()
                .name("Pending audience")
                .niche(niche)
                .hypothesis(hyp2)
                .approved(false)
                .build());

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
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        var exp = service.create(req);
        exp.setStatus(ExperimentStatus.RUNNING);
        experimentRepository.save(exp);

        var result = service.listByStatusAndPlatform(ExperimentStatus.RUNNING, ExperimentPlatform.FACEBOOK);
        assertThat(result).extracting(Experiment::getId).containsExactly(exp.getId());
    }

    @Test
    void createAssociatesSalesFunnelByName() {
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
        SalesFunnel funnel = salesFunnelRepository.save(SalesFunnel.builder().name("Topo").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setSalesFunnelName("Topo");

        Experiment exp = service.create(req);

        assertThat(exp.getSalesFunnel()).isNotNull();
        assertThat(exp.getSalesFunnel().getId()).isEqualTo(funnel.getId());
    }

    @Test
    void updateChangesSalesFunnelWhenProvided() {
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
        SalesFunnel first = salesFunnelRepository.save(SalesFunnel.builder().name("Topo").build());
        SalesFunnel second = salesFunnelRepository.save(SalesFunnel.builder().name("Meio").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setSalesFunnelName(first.getName());
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setSalesFunnelName(second.getName());

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getSalesFunnel()).isNotNull();
        assertThat(updated.getSalesFunnel().getId()).isEqualTo(second.getId());
    }

    @Test
    void updateClearsSalesFunnelWhenBlank() {
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
        SalesFunnel funnel = salesFunnelRepository.save(SalesFunnel.builder().name("Topo").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setSalesFunnelName(funnel.getName());
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setSalesFunnelName("");

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getSalesFunnel()).isNull();
    }
}
