package com.marketinghub.pde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Inicializa o backend da plataforma PDE multi-produto. */
@SpringBootApplication
public class PdePlatformApplication {

    /** Sobe a aplicação Spring Boot da plataforma PDE. */
    public static void main(String[] args) {
        SpringApplication.run(PdePlatformApplication.class, args);
    }
}
