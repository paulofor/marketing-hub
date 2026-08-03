package com.marketinghub.growthoperatorworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o executor sandbox do Operador de Crescimento. */
@EnableScheduling
@SpringBootApplication
public class GrowthOperatorWorkerApplication {
  /** Inicia a aplicacao Spring Boot do worker. */
  public static void main(String[] args) {
    SpringApplication.run(GrowthOperatorWorkerApplication.class, args);
  }
}
