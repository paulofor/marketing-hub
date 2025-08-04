package com.marketinghub.experiment;

import com.marketinghub.experiment.MetricPreset;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
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
}
