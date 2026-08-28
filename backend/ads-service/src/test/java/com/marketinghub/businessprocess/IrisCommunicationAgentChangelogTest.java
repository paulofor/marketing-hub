package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a criação e a redistribuição BPM da nona agente Íris. */
class IrisCommunicationAgentChangelogTest {
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-28-iris-communication-agent-v1.yaml");
  private final ObjectMapper json = new ObjectMapper();

  /** Executa a evolução depois da matriz histórica e sempre com include relativo. */
  @Test
  void shouldIncludeIncrementAfterResponsibilityMatrix() throws Exception {
    String master = Files.readString(MASTER);
    int matrix = master.indexOf("2026-08-28-agent-responsibility-matrix-v3.yaml");
    int iris = master.indexOf("2026-08-28-iris-communication-agent-v1.yaml");

    assertThat(matrix).isGreaterThanOrEqualTo(0);
    assertThat(iris).isGreaterThan(matrix);
    assertThat(master.substring(iris, Math.min(master.length(), iris + 160)))
        .contains("relativeToChangelogFile: true");
  }

  /** Cadastra identidade, contratos e transfere os recursos comerciais legados para Íris. */
  @Test
  void shouldCreateOperationalIrisAndRestrictDedalo() throws Exception {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains(
            "'communication-director'",
            "'Íris'",
            "'COMMUNICATION_MATERIALIZATION'",
            "'iris-communication-worker'",
            "'communication-agent-worker'",
            "prompts/iris/v1/behavioral-core.md",
            "prompts/iris/v1/output-schema.json",
            "não cria copy, landing, anúncio, e-mail ou comunicação pré-compra",
            "WHERE resource_code = 'themis-image-studio'",
            "WHERE resource_code = 'pde-visual-materialization'",
            "executor_reference = 'iris-image-studio'",
            "não produz entregáveis nem prova de produto");
  }

  /** Publica somente atividades com um agente e domínio correspondente à matriz vigente. */
  @Test
  void shouldPublishSingleOwnerCompatibleActivities() throws Exception {
    List<JsonNode> diagrams = diagrams(Files.readAllLines(CHANGELOG));
    int assignedTasks = 0;
    int irisTasks = 0;
    for (JsonNode diagram : diagrams) {
      for (JsonNode node : diagram.path("nodes")) {
        JsonNode keys = node.path("responsibleAgentKeys");
        if (!keys.isArray()) continue;
        assignedTasks++;
        assertThat(keys).hasSize(1);
        String key = keys.get(0).asText();
        String domain = node.path("responsibilityDomain").asText();
        if ("communication-director".equals(key)) {
          irisTasks++;
          assertThat(domain).isEqualTo("COMMUNICATION_MATERIALIZATION");
          assertThat(node.path("owner").asText()).isEqualTo("Íris");
        }
        assertThat(key + ":" + domain)
            .isNotEqualTo("landing-generator:COMMUNICATION_MATERIALIZATION")
            .isNotEqualTo("meta-ad-approver:COMMUNICATION_MATERIALIZATION");
      }
    }

    assertThat(diagrams).hasSize(4);
    assertThat(assignedTasks).isGreaterThan(10);
    assertThat(irisTasks).isEqualTo(6);
  }

  /** Mantém SQL compatível com MySQL 5.7, idempotente e sem padrão 1093. */
  @Test
  void shouldKeepMysql57Contract() throws Exception {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains("type: mysql", "splitStatements: true", "stripComments: true")
        .doesNotContain("TIMESTAMP NOT NULL", "CURRENT_TIMESTAMP(6)");
    assertThat(changelog.split(";")).allSatisfy(this::assertNoMysql1093Pattern);
    assertThat(changelog)
        .contains(
            "WHERE NOT EXISTS (",
            "version_number = 7",
            "version_number = 8",
            "version_number = 6",
            "version_number = 5",
            "version_number = 9");
  }

  /** Exige que a nova agente seja aplicada, consultada e reaplicada no MySQL 5.7 físico. */
  @Test
  void shouldKeepDedicatedMysql57PhysicalValidation() throws Exception {
    Path moduleRoot = Path.of("").toAbsolutePath();
    String baseline =
        Files.readString(
            moduleRoot.resolve(
                "src/test/resources/liquibase-mysql57/agent-responsibility-boundaries-v2-baseline.sql"));
    String compose =
        Files.readString(
            moduleRoot.resolve("docker-compose.agent-responsibility-boundaries-mysql57.yml"));
    String runner =
        Files.readString(
            moduleRoot.resolve("scripts/validate-agent-responsibility-boundaries-mysql57.sh"));
    String workflow =
        Files.readString(moduleRoot.resolve("../../.github/workflows/liquibase-mysql57.yml"));

    assertThat(baseline)
        .contains(
            "CREATE TABLE agent_theme", "CREATE TABLE agent_input", "CREATE TABLE agent_output");
    assertThat(compose)
        .contains(
            "liquibase-iris-communication-agent", "2026-08-28-iris-communication-agent-v1.yaml");
    assertThat(runner)
        .contains(
            "compose run --rm --build liquibase-iris-communication-agent",
            "communication-director:1",
            "matriz dos nove agentes",
            "a reaplicação da Íris duplicou atividades");
    assertThat(workflow).contains("validate-agent-responsibility-boundaries-mysql57.sh");
  }

  /** Valida o risco 1093 dentro de cada comando sem misturar SQLs independentes do changelog. */
  private void assertNoMysql1093Pattern(String statement) {
    java.util.regex.Matcher target =
        java.util.regex.Pattern.compile("(?is)^\\s*(?:UPDATE|DELETE\\s+FROM)\\s+([a-z0-9_]+)")
            .matcher(statement);
    if (!target.find()) return;
    String table = java.util.regex.Pattern.quote(target.group(1));
    assertThat(statement).doesNotMatch("(?is).*SELECT.+FROM\\s+`?" + table + "`?(?:\\s|$).*");
  }

  /** Extrai os diagramas JSON literais dos processos criados pelo changelog. */
  private List<JsonNode> diagrams(List<String> lines) throws Exception {
    List<JsonNode> diagrams = new ArrayList<>();
    for (String line : lines) {
      int start = line.indexOf("{\"nodes\"");
      if (start < 0) continue;
      int end = line.lastIndexOf("}',");
      assertThat(end).isGreaterThan(start);
      diagrams.add(json.readTree(line.substring(start, end + 1)));
    }
    return diagrams;
  }
}
