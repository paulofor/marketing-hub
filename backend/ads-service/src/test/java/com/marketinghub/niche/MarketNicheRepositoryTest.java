package com.marketinghub.niche;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.repository.jpa.differentiatedtechnology.DifferentiatedTechnologyRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Responsabilidade: validar consultas JPA de nichos de mercado. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class MarketNicheRepositoryTest {

    @Autowired
    MarketNicheRepository repository;

    @Autowired
    DifferentiatedTechnologyRepository technologyRepository;

    /** Deve persistir um nicho de mercado básico. */
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


    /** Deve listar nichos recentes primeiro com custo e agregados zerados quando não há filhos. */
    @Test
    void findListItemsOrdersByCreationAndReturnsCost() throws Exception {
        MarketNiche older = MarketNiche.builder()
                .name("Nicho antigo")
                .totalCost(new BigDecimal("10.00"))
                .build();
        repository.saveAndFlush(older);
        Thread.sleep(5);
        MarketNiche newer = MarketNiche.builder()
                .name("Nicho recente")
                .totalCost(new BigDecimal("20.00"))
                .build();
        repository.saveAndFlush(newer);

        var result = repository.findListItems(PageRequest.of(0, 30));

        assertThat(result.getContent())
                .extracting(item -> item.getName())
                .containsExactly("Nicho recente", "Nicho antigo");
        assertThat(result.getContent().get(0).getTotalCost()).isEqualByComparingTo("20.00");
        assertThat(result.getContent().get(0).getPipelineHypothesesCount()).isZero();
        assertThat(result.getContent().get(0).getExperimentsCount()).isZero();
    }

    /** Deve retornar apenas nichos configurados para geração de hipóteses. */
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

    /** Deve carregar a tecnologia diferenciada junto dos nichos com hipóteses pendentes. */
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
