package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato de pesquisa governada de preços de Plutus. */
class ProviderPricingResearchContractTest {

  /** Exige fonte oficial, diferenças de plataforma e proibição de gasto automático. */
  @Test
  void shouldPreserveOfficialEvidenceAndFinancialAuthority() throws Exception {
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/financial-agent/v1/provider-pricing.md"));
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/financial-agent/v1/provider-pricing-schema.json"));

    assertThat(prompt)
        .contains(
            "página oficial",
            "plataforma de acesso",
            "fabricante",
            "agregador",
            "conta de créditos",
            "rota",
            "não autoriza compra",
            "{{MODEL}}");
    assertThat(schema).contains("VERIFIED", "INCOMPARABLE", "BLOCKED", "sourceUrl");

    String backendClient =
        Files.readString(
            Path.of(
                "src/main/java/com/marketinghub/financialagentworker/FinancialAgentBackendClient.java"));
    assertThat(backendClient)
        .contains("/api/internal/sales-videos/provider-models/pricing/pending")
        .doesNotContain("/api/sales-videos/provider-models\"");
  }
}
