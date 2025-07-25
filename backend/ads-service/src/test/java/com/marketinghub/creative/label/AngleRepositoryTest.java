package com.marketinghub.creative.label;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.label.repository.AngleRepository;
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
