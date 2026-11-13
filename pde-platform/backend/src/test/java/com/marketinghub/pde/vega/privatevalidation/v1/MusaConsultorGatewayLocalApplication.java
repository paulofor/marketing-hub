package com.marketinghub.pde.vega.privatevalidation.v1;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.*;

/** Executa somente a fronteira real da candidata com backend local e sem integrações externas. */
@Configuration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@Import({MusaConsultorGatewayController.class, VegaPrivateGatewayService.class})
public class MusaConsultorGatewayLocalApplication {
  /** Inicia a fronteira local isolada sem carregar perfis nem segredos produtivos. */
  public static void main(String[] args) {
    new SpringApplication(MusaConsultorGatewayLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:/nonexistent-local-only.properties",
            "--server.port=18096",
            "--pde.catalog.marketing-hub-base-url=http://127.0.0.1:18086",
            "--spring.mvc.problemdetails.enabled=true",
            "--server.forward-headers-strategy=framework",
            "--logging.level.root=WARN");
  }

  /** Usa exclusivamente uma credencial sintética conhecida pelos testes locais. */
  @Bean
  InternalApiAuthorizer authorizer() {
    return new InternalApiAuthorizer("vega-local-internal-only");
  }
}
