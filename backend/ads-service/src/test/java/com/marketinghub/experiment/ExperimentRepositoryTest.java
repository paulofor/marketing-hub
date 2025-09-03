package com.marketinghub.experiment;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = AdsServiceApplication.class)
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ExperimentRepositoryTest {

    @Autowired
    ExperimentRepository repository;

    @Autowired
    MarketNicheRepository nicheRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void findAllToGenerateCreativesFetchesHypothesis() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("N1").build());
        Hypothesis hyp = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .build());
        Experiment exp = repository.save(Experiment.builder()
                .niche(niche)
                .hypothesisRef(hyp)
                .name("E1")
                .creativesToGenerate(1)
                .build());

        entityManager.flush();
        entityManager.clear();

        List<Experiment> result = repository.findAllToGenerateCreatives();
        assertThat(result).hasSize(1);
        Experiment fetched = result.get(0);

        entityManager.clear();

        assertThat(fetched.getHypothesisRef().getTitle()).isEqualTo("T");
    }
}

