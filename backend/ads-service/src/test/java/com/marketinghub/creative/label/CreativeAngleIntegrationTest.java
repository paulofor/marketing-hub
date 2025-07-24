package com.marketinghub.creative.label;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = AdsServiceApplication.class)
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class CreativeAngleIntegrationTest {
    @Autowired
    CreativeRepository creativeRepository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    MarketNicheRepository nicheRepository;
    @Autowired
    AngleRepository angleRepository;
    @Autowired
    HypothesisRepository hypothesisRepository;

    @Test
    void persistRelationships() {
        MarketNiche niche = MarketNiche.builder().name("N").build();
        nicheRepository.save(niche);
        Angle base = angleRepository.save(Angle.builder().name("Base").build());
        var hyp = com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H")
                .premiseAngle(base)
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(java.math.BigDecimal.ONE)
                .build();
        hyp = hypothesisRepository.save(hyp);
        Experiment exp = Experiment.builder()
                .niche(niche)
                .name("E")
                .hypothesis("H")
                .hypothesisRef(hyp)
                .status(com.marketinghub.experiment.ExperimentStatus.PLANNED)
                .platform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK)
                .build();
        experimentRepository.save(exp);
        Angle angle = Angle.builder().name("Ganho").build();
        angleRepository.save(angle);
        Creative c = Creative.builder()
                .experiment(exp)
                .headline("h")
                .primaryText("p")
                .imageUrl("i")
                .status(CreativeStatus.DRAFT)
                .angles(Set.of(angle))
                .build();
        creativeRepository.save(c);
        Creative found = creativeRepository.findById(c.getId()).orElseThrow();
        assertThat(found.getAngles()).hasSize(1);
    }
}
