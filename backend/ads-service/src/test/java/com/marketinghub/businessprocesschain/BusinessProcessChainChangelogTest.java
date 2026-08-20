package com.marketinghub.businessprocesschain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o schema MySQL 5.7 e a cadeia PDE canônica versionada. */
class BusinessProcessChainChangelogTest {
  /** Exige tabelas, seis processos, métrica comercial e include relativo no master. */
  @Test
  void declaresCompatiblePdeValueChain() throws Exception {
    String change =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-20-business-process-value-chains.yaml"));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains("dbms:\n            type: mysql")
        .contains("CREATE TABLE business_process_chain_definition")
        .contains("CREATE TABLE business_process_chain_item")
        .contains("created_at DATETIME NOT NULL")
        .contains("'pde-opportunity-discovery'")
        .contains("'pde-commercial-plan-offer'")
        .contains("'pde-construction-approval'")
        .contains("'pde-communication-sales-journey'")
        .contains("'pde-commercial-homologation-activation'")
        .contains("'pde-sales-delivery-learning'")
        .contains("'Tempo até venda entregue com satisfação'")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-20-business-process-value-chains.yaml\n      relativeToChangelogFile: true");
  }
}
