package com.marketinghub.metaadapproverworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Responsabilidade: iniciar o executor independente do Aprovador de Anúncios Meta. */
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(MetaAdApproverProperties.class)
public class MetaAdApproverWorkerApplication {
  /** Inicia o módulo e sua fila operacional própria. */
  public static void main(String[] args) {
    SpringApplication.run(MetaAdApproverWorkerApplication.class, args);
  }
}
