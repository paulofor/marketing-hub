package com.marketinghub.niche;

import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class MarketNicheRepositoryTest {

    @Autowired
    MarketNicheRepository repository;

    @Test
    void testSaveMarketNiche() {
        MarketNiche niche = MarketNiche.builder()
                .name("Fitness")
                .demandVolume("High")
                .promises("Lose weight")
                .offers("E-book")
                .build();
        repository.save(niche);
        assertThat(repository.findById(niche.getId())).isPresent();
    }

    @Test
    void findAllToGenerateHypothesesReturnsOnlyConfiguredNiches() {
        MarketNiche withHyps = MarketNiche.builder()
                .name("Saúde")
                .hypothesesToGenerate(2)
                .build();
        repository.save(withHyps);

        MarketNiche withoutHyps = MarketNiche.builder()
                .name("Ignored")
                .hypothesesToGenerate(0)
                .build();
        repository.save(withoutHyps);

        var result = repository.findAllToGenerateHypotheses();
        assertThat(result)
                .extracting(MarketNiche::getName)
                .containsExactly("Saúde");
    }
}
