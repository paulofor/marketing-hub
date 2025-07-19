package com.marketinghub.ads;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@ImportAutoConfiguration
@ContextConfiguration(classes = AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb", 
        "spring.datasource.driverClassName=org.h2.Driver", 
        "spring.jpa.hibernate.ddl-auto=create"
})
class DataSeederIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void allSeedersRunWithoutException() {
        Collection<org.springframework.boot.CommandLineRunner> runners =
                context.getBeansOfType(org.springframework.boot.CommandLineRunner.class).values();
        assertThatCode(() -> {
            for (org.springframework.boot.CommandLineRunner runner : runners) {
                runner.run(new String[]{});
            }
        }).doesNotThrowAnyException();
    }
}
