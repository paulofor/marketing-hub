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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = AdsServiceApplication.class)
class CreativeAngleIntegrationTest {
    @Autowired
    CreativeRepository creativeRepository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    MarketNicheRepository nicheRepository;
    @Autowired
    AngleRepository angleRepository;

    @Test
    void persistRelationships() {
        MarketNiche niche = MarketNiche.builder().name("N").build();
        nicheRepository.save(niche);
        Experiment exp = Experiment.builder().niche(niche).name("E").status(com.marketinghub.experiment.ExperimentStatus.PLANNED).platform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK).build();
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
