package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a matriz exclusiva dos oito agentes nos processos persistidos. */
class AgentResponsibilityMatrixChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String LEGACY_CHANGELOG =
      "changesets/2026-08-28-agent-responsibility-boundaries-v2.yaml";
  private static final String MATRIX_CHANGELOG =
      "changesets/2026-08-28-agent-responsibility-matrix-v3.yaml";
  private static final Pattern DIAGRAM =
      Pattern.compile("'(\\{\\\"nodes\\\".*?\\})',\\s*\\R\\s*UTC_TIMESTAMP", Pattern.DOTALL);
  private static final ObjectMapper JSON = new ObjectMapper();

  /**
   * Confirma que a correção incremental executa depois da versão histórica e usa include relativo.
   */
  @Test
  void shouldIncludeMatrixAfterHistoricalBoundary() throws Exception {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master.indexOf(LEGACY_CHANGELOG)).isGreaterThanOrEqualTo(0);
    assertThat(master.indexOf(MATRIX_CHANGELOG)).isGreaterThan(master.indexOf(LEGACY_CHANGELOG));
    assertThat(master)
        .contains(
            "file: "
                + MATRIX_CHANGELOG
                + System.lineSeparator()
                + "      relativeToChangelogFile: true");
  }

  /** Exige uma nova versão auditável para cada uma das oito identidades canônicas. */
  @Test
  void shouldVersionAllEightAgentContracts() throws Exception {
    String changelog = changelog();

    assertThat(changelog)
        .contains(
            "'market-radar'",
            "'experiment-strategist'",
            "'financial-agent'",
            "'landing-generator'",
            "'videomaker'",
            "'customer-agent'",
            "'meta-ad-approver'",
            "'growth-operator'",
            "'MARKET_EVIDENCE'",
            "'MARKET_STRATEGY'",
            "'FINANCIAL_VALIDATION'",
            "'PDE_CONSTRUCTION'",
            "'AUDIOVISUAL_PRODUCTION'",
            "'HUMAN_EXPERIENCE_REVIEW'",
            "'COMMERCIAL_INTEGRITY_REVIEW'",
            "'GROWTH_OPERATION'",
            "'matrixVersion', 'AGENT_RESPONSIBILITY_MATRIX_V1'");
  }

  /** Valida todos os diagramas novos como JSON e impede coautoria ou domínio incompatível. */
  @Test
  void shouldPublishOnlySingleOwnerAgentActivities() throws Exception {
    Matcher matcher = DIAGRAM.matcher(changelog());
    int diagrams = 0;
    int agentActivities = 0;
    Set<String> assignedAgents = new HashSet<>();
    Map<String, AgentContract> contracts = contracts();
    while (matcher.find()) {
      diagrams++;
      JsonNode diagram = JSON.readTree(matcher.group(1));
      for (JsonNode node : diagram.path("nodes")) {
        JsonNode keys = node.path("responsibleAgentKeys");
        if (!keys.isArray()) continue;
        agentActivities++;
        assertThat(keys).as(node.path("id").asText()).hasSize(1);
        String key = keys.get(0).asText();
        assignedAgents.add(key);
        AgentContract contract = contracts.get(key);
        assertThat(contract).as(key).isNotNull();
        assertThat(node.path("responsibilityDomain").asText()).isEqualTo(contract.domain());
        assertThat(node.path("owner").asText()).isEqualTo(contract.owner());
        assertThat(node.path("owner").asText()).doesNotContain(" e ", " ou ", ",", "/");
      }
    }

    assertThat(diagrams).isEqualTo(11);
    assertThat(agentActivities).isGreaterThanOrEqualTo(25);
    assertThat(assignedAgents).containsExactlyInAnyOrderElementsOf(contracts.keySet());
  }

  /** Confirma gates separados e a cadeia que libera Hermes somente após autorização humana. */
  @Test
  void shouldPublishIndependentGatesAndValueChainV8() throws Exception {
    String changelog = changelog();

    assertThat(changelog)
        .contains(
            "\"id\":\"humanExperienceReview\"",
            "\"id\":\"commercialIntegrityReview\"",
            "\"responsibleAgentKeys\":[\"customer-agent\"]",
            "\"responsibleAgentKeys\":[\"meta-ad-approver\"]",
            "Somente a autorização permite a operação posterior de Hermes",
            "version_number = 8")
        .doesNotContain(
            "\"responsibleAgentKeys\":[\"customer-agent\",\"meta-ad-approver\"]",
            "Têmis produz imagem",
            "Têmis traduz estratégia");
  }

  /** Garante que os recursos de produção pertençam a Dédalo e Apolo, nunca à revisora Têmis. */
  @Test
  void shouldReassignProductionResourcesToTheirCanonicalOwners() throws Exception {
    String changelog = changelog();

    assertThat(changelog)
        .contains(
            "WHERE resource_code = 'themis-image-studio'",
            "'pde-visual-materialization'",
            "'landing-generator'",
            "'video-management-service'",
            "'videomaker'",
            "executionResourceCode\":\"pde-visual-materialization",
            "executionResourceCode\":\"video-management-service")
        .doesNotContain("TEMIS_DIRECTION", "metadata/temis-direction.json");
  }

  /** Lê integralmente o changelog da matriz. */
  private String changelog() throws Exception {
    return Files.readString(CHANGELOG_ROOT.resolve(MATRIX_CHANGELOG));
  }

  /** Monta as oito combinações aceitas de identidade, domínio e responsável visível. */
  private Map<String, AgentContract> contracts() {
    Map<String, AgentContract> values = new LinkedHashMap<>();
    values.put("market-radar", new AgentContract("MARKET_EVIDENCE", "Argos"));
    values.put("experiment-strategist", new AgentContract("MARKET_STRATEGY", "Atena"));
    values.put("financial-agent", new AgentContract("FINANCIAL_VALIDATION", "Plutus"));
    values.put("landing-generator", new AgentContract("PDE_CONSTRUCTION", "Dédalo"));
    values.put("videomaker", new AgentContract("AUDIOVISUAL_PRODUCTION", "Apolo"));
    values.put("customer-agent", new AgentContract("HUMAN_EXPERIENCE_REVIEW", "Psique"));
    values.put("meta-ad-approver", new AgentContract("COMMERCIAL_INTEGRITY_REVIEW", "Têmis"));
    values.put("growth-operator", new AgentContract("GROWTH_OPERATION", "Hermes"));
    return values;
  }

  /** Representa a combinação única aceita em um nó de agente. */
  private record AgentContract(String domain, String owner) {}
}
