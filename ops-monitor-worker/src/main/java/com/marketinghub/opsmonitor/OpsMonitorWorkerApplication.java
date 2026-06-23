package com.marketinghub.opsmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Inicializa o worker responsável pelo monitoramento operacional dos módulos. */
@SpringBootApplication
@EnableScheduling
public class OpsMonitorWorkerApplication {

    /** Executa a aplicação Spring Boot do monitor operacional. */
    public static void main(String[] args) {
        SpringApplication.run(OpsMonitorWorkerApplication.class, args);
    }
}
