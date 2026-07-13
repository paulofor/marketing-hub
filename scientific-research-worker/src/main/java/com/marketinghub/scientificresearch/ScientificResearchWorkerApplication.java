package com.marketinghub.scientificresearch;

import com.marketinghub.scientificresearch.config.ScientificResearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Inicializa o worker de pesquisa científica do Marketing Hub.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ScientificResearchProperties.class)
public class ScientificResearchWorkerApplication {

    /**
     * Sobe a aplicação Spring Boot do worker.
     */
    public static void main(String[] args) {
        SpringApplication.run(ScientificResearchWorkerApplication.class, args);
    }
}
