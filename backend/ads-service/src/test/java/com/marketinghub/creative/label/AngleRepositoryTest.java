package com.marketinghub.creative.label;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.label.repository.AngleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = AdsServiceApplication.class)
class AngleRepositoryTest {

    @Autowired
    AngleRepository repository;

    @Test
    void testSaveAngle() {
        Angle angle = Angle.builder()
                .name("Ganho")
                .description("Descricao")
                .frameType("BENEFIT")
                .build();
        repository.save(angle);
        assertThat(repository.findById(angle.getId())).isPresent();
    }
}
