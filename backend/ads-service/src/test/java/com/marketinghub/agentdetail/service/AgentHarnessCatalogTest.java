package com.marketinghub.agentdetail.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: proteger a cobertura e as referências versionadas do catálogo de harness. */
class AgentHarnessCatalogTest {
  private static final Set<String> EXPECTED_AGENT_KEYS =
      Set.of(
          "growth-operator",
          "customer-agent",
          "financial-agent",
          "experiment-strategist",
          "meta-ad-approver",
          "landing-generator",
          "market-radar",
          "videomaker");

  /**
   * Exige harness completo para todos os agentes atualmente cadastrados no catálogo operacional.
   */
  @Test
  void coversEveryOperationalAgent() {
    AgentHarnessCatalog catalog = new AgentHarnessCatalog(new ObjectMapper());

    EXPECTED_AGENT_KEYS.forEach(
        agentKey -> {
          var harness = catalog.getByAgentKey(agentKey);
          assertThat(harness.status()).isEqualTo("COMPLETE");
          assertThat(harness.sections())
              .extracting("code")
              .contains(
                  "executor", "runtime", "orchestration", "memory", "security", "observability");
          assertThat(harness.artifacts())
              .extracting("artifactType")
              .contains("PROMPT", "OUTPUT_SCHEMA");
        });
  }

  /** Impede que o backend anuncie como versionado um arquivo ausente no repositório. */
  @Test
  void referencesOnlyExistingRepositoryArtifacts() throws IOException {
    JsonNode document = readManifest();
    Path repositoryRoot = repositoryRoot();

    for (JsonNode agent : document.path("agents")) {
      for (JsonNode artifact : agent.path("artifacts")) {
        String relativePath = artifact.path("path").asText();
        assertThat(repositoryRoot.resolve(relativePath))
            .as("Artefato do harness deve existir: %s", relativePath)
            .isRegularFile();
      }
    }
  }

  /** Bloqueia valores que se pareçam com credenciais no manifesto servido pela tela. */
  @Test
  void neverEmbedsSensitiveValues() throws IOException {
    JsonNode document = readManifest();
    String serializedDocument = document.toString();

    assertThat(serializedDocument).doesNotContain("${");
    assertThat(serializedDocument).doesNotContainPattern("(?i)(sk-|ghp_|Bearer\\s+[A-Za-z0-9])");
  }

  /** Mantém explícita a ausência de manifesto para agentes futuros ainda não catalogados. */
  @Test
  void reportsUnknownHarnessWithoutInference() {
    var harness = new AgentHarnessCatalog(new ObjectMapper()).getByAgentKey("future-agent");

    assertThat(harness.status()).isEqualTo("NOT_REGISTERED");
    assertThat(harness.sections()).isEmpty();
    assertThat(harness.artifacts()).isEmpty();
  }

  /** Lê o mesmo documento empacotado que alimenta o endpoint administrativo. */
  private JsonNode readManifest() throws IOException {
    try (InputStream input =
        new ClassPathResource(AgentHarnessCatalog.MANIFEST_PATH).getInputStream()) {
      return new ObjectMapper().readTree(input);
    }
  }

  /** Localiza a raiz do monorepositório sem depender do diretório usado pelo executor de testes. */
  private Path repositoryRoot() {
    Path candidate = Path.of("").toAbsolutePath().normalize();
    while (candidate != null) {
      if (Files.isRegularFile(candidate.resolve("AGENTS.md"))
          && Files.isDirectory(candidate.resolve("backend"))
          && Files.isDirectory(candidate.resolve("frontend"))) {
        return candidate;
      }
      candidate = candidate.getParent();
    }
    throw new IllegalStateException("Raiz do repositório não encontrada para validar o harness.");
  }
}
