package com.marketinghub.productaiworker;

import com.marketinghub.productaiworker.config.ProductAiWorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o módulo executor de Produtos IA. */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ProductAiWorkerProperties.class)
public class ProductAiWorkerApplication {
    /** Inicia a aplicação Spring Boot do worker. */
    public static void main(String[] args) {
        SpringApplication.run(ProductAiWorkerApplication.class, args);
    }
}
