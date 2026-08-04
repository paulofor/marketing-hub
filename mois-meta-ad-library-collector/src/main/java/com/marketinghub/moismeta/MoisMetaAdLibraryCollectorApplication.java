package com.marketinghub.moismeta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Inicializa o coletor autorizado da Biblioteca de Anúncios da Meta. */
@SpringBootApplication
@EnableScheduling
public class MoisMetaAdLibraryCollectorApplication {

  /** Inicia o processo Spring Boot do coletor. */
  public static void main(String[] args) {
    SpringApplication.run(MoisMetaAdLibraryCollectorApplication.class, args);
  }
}
