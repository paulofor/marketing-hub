package com.marketinghub.experiment;

import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create"
})
class ExperimentServiceTest {
    @Autowired
    ExperimentService service;
    @Autowired
    MarketNicheRepository nicheRepository;

    @Test
    void createValidExperiment() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTarget(new BigDecimal("10"));
        var exp = service.create(niche.getId(), req);
        assertThat(exp.getId()).isNotNull();
        assertThat(exp.getPlatform()).isEqualTo(ExperimentPlatform.FACEBOOK);
    }

    @Test
    void validateDates() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setName("Exp1");
        req.setStartDate(java.time.LocalDate.of(2024,2,1));
        req.setEndDate(java.time.LocalDate.of(2024,1,1));
        assertThatThrownBy(() -> service.create(niche.getId(), req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
