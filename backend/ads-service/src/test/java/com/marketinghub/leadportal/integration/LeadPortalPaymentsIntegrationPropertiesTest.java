package com.marketinghub.leadportal.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Protege os limites seguros da integração oficial com o serviço de pagamentos. */
class LeadPortalPaymentsIntegrationPropertiesTest {

  /** Mantém tempo suficiente para reconciliar uma preferência idempotente criada pelo provedor. */
  @Test
  void usesReadTimeoutCompatibleWithCommercialCheckoutCreation() {
    LeadPortalPaymentsIntegrationProperties properties =
        new LeadPortalPaymentsIntegrationProperties();

    assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(20));
  }

  /** Mantém o mesmo limite na configuração carregada pelo runtime produtivo. */
  @Test
  void declaresReadTimeoutInRuntimeConfiguration() throws Exception {
    String properties = Files.readString(Path.of("src/main/resources/application.properties"));

    assertThat(properties)
        .contains(
            "integrations.lead-portal-payments.read-timeout=${LEAD_PORTAL_PAYMENTS_READ_TIMEOUT:PT20S}");
  }
}
