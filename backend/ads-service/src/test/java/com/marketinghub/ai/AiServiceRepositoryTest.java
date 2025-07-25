package com.marketinghub.ai;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.ai.repository.AiServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

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
class AiServiceRepositoryTest {

    @Autowired
    AiServiceRepository repository;

    @Test
    void testSaveAiService() {
        AiService service = AiService.builder()
                .name("OpenAI GPT-4")
                .objective("Geração de texto")
                .url("https://example.com")
                .phase("Planejamento")
                .price(new BigDecimal("20.0"))
                .cost(new BigDecimal("20.0"))
                .build();
        repository.save(service);
        assertThat(repository.findById(service.getId())).isPresent();
    }
}
