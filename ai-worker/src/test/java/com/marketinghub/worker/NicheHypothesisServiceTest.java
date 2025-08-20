package com.marketinghub.worker;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.WorkerMarketNicheRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ImportAutoConfiguration
@EntityScan("com.marketinghub")
@ContextConfiguration(classes = AiWorkerApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:testdb}",
        "spring.datasource.driverClassName=${TEST_DB_DRIVER:org.h2.Driver}",
        "spring.datasource.username=${TEST_DB_USERNAME:sa}",
        "spring.datasource.password=${TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class NicheHypothesisServiceTest {

    @Autowired
    WorkerMarketNicheRepository nicheRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

    @Autowired
    AngleRepository angleRepository;

    @Test
    void generatesHypothesesAndResetsCounter() {
        MarketNiche niche = MarketNiche.builder()
                .name("Saúde")
                .hypothesesToGenerate(2)
                .build();
        nicheRepository.save(niche);
        Angle angle = angleRepository.save(Angle.builder().name("General").build());
        HypothesisChatGptClient client = (n, qty) -> {
            List<Hypothesis> list = new ArrayList<>();
            for (int i = 1; i <= qty; i++) {
                list.add(Hypothesis.builder()
                        .title("H" + i)
                        .promise("Promise" + i)
                        .problem("Problem" + i)
                        .persona("Persona" + i)
                        .premiseAngle(angle)
                        .offerType(OfferType.LEAD_MAGNET)
                        .kpiTargetCpl(BigDecimal.ONE)
                        .build());
            }
            return list;
        };
        NicheHypothesisService service =
                new NicheHypothesisService(nicheRepository, hypothesisRepository, client);
        service.generateHypothesesForNiches();
        assertThat(hypothesisRepository.findByMarketNicheId(niche.getId())).hasSize(2);
        assertThat(nicheRepository.findById(niche.getId()).orElseThrow().getHypothesesToGenerate()).isZero();
    }
}

