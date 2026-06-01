package com.marketinghub.ai;

import com.marketinghub.repository.jpa.ai.AiServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
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
                .observation("Teste de observação")
                .build();
        repository.save(service);
        assertThat(repository.findById(service.getId())).isPresent();
    }
}
