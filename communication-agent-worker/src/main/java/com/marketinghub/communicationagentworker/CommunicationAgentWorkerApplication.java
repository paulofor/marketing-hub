package com.marketinghub.communicationagentworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o executor independente da agente de comunicação Íris. */
@EnableScheduling
@SpringBootApplication
public class CommunicationAgentWorkerApplication {
  /** Inicia a aplicação Spring Boot do worker. */
  public static void main(String[] args) {
    SpringApplication.run(CommunicationAgentWorkerApplication.class, args);
  }
}
