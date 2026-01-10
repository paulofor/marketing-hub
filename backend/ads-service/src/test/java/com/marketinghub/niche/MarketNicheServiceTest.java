package com.marketinghub.niche;

import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.niche.service.MarketNicheService;
import com.marketinghub.chat.repository.ChatDialogRepository;
import com.marketinghub.differentiatedtechnology.repository.DifferentiatedTechnologyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class MarketNicheServiceTest {

    @Autowired
    MarketNicheRepository repository;

    MarketNicheService service;

    @BeforeEach
    void setup() {
        ChatDialogRepository chatRepo = mock(ChatDialogRepository.class);
        DifferentiatedTechnologyRepository differentiatedTechnologyRepository =
                mock(DifferentiatedTechnologyRepository.class);
        service = new MarketNicheService(repository, chatRepo, differentiatedTechnologyRepository);
    }

    @Test
    void updatePersistsHypothesesToGenerate() {
        MarketNiche niche = MarketNiche.builder()
                .name("Fitness")
                .hypothesesToGenerate(1)
                .build();
        repository.save(niche);

        CreateMarketNicheRequest req = new CreateMarketNicheRequest();
        req.setName("Fitness");
        req.setHypothesesToGenerate(5);

        service.update(niche.getId(), req);

        MarketNiche updated = repository.findById(niche.getId()).orElseThrow();
        assertThat(updated.getHypothesesToGenerate()).isEqualTo(5);
    }

    @Test
    void requestHypothesesUpdatesQuantity() {
        MarketNiche niche = MarketNiche.builder()
                .name("Fitness")
                .hypothesesToGenerate(1)
                .build();
        repository.save(niche);

        service.requestHypotheses(niche.getId(), 4, "gpt-4o", null);

        MarketNiche updated = repository.findById(niche.getId()).orElseThrow();
        assertThat(updated.getHypothesesToGenerate()).isEqualTo(4);
        assertThat(updated.getHypothesisModel()).isEqualTo("gpt-4o");
    }
}
