package com.marketinghub.productdiscovery.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: manter documentados os contratos HTTP da descoberta ampla de Argos. */
class ArgosMarketDiscoverySwaggerTest {
  private static final Path SWAGGER_ROOT = Path.of("../../docs/swagger");

  /** Exige modo, comprador, fontes e auditoria no contrato do ciclo e do worker. */
  @Test
  void documentsProductDiscoveryV11() throws Exception {
    String swagger = Files.readString(SWAGGER_ROOT.resolve("product-discovery-swagger.yaml"));

    assertThat(swagger)
        .contains(
            "version: 1.1.0",
            "DISCOVER_MARKETS",
            "VALIDATE_MARKET",
            "marketType",
            "referenceSources",
            "ProductDiscoveryAnalysisAudit",
            "evidenceReport",
            "accessedUrls");
  }

  /** Exige que o catálogo genérico exponha e documente opções seletivas do backend. */
  @Test
  void documentsIndependentSelectFields() throws Exception {
    String swagger = Files.readString(SWAGGER_ROOT.resolve("business-processes-swagger.yaml"));

    assertThat(swagger)
        .contains(
            "version: 1.10.0",
            "enum: [TEXT, TEXTAREA, SELECT]",
            "IndependentBusinessProcessInputOption",
            "Opções canônicas exigidas quando controlType for SELECT");
  }
}
