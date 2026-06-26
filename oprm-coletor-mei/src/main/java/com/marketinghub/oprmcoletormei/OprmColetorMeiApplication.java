package com.marketinghub.oprmcoletormei;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Inicializa o coletor OPRM MEI com o pipeline NichoCNAE v3 no contexto Spring.
 */
@SpringBootApplication(scanBasePackages = {"com.marketinghub.oprmcoletormei", "com.marketinghub.pipelines.nichocnae.v3"})
@EnableScheduling
@ConfigurationPropertiesScan
public class OprmColetorMeiApplication {

    /** Executa a aplicação Spring Boot do coletor OPRM MEI. */
    public static void main(String[] args) {
        SpringApplication.run(OprmColetorMeiApplication.class, args);
    }
}
