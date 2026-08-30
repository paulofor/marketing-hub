package com.marketinghub.agentdetail.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
          "communication-director",
          "market-radar",
          "videomaker");
  private static final Map<String, List<String>> BEHAVIOR_SOURCE_ROOTS =
      Map.of(
          "growth-operator",
          List.of("growth-operator-worker/src/main/resources/prompts"),
          "customer-agent",
          List.of("customer-agent-worker/src/main/resources/prompts"),
          "financial-agent",
          List.of("financial-agent-worker/src/main/resources/prompts"),
          "experiment-strategist",
          List.of(
              "experiment-strategist-worker/src/main/resources/prompts",
              "experiment-strategist-worker/src/main/resources/behavioral-science"),
          "meta-ad-approver",
          List.of("meta-ad-approver-worker/src/main/resources/prompts"),
          "landing-generator",
          List.of("landing-generator-agent-worker/src/main/resources/prompts"),
          "communication-director",
          List.of(
              "communication-agent-worker/src/main/resources/prompts",
              "meta-ad-approver-worker/src/main/resources/prompts/image-studio/v1/production.md"),
          "market-radar",
          List.of("product-discovery-worker/prompts"),
          "videomaker",
          List.of("video-management-service/src/main/resources/prompts"));
  private static final Map<String, Set<String>> FILES_OWNED_BY_ANOTHER_AGENT =
      Map.of(
          "meta-ad-approver",
          Set.of(
              "meta-ad-approver-worker/src/main/resources/prompts/image-studio/v1/production.md"));

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
          assertThat(harness.behaviorFiles()).isNotEmpty();
        });
  }

  /** Impede que qualquer prompt, schema ou biblioteca de comportamento fique fora do harness. */
  @Test
  void catalogsEveryBehaviorFileFromEveryAgentModule() throws IOException {
    JsonNode document = readManifest();
    Path repositoryRoot = repositoryRoot();
    Map<String, Set<String>> declaredByAgent = declaredBehaviorFiles(document);

    BEHAVIOR_SOURCE_ROOTS.forEach(
        (agentKey, sourceRoots) -> {
          try {
            Set<String> expected = repositoryBehaviorFiles(repositoryRoot, sourceRoots);
            expected.removeAll(FILES_OWNED_BY_ANOTHER_AGENT.getOrDefault(agentKey, Set.of()));
            assertThat(declaredByAgent.get(agentKey))
                .as("Cobertura comportamental do agente %s", agentKey)
                .containsExactlyInAnyOrderElementsOf(expected);
          } catch (IOException ex) {
            throw new IllegalStateException(
                "Não foi possível inspecionar as fontes comportamentais de " + agentKey + ".", ex);
          }
        });
  }

  /**
   * Confirma que a API entrega conteúdo integral e identidade criptográfica do arquivo original.
   */
  @Test
  void exposesExactBehaviorFileContentAndIntegrity() throws IOException {
    Path repositoryRoot = repositoryRoot();
    var harness = new AgentHarnessCatalog(new ObjectMapper()).getByAgentKey("customer-agent");
    var core =
        harness.behaviorFiles().stream()
            .filter(file -> file.path().endsWith("prompts/psique/behavioral-core-v4.md"))
            .findFirst()
            .orElseThrow();

    assertThat(core.behaviorType()).isEqualTo("PROMPT");
    assertThat(core.mediaType()).isEqualTo("text/markdown");
    assertThat(core.sha256()).matches("[a-f0-9]{64}");
    assertThat(core.content()).isEqualTo(Files.readString(repositoryRoot.resolve(core.path())));
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

    AgentHarnessCatalog catalog = new AgentHarnessCatalog(new ObjectMapper());
    EXPECTED_AGENT_KEYS.forEach(
        agentKey ->
            catalog
                .getByAgentKey(agentKey)
                .behaviorFiles()
                .forEach(
                    file ->
                        assertThat(file.content())
                            .as("Conteúdo seguro do arquivo %s", file.path())
                            .doesNotContainPattern(
                                "(?i)(sk-[A-Za-z0-9_-]{20,}|ghp_[A-Za-z0-9]{20,}|Bearer\\s+[A-Za-z0-9._-]{24,}|BEGIN (RSA )?PRIVATE KEY)")));
  }

  /** Garante que todo contrato JSON servido pela árvore do frontend seja sintaticamente válido. */
  @Test
  void exposesOnlyValidJsonBehaviorContracts() {
    ObjectMapper objectMapper = new ObjectMapper();
    AgentHarnessCatalog catalog = new AgentHarnessCatalog(objectMapper);

    EXPECTED_AGENT_KEYS.forEach(
        agentKey ->
            catalog.getByAgentKey(agentKey).behaviorFiles().stream()
                .filter(file -> "application/json".equals(file.mediaType()))
                .forEach(
                    file -> {
                      try {
                        assertThat(objectMapper.readTree(file.content())).isNotNull();
                      } catch (IOException ex) {
                        throw new IllegalStateException(
                            "Schema comportamental inválido em " + file.path() + ".", ex);
                      }
                    }));
  }

  /**
   * Torna os princípios afetivos e sensoriais de Psique visíveis sem abrir arquivos no frontend.
   */
  @Test
  void exposesPsiqueBehavioralAndSensoryConstitution() {
    var harness = new AgentHarnessCatalog(new ObjectMapper()).getByAgentKey("customer-agent");

    var constitution =
        harness.sections().stream()
            .filter(section -> "behavioral-constitution".equals(section.code()))
            .findFirst()
            .orElseThrow();

    assertThat(constitution.title()).isEqualTo("Constituição humana, sensorial e estética");
    assertThat(constitution.items())
        .extracting("key")
        .contains(
            "fundamental-drive",
            "affective-first",
            "sensory-modalities",
            "sensory-dimensions",
            "visual-composition",
            "human-connection",
            "sensory-scale",
            "evidence-boundary",
            "ethical-boundary");
    assertThat(harness.artifacts())
        .extracting("path")
        .contains(
            "customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v4.md",
            "customer-agent-worker/src/main/resources/prompts/customer-agent/behavioral-v4/evaluation-schema.json");
  }

  /** Mantém explícita a ausência de manifesto para agentes futuros ainda não catalogados. */
  @Test
  void reportsUnknownHarnessWithoutInference() {
    var harness = new AgentHarnessCatalog(new ObjectMapper()).getByAgentKey("future-agent");

    assertThat(harness.status()).isEqualTo("NOT_REGISTERED");
    assertThat(harness.sections()).isEmpty();
    assertThat(harness.artifacts()).isEmpty();
    assertThat(harness.behaviorFiles()).isEmpty();
  }

  /** Agrupa por agente as fontes comportamentais declaradas no manifesto. */
  private Map<String, Set<String>> declaredBehaviorFiles(JsonNode document) {
    Map<String, Set<String>> result = new HashMap<>();
    for (JsonNode agent : document.path("agents")) {
      Set<String> paths = new TreeSet<>();
      for (JsonNode artifact : agent.path("artifacts")) {
        String artifactType = artifact.path("artifactType").asText();
        if (Set.of("PROMPT", "OUTPUT_SCHEMA", "BEHAVIOR_LIBRARY").contains(artifactType)) {
          paths.add(artifact.path("path").asText());
        }
      }
      result.put(agent.path("agentKey").asText(), paths);
    }
    return result;
  }

  /** Descobre os arquivos Markdown e JSON existentes nos diretórios comportamentais canônicos. */
  private Set<String> repositoryBehaviorFiles(Path repositoryRoot, List<String> sourceRoots)
      throws IOException {
    Set<String> result = new TreeSet<>();
    for (String sourceRoot : sourceRoots) {
      try (var files = Files.walk(repositoryRoot.resolve(sourceRoot))) {
        files
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".md") || path.toString().endsWith(".json"))
            .map(repositoryRoot::relativize)
            .map(Path::toString)
            .forEach(result::add);
      }
    }
    return result;
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
