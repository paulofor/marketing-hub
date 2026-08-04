package com.marketinghub.financialagentworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o executor somente leitura do Agente Financeiro. */
@EnableScheduling
@SpringBootApplication
public class FinancialAgentWorkerApplication {
  /** Inicia o worker financeiro. */
  public static void main(String[] args) {
    SpringApplication.run(FinancialAgentWorkerApplication.class, args);
  }
}
