package com.marketinghub.worker.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Garante que o bootstrap produtivo do AI Worker não declara acesso direto a banco. */
class AiWorkerNoDatabaseBootstrapTest {
    /** Valida que o POM produtivo não reintroduz JPA nem driver MySQL. */
    @Test
    void pomShouldNotDeclareJpaOrMysqlRuntimeDependencies() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom).doesNotContain("spring-boot-starter-data-jpa");
        assertThat(pom).doesNotContain("mysql-connector-j");
    }

    /** Valida que propriedades produtivas não carregam datasource nem Hibernate. */
    @Test
    void applicationPropertiesShouldNotDeclareDatasourceOrJpa() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties).doesNotContain("spring.datasource.");
        assertThat(properties).doesNotContain("spring.jpa.");
        assertThat(properties).doesNotContain("spring.liquibase.");
    }
}
