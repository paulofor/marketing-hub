package com.marketinghub.experiment;

import com.marketinghub.FixtureUtils;
import com.marketinghub.experiment.dto.CreateAdSetRequest;
import com.marketinghub.experiment.service.AdSetService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=true"
})
class AdSetServiceTest {
    @Autowired
    AdSetService service;
    @Autowired
    FixtureUtils fixtures;
    @Autowired
    MarketNicheRepository nicheRepository;
    @Autowired
    ExperimentRepository experimentRepository;

    Long experimentId;
    Long nicheId;

    @BeforeEach
    void setup() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("N").build());
        Experiment exp = fixtures.createAndSaveExperiment(niche);
        experimentId = exp.getId();
        nicheId = niche.getId();
    }

    @Test
    void rejectWhenExperimentFinished() {
        Experiment exp = fixtures.createAndSaveExperiment(nicheRepository.findById(nicheId).orElseThrow());
        exp.setStatus(ExperimentStatus.FINISHED);
        experimentRepository.save(exp);
        CreateAdSetRequest req = new CreateAdSetRequest();
        req.setExperimentId(exp.getId());
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
