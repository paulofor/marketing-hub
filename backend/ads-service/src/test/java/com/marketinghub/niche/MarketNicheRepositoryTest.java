package com.marketinghub.niche;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import com.marketinghub.test.MySqlContainerBase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AdsServiceApplication.class)
class MarketNicheRepositoryTest extends MySqlContainerBase {

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
}
