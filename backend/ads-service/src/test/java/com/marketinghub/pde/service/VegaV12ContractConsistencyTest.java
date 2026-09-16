package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir divergência entre o contrato v12 empacotado e sua semente SQL. */
class VegaV12ContractConsistencyTest {

  /**
   * Compara a semente histórica e exige que apenas o vínculo incremental de acesso a complemente.
   */
  @Test
  void keepsMigrationSeedEqualToPackagedV12Contract() throws Exception {
    Path repositoryRoot = repositoryRoot();
    Path contract =
        repositoryRoot.resolve(
            "pde-platform/backend/src/main/resources/contracts/musa-v12-product-v1.json");
    Path migration =
        repositoryRoot.resolve(
            "backend/ads-service/src/main/resources/db/changelog/changesets/2026-09-16-vega-v12-commercial-candidate.sql");
    String sql = Files.readString(migration);
    String prefix = "SET @vega_v12_contract = '";
    String suffix = "';\n\nINSERT IGNORE INTO pde_production_slot";
    int start = sql.indexOf(prefix);
    int end = sql.indexOf(suffix, start + prefix.length());

    assertThat(start).isGreaterThanOrEqualTo(0);
    assertThat(end).isGreaterThan(start);
    String seededJson = sql.substring(start + prefix.length(), end).replace("''", "'");

    ObjectMapper mapper = new ObjectMapper();
    var packaged =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.readTree(Files.readString(contract));
    var access = packaged.remove("commercialAccess");

    assertThat(mapper.readTree(seededJson)).isEqualTo(packaged);
    assertThat(access.path("experienceVersion").asText())
        .isEqualTo("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
    assertThat(access.path("accessDays").asInt()).isEqualTo(90);
    assertThat(access.path("renewal").asBoolean(true)).isFalse();
    assertThat(access.path("activationTrigger").asText()).isEqualTo("PAYMENT_APPROVED");
  }

  /** Localiza a raiz independentemente de o Maven ser iniciado nela ou no módulo backend. */
  private Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isRegularFile(
          current.resolve(
              "pde-platform/backend/src/main/resources/contracts/musa-v12-product-v1.json"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Raiz do repositório não encontrada");
  }
}
