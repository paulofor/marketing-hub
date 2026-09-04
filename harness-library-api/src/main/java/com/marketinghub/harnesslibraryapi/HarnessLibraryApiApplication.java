package com.marketinghub.harnesslibraryapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Inicializa a API externa e sem estado da Biblioteca do Harness. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class HarnessLibraryApiApplication {

  /** Inicia o gateway HTTP com sua configuração versionada. */
  public static void main(String[] args) {
    SpringApplication.run(HarnessLibraryApiApplication.class, args);
  }
}
