package com.marketinghub.hypothesis;

import com.marketinghub.FixtureUtils;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.service.HypothesisService;
import com.marketinghub.niche.MarketNiche;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

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
@org.springframework.transaction.annotation.Transactional
class HypothesisServiceTest {
    @Autowired
    HypothesisService service;
    @Autowired
    FixtureUtils fixtures;

    @Test
    void createValidHypothesis() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("Teste");
        Hypothesis h = service.create(req);
        assertThat(h.getId()).isNotNull();
        assertThat(h.getStatus()).isEqualTo(HypothesisStatus.BACKLOG);
        assertThat(h.getGeneratedAt()).isNotNull();
    }

    @Test
    void createHypothesisWithoutAngle() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("Sem ângulo");
        Hypothesis h = service.create(req);
        assertThat(h.getPremiseAngle()).isNull();
    }

    @Test
    void validateTitle() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("   ");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void listByMarketNicheWithNullStatusReturnsAll() {
        MarketNiche niche = fixtures.createAndSaveNiche();

        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("H1");

        Hypothesis h1 = service.create(req);
        Hypothesis h2 = service.create(req);
        service.updateStatus(h2.getId(), HypothesisStatus.TESTING);

        Iterable<Hypothesis> all = service.listByMarketNiche(niche.getId(), null);
        java.util.List<Hypothesis> list = java.util.stream.StreamSupport
                .stream(all.spliterator(), false)
                .toList();

        assertThat(list).hasSize(2);
    }

    @Test
    void updateHypothesisOnlyWhenBacklog() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("H1");

        Hypothesis h = service.create(req);

        com.marketinghub.hypothesis.dto.UpdateHypothesisRequest u = new com.marketinghub.hypothesis.dto.UpdateHypothesisRequest();
        u.setTitle("H2");

        Hypothesis updated = service.update(h.getId(), u);
        assertThat(updated.getTitle()).isEqualTo("H2");

        service.updateStatus(h.getId(), HypothesisStatus.TESTING);
        assertThatThrownBy(() -> service.update(h.getId(), u))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
