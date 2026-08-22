package com.marketinghub.businessprocesschain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o schema MySQL 5.7 e a cadeia PDE canônica versionada. */
class BusinessProcessChainChangelogTest {
  private static final String FORMAT_NEUTRAL_CHANGESET =
      "changesets/2026-08-21-pde-format-neutral-chain-v2.yaml";
  private static final String PROOF_DISTRIBUTION_CHANGESET =
      "changesets/2026-08-21-pde-proof-distribution-intent-chain-v3.yaml";
  private static final String AI_DECISION_READINESS_CHANGESET =
      "changesets/2026-08-21-pde-ai-decision-readiness-chain-v4.yaml";
  private static final String HOMOLOGATION_CHANGELOG =
      "src/test/resources/db/changelog/pde-chain-v4-homologation.yaml";

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

  /** Exige uma nova versão auditável que compare formatos e preserve a cadeia anterior. */
  @Test
  void declaresFormatNeutralPdeValueChainVersion() throws Exception {
    String change =
        Files.readString(Path.of("src/main/resources/db/changelog/" + FORMAT_NEUTRAL_CHANGESET));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains("dbms:\n            type: mysql")
        .contains("'pde-value-creation-delivery'")
        .contains("\"label\":\"Comparar formatos digitais\"")
        .contains("\"label\":\"Escolher o melhor formato digital\"")
        .contains("\"label\":\"Materializar o formato aprovado\"")
        .contains("'pde-opportunity-discovery' AND version_number = 2")
        .contains("'pde-commercial-plan-offer' AND version_number = 2")
        .contains("'pde-construction-approval' AND version_number = 2")
        .contains("AND chain_definition.version_number = 2")
        .contains("SET status = 'RETIRED'")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains("file: " + FORMAT_NEUTRAL_CHANGESET + "\n      relativeToChangelogFile: true");
  }

  /** Exige prova verificável, distribuição acumulável e intenção governada nos seis processos. */
  @Test
  void declaresProofDistributionAndIntentPdeValueChainVersion() throws Exception {
    String change =
        Files.readString(
            Path.of("src/main/resources/db/changelog/" + PROOF_DISTRIBUTION_CHANGESET));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains("dbms:\n            type: mysql")
        .contains("\"label\":\"Mapear perguntas e sinais de confiança\"")
        .contains("\"label\":\"Desenhar prova antes da compra\"")
        .contains("\"label\":\"Construir pacote de prova verificável\"")
        .contains("\"label\":\"Preparar canal e ativo próprio\"")
        .contains("\"label\":\"Validar canal, consentimento e atribuição\"")
        .contains("\"label\":\"Coletar prova e dúvidas reais\"")
        .contains("WHERE process_code = 'pde-opportunity-discovery' AND version_number = 3")
        .contains("WHERE process_code = 'pde-commercial-plan-offer' AND version_number = 3")
        .contains("WHERE process_code = 'pde-construction-approval' AND version_number = 3")
        .contains("WHERE process_code = 'pde-communication-sales-journey' AND version_number = 2")
        .contains(
            "WHERE process_code = 'pde-commercial-homologation-activation' AND version_number = 2")
        .contains("WHERE process_code = 'pde-sales-delivery-learning' AND version_number = 2")
        .contains("AND chain_definition.version_number = 3")
        .contains("'pde-value-creation-delivery'")
        .contains("SET status = 'RETIRED'")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains(
            "file: " + PROOF_DISTRIBUTION_CHANGESET + "\n      relativeToChangelogFile: true");
  }

  /** Exige decisão assistida auditável, fricção agregada e controle da personalização. */
  @Test
  void declaresAiDecisionReadinessPdeValueChainVersion() throws Exception {
    String change =
        Files.readString(
            Path.of("src/main/resources/db/changelog/" + AI_DECISION_READINESS_CHANGESET));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains("dbms:\n            type: mysql")
        .contains("\"label\":\"Mapear jornada de decisão assistida\"")
        .contains("\"label\":\"Definir Cartão de Decisão\"")
        .contains("\"label\":\"Materializar Cartão de Decisão\"")
        .contains("\"label\":\"Prontidão para Decisão por IA aprovada?\"")
        .contains("\"label\":\"Explicar recomendação e preservar controle\"")
        .contains("\"label\":\"Extrair e comparar fatos de decisão\"")
        .contains("\"label\":\"Diagnosticar fricção agregada\"")
        .contains("WHERE process_code = 'pde-opportunity-discovery' AND version_number = 4")
        .contains("WHERE process_code = 'pde-commercial-plan-offer' AND version_number = 4")
        .contains("WHERE process_code = 'pde-construction-approval' AND version_number = 4")
        .contains("WHERE process_code = 'pde-communication-sales-journey' AND version_number = 3")
        .contains(
            "WHERE process_code = 'pde-commercial-homologation-activation' AND version_number = 3")
        .contains("WHERE process_code = 'pde-sales-delivery-learning' AND version_number = 3")
        .contains("AND chain_definition.version_number = 4")
        .contains("'pde-value-creation-delivery'")
        .contains("SET status = 'RETIRED'")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains(
            "file: " + AI_DECISION_READINESS_CHANGESET + "\n      relativeToChangelogFile: true");

    var matcher = Pattern.compile("'(\\{\\\"nodes\\\".*?\\})',").matcher(change);
    int diagramCount = 0;
    var objectMapper = new ObjectMapper();
    while (matcher.find()) {
      var diagram = objectMapper.readTree(matcher.group(1));
      assertThat(diagram.path("nodes").isArray()).isTrue();
      assertThat(diagram.path("flows").isArray()).isTrue();
      diagramCount++;
    }
    assertThat(diagramCount).isEqualTo(6);

    try (DirectoryResourceAccessor resourceAccessor =
        new DirectoryResourceAccessor(Path.of(".").toAbsolutePath().normalize())) {
      var database =
          DatabaseFactory.getInstance()
              .openDatabase("offline:mysql", null, null, null, resourceAccessor);
      var liquibase =
          new Liquibase(
              "src/main/resources/db/changelog/" + AI_DECISION_READINESS_CHANGESET,
              resourceAccessor,
              database);

      assertThat(liquibase.getDatabaseChangeLog().getChangeSets()).hasSize(3);

      var homologation = new Liquibase(HOMOLOGATION_CHANGELOG, resourceAccessor, database);
      assertThat(homologation.getDatabaseChangeLog().getChangeSets())
          .extracting("id")
          .contains("2026-08-21-pde-ai-decision-readiness-chain-v4-3");
    }
  }
}
