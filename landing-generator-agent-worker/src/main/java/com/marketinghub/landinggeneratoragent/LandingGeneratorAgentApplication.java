package com.marketinghub.landinggeneratoragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o executor independente do Agente Gerador de Landing. */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LandingGeneratorAgentProperties.class)
public class LandingGeneratorAgentApplication {
  /** Inicia o worker sem assumir orquestração do backend. */
  public static void main(String[] args) {
    SpringApplication.run(LandingGeneratorAgentApplication.class, args);
  }
}
