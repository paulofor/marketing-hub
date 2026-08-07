package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato de isolamento do executor Estrategista. */
class CodexStrategistRunnerTest {
  /** Confirma busca publica, sandbox somente leitura e schema versionado. */
  @Test
  void buildsReadOnlyResearchCommand() {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/marketing-hub");
    properties.setModel("gpt-5.6-sol");
    CodexStrategistRunner runner = new CodexStrategistRunner(properties, new ObjectMapper());

    var command = runner.command(Path.of("/tmp/output.json"), Path.of("/tmp/schema.json"));

    assertThat(command).contains("--search", "--sandbox", "read-only", "--output-schema");
    assertThat(command).doesNotContain("danger-full-access");
    assertThat(properties.getCodexTimeout().toMinutes()).isEqualTo(40);
  }

  /** Confirma que navegador, pesquisa externa e procedência entram no artefato versionado. */
  @Test
  void packagesAuditableBrowserResearch() throws Exception {
    String dockerfile = Files.readString(Path.of("Dockerfile"));
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v1/research.md"));
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v1/research-schema.json"));

    assertThat(dockerfile)
        .contains("FROM eclipse-temurin:21-jre-noble")
        .contains("npx playwright-core install --with-deps chromium")
        .contains("COPY --from=build /build/src/main/resources/browser /app/browser");
    assertThat(prompt)
        .contains("node /app/browser/public-research.mjs")
        .contains("duas classes independentes de evidência")
        .contains("mapa comparativo dos concorrentes")
        .contains("linguagem literal pública de clientes")
        .contains("o mercado oferece X, mas o cliente ainda precisa fazer Y");
    assertThat(schema)
        .contains("marketIntelligence", "customerLanguage", "competitors", "customerEffort")
        .contains("evidenceClass", "statementType", "positioning", "memoryOutcome");
  }

  /** Confirma que constantes booleanas mantêm o tipo exigido pelo Structured Outputs. */
  @Test
  void declaresTypeForBooleanConstantInStrictSchema() throws Exception {
    var schema =
        new ObjectMapper()
            .readTree(
                Path.of("src/main/resources/prompts/experiment-strategist/v1/research-schema.json")
                    .toFile());

    var approval =
        schema
            .path("properties")
            .path("recommendation")
            .path("properties")
            .path("requiresHumanApproval");

    assertThat(approval.path("type").asText()).isEqualTo("boolean");
    assertThat(approval.path("const").asBoolean()).isTrue();
  }
}
