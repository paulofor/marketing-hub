package com.marketinghub.customeragentworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o executor somente leitura do Agente Cliente. */
@EnableScheduling
@SpringBootApplication
public class CustomerAgentWorkerApplication {
  /** Inicia o processo do worker. */
  public static void main(String[] args) {
    SpringApplication.run(CustomerAgentWorkerApplication.class, args);
  }
}
