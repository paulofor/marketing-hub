package com.marketinghub.niche;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=true"
})
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
}
