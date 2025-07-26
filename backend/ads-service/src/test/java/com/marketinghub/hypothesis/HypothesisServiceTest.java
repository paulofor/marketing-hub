package com.marketinghub.hypothesis;

import com.marketinghub.FixtureUtils;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.service.HypothesisService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=true"
})
@org.springframework.transaction.annotation.Transactional
class HypothesisServiceTest {
    @Autowired
    HypothesisService service;
    @Autowired
    MarketNicheRepository nicheRepository;
    @Autowired
    AngleRepository angleRepository;
    @Autowired
    FixtureUtils fixtures;

    @Test
    void createValidHypothesis() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("Teste");
        req.setPremiseAngleId(angle.getId());
        req.setOfferType("LEAD");
        req.setKpiTargetCpl(new BigDecimal("5"));
        Hypothesis h = service.create(req);
        assertThat(h.getId()).isNotNull();
        assertThat(h.getStatus()).isEqualTo(HypothesisStatus.BACKLOG);
    }

    @Test
    void validateTitle() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("   ");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void validateAngleAndKpi() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("T");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void priceRequiredForTripwire() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("T");
        req.setPremiseAngleId(angle.getId());
        req.setOfferType("TRIPWIRE");
        req.setKpiTargetCpl(new BigDecimal("5"));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void marketNicheIdRequired() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        req.setTitle("T");
        req.setPremiseAngleId(angle.getId());
        req.setKpiTargetCpl(new BigDecimal("5"));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
