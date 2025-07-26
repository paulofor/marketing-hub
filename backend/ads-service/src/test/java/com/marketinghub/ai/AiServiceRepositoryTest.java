package com.marketinghub.ai;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.ai.repository.AiServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import com.marketinghub.test.MySqlContainerBase;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AdsServiceApplication.class)
class AiServiceRepositoryTest extends MySqlContainerBase {

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
