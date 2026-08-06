package com.marketinghub.experimentstrategistworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o executor isolado do Agente Estrategista. */
@EnableScheduling
@SpringBootApplication
public class ExperimentStrategistWorkerApplication {
  /** Inicia o worker Spring Boot. */
  public static void main(String[] args) {
    SpringApplication.run(ExperimentStrategistWorkerApplication.class, args);
  }
}
