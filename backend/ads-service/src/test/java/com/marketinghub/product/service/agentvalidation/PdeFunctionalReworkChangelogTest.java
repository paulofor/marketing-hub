package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o processo v8 de correção funcional e revalidação integral do PDE. */
class PdeFunctionalReworkChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String CHANGELOG = "changesets/2026-09-07-pde-functional-rework-v1.yaml";

  /** Exige inclusão relativa, contrato MySQL 5.7 e três mudanças reversíveis. */
  @Test
  void declaresCompatibleAndRelativelyIncludedChangelog() throws Exception {
    String change = Files.readString(CHANGELOG_ROOT.resolve(CHANGELOG));
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master)
        .contains(
            "file: " + CHANGELOG + System.lineSeparator() + "      relativeToChangelogFile: true");
    assertThat(change)
        .contains("dbms:\n            type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .doesNotContain("TIMESTAMP NOT NULL")
        .doesNotContain("WHERE id IN (SELECT", "UPDATE product product");

    try (DirectoryResourceAccessor resources =
        new DirectoryResourceAccessor(Path.of(".").toAbsolutePath().normalize())) {
      var database =
          DatabaseFactory.getInstance().openDatabase("offline:mysql", null, null, null, resources);
      var liquibase =
          new Liquibase("src/main/resources/db/changelog/" + CHANGELOG, resources, database);
      assertThat(liquibase.getDatabaseChangeLog().getChangeSets()).hasSize(3);
    }
  }

  /** Comprova atividade condicional, retorno visual e sequência completa de revalidação. */
  @Test
  void declaresExplicitCorrectionAndSequentialAgentValidation() throws Exception {
    String change = Files.readString(CHANGELOG_ROOT.resolve(CHANGELOG));
    var matcher = Pattern.compile("'(\\{\\\"nodes\\\".*?\\})',").matcher(change);
    assertThat(matcher.find()).isTrue();
    var diagram = new ObjectMapper().readTree(matcher.group(1));

    var correction =
        StreamSupport.stream(diagram.path("nodes").spliterator(), false)
            .filter(node -> "prototypeCorrection".equals(node.path("id").asText()))
            .findFirst()
            .orElseThrow();
    assertThat(correction.path("activationMode").asText()).isEqualTo("ON_FUNCTIONAL_REJECTION");
    assertThat(correction.path("actionLabel").asText()).isEqualTo("Criar tarefa de correção");
    assertThat(correction.path("responsibleAgentKeys").path(0).asText())
        .isEqualTo("landing-generator");
    assertThat(correction.path("remediatesActivities")).hasSize(5);

    assertThat(diagram.path("flows"))
        .anySatisfy(
            flow -> {
              assertThat(flow.path("from").asText()).isEqualTo("prototypeCorrection");
              assertThat(flow.path("to").asText()).isEqualTo("technicalHomologation");
              assertThat(flow.path("kind").asText()).isEqualTo("REWORK");
            })
        .anySatisfy(
            flow -> {
              assertThat(flow.path("from").asText()).isEqualTo("technicalHomologation");
              assertThat(flow.path("to").asText()).isEqualTo("psiqueAdherent");
              assertThat(flow.path("kind").asText()).isBlank();
            })
        .anySatisfy(
            flow -> {
              assertThat(flow.path("from").asText()).isEqualTo("psiqueAdherent");
              assertThat(flow.path("to").asText()).isEqualTo("psiqueRecovery");
              assertThat(flow.path("kind").asText()).isBlank();
            })
        .anySatisfy(
            flow -> {
              assertThat(flow.path("from").asText()).isEqualTo("psiqueRecovery");
              assertThat(flow.path("to").asText()).isEqualTo("psiqueSafety");
              assertThat(flow.path("kind").asText()).isBlank();
            });
    assertThat(change)
        .contains(
            "version_number = 8",
            "'mira-private-v2'",
            "version_number = 12",
            "PDE_AGENT_VALIDATION_REWORK_MIGRATED_ARTIFACT_V1")
        .doesNotContain(
            "{\"from\":\"technicalHomologation\",\"to\":\"psiqueRecovery\"}",
            "{\"from\":\"technicalHomologation\",\"to\":\"psiqueSafety\"}");
  }
}
