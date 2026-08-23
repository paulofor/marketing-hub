package com.marketinghub.product.service.commercialoffer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protege a coerência entre a identidade institucional e a configuração pública de venda. */
class PublicCommercialSupplierConfigurationTest {
  private static final String LEGAL_NAME = "PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA";
  private static final String REGISTRATION_NUMBER = "25.215.414/0001-69";
  private static final String SUPPORT_EMAIL = "contato@digicomdigital.com.br";
  private static final String ADDRESS =
      "Rua Antonio Basilio, 204, apto 805 - Tijuca - Rio de Janeiro/RJ - CEP 20511-190";

  /** Confirma que aplicação, Compose e presença institucional usam o mesmo fornecedor. */
  @Test
  void keepsCommercialSupplierConsistentAcrossVersionedSources() throws Exception {
    String properties = Files.readString(Path.of("src/main/resources/application.properties"));
    String compose = Files.readString(Path.of("../../deploy/docker-compose.yml"));
    String institutionalSite =
        Files.readString(Path.of("../../institutional-site/public/index.html"));

    assertThat(properties).contains(LEGAL_NAME, REGISTRATION_NUMBER, ADDRESS, SUPPORT_EMAIL);
    assertThat(compose).contains(LEGAL_NAME, REGISTRATION_NUMBER, ADDRESS, SUPPORT_EMAIL);
    assertThat(institutionalSite).contains(LEGAL_NAME, REGISTRATION_NUMBER, SUPPORT_EMAIL);
  }
}
