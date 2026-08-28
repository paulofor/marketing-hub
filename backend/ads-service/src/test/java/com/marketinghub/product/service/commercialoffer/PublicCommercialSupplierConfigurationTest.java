package com.marketinghub.product.service.commercialoffer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protege a identidade comercial mínima usada nas superfícies públicas de venda. */
class PublicCommercialSupplierConfigurationTest {
  private static final String DISPLAY_NAME = "Digicom Digital";
  private static final String REGISTRATION_NUMBER = "25.215.414/0001-69";
  private static final String SUPPORT_EMAIL = "contato@digicomdigital.com.br";

  /** Confirma que a configuração pública preserva confiança sem expor nome legal ou endereço. */
  @Test
  void keepsOnlyMinimalCommercialIdentityInPublicConfiguration() throws Exception {
    String properties = Files.readString(Path.of("src/main/resources/application.properties"));
    String compose = Files.readString(Path.of("../../deploy/docker-compose.yml"));

    assertThat(properties)
        .contains(DISPLAY_NAME, REGISTRATION_NUMBER, SUPPORT_EMAIL)
        .doesNotContain("commerce.supplier.legal-name", "commerce.supplier.address");
    assertThat(compose)
        .contains(DISPLAY_NAME, REGISTRATION_NUMBER, SUPPORT_EMAIL)
        .doesNotContain("COMMERCE_SUPPLIER_LEGAL_NAME", "COMMERCE_SUPPLIER_ADDRESS");
  }
}
