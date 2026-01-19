package com.marketinghub.niche;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.differentiatedtechnology.repository.DifferentiatedTechnologyRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class MarketNicheRepositoryTest {

    @Autowired
    MarketNicheRepository repository;

    @Autowired
    DifferentiatedTechnologyRepository technologyRepository;

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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findAllToGenerateHypothesesFetchesDifferentiatedTechnology() {
        DifferentiatedTechnology technology = technologyRepository.save(DifferentiatedTechnology.builder()
                .name("IA de imagens")
                .description("Processa fotos e entrega versões tratadas")
                .promptText("Base de prompt")
                .build());

        MarketNiche niche = MarketNiche.builder()
                .name("Educação")
                .hypothesesToGenerate(1)
                .differentiatedTechnology(technology)
                .build();
        repository.saveAndFlush(niche);

        var result = repository.findAllToGenerateHypotheses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDifferentiatedTechnology())
                .extracting(DifferentiatedTechnology::getDescription)
                .isEqualTo("Processa fotos e entrega versões tratadas");

        repository.deleteAll();
        technologyRepository.deleteAll();
    }
}
