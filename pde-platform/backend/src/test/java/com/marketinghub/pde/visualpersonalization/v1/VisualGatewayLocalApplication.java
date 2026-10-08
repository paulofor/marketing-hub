package com.marketinghub.pde.visualpersonalization.v1;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** Responsabilidade: homologar o transporte PDE real com origem principal exclusivamente local. */
@TestConfiguration
@EnableAutoConfiguration
@Import({VisualPreparationGatewayController.class, VisualPreparationGatewayService.class, InternalApiAuthorizer.class})
public class VisualGatewayLocalApplication {
  /** Inicia o gateway sem propriedades, bancos, SMTP ou credenciais produtivas. */
  public static void main(String[] args) {
    new SpringApplication(VisualGatewayLocalApplication.class).run(
        "--spring.config.location=optional:classpath:visual-personalization/no-production.properties",
        "--server.address=127.0.0.1", "--server.port=18096",
        "--pde.catalog.marketing-hub-base-url=http://127.0.0.1:18095",
        "--pde.internal-api.token=visual-local-only", "--logging.level.root=WARN");
  }
}
