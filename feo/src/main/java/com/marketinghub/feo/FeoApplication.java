package com.marketinghub.feo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Inicializa a FEO como worker Spring Boot separado para fabricar entregaveis de oferta.
 */
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class FeoApplication {

    /**
     * Inicia a aplicacao do worker FEO.
     */
    public static void main(String[] args) {
        SpringApplication.run(FeoApplication.class, args);
    }
}
