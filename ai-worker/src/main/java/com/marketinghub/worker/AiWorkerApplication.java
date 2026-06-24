package com.marketinghub.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Responsabilidade: iniciar o AI Worker sem acesso direto a banco, usando o backend como camada de persistência.
 */
@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                LiquibaseAutoConfiguration.class
        }
)
public class AiWorkerApplication {
    /** Inicializa o processo Spring Boot do AI Worker. */
    public static void main(String[] args) {
        SpringApplication.run(AiWorkerApplication.class, args);
    }
}
