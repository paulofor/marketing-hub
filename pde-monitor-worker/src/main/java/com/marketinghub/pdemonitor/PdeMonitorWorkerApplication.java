package com.marketinghub.pdemonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Inicializa o worker dedicado ao monitoramento 24/7 dos PDEs críticos. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class PdeMonitorWorkerApplication {

    /** Executa a aplicação Spring Boot do monitor dedicado de PDEs. */
    public static void main(String[] args) {
        SpringApplication.run(PdeMonitorWorkerApplication.class, args);
    }
}
