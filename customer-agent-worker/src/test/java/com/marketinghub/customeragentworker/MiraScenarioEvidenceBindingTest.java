package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: comprovar a passagem de capturas reais para a interpretação de Psique. */
class MiraScenarioEvidenceBindingTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz a omissão que bloqueou a tarefa #609 antes de qualquer inferência paga. */
  @Test
  void rejectsScenarioWithoutItsExplicitCaptureReference() throws Exception {
    var raw = rawScenario("[]");
    assertThatThrownBy(() -> bind(raw)).hasMessageContaining("Cenário sem vínculo explícito");
  }

  /** Impede associar um cenário à captura de outra execução ou cenário. */
  @Test
  void rejectsReferenceToAnUnpersistedCapture() throws Exception {
    var raw = rawScenario("[\"other-scenario\"]");
    assertThatThrownBy(() -> bind(raw))
        .hasMessageContaining("Referência visual do cenário de Psique não foi persistida");
  }

  /** Preserva o contrato já válido dos protótipos legados com chave explícita. */
  @Test
  void preservesExistingExplicitCaptureContract() throws Exception {
    var result = bind(rawScenario("[\"ADHERENT-DESKTOP_1440\"]"));
    assertThat(result.path("scenarios").get(0).path("screenshotEvidenceIds").get(0).asLong())
        .isEqualTo(10_000L);
    assertThat(result.toString()).doesNotContain("localPath");
    assertThat(result.path("scenarios").toString()).doesNotContain("screenshotEvidenceKeys");
  }

  /** Valida os três browsers reais e a sanitização usada antes do parecer independente. */
  @ParameterizedTest
  @ValueSource(strings = {"ADHERENT", "RECOVERY", "SAFETY"})
  void bindsRealScenarioOutputBeforeModel(String scenario) throws Exception {
    String directory = System.getenv("MIRA_LOCAL_SCENARIO_REPORT_DIR");
    Assumptions.assumeTrue(directory != null && !directory.isBlank());
    Path report = Path.of(directory).resolve("scenario-" + scenario + ".json");
    var raw = json.readTree(report.toFile());
    var input =
        json.readTree(report.resolveSibling("scenario-" + scenario + "-input.json").toFile());
    var expected = json.convertValue(input, new TypeReference<Map<String, Object>>() {});
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "synthetic", true);
    List<?> artifacts =
        ReflectionTestUtils.invokeMethod(
            runner,
            "validateOutput",
            raw,
            input.path("captureSessionId").asText(),
            report.resolveSibling("captures-" + scenario),
            "SCENARIO",
            scenario,
            expected);
    assertThat(artifacts).hasSize(1);
    var result = bind(raw);
    assertThat(result.path("scenarios").get(0).path("screenshotEvidenceIds")).hasSize(1);
    assertThat(result.path("artifacts").get(0).path("artifactId").asLong()).isEqualTo(10_000L);
    assertThat(result.path("sourceReference").asText())
        .isEqualTo("RECOVERY".equals(scenario) ? "experiment:9017" : "experiment:9006");
    assertThat(result.path("providerCalls").asInt()).isZero();
    assertThat(result.toString()).doesNotContain("localPath");
    assertThat(result.path("scenarios").toString()).doesNotContain("screenshotEvidenceKeys");
  }

  /** Confirma que os dezoito percursos técnicos também recebem ids individuais persistíveis. */
  @Test
  void bindsEveryActualTechnicalScenario() throws Exception {
    String report = System.getenv("MIRA_LOCAL_REPORT");
    Assumptions.assumeTrue(report != null && !report.isBlank());
    var result = bind(json.readTree(Path.of(report).toFile()));
    assertThat(result.path("scenarios")).hasSize(18);
    for (JsonNode scenario : result.path("scenarios"))
      assertThat(scenario.path("screenshotEvidenceIds")).hasSize(1);
  }

  /** Usa somente o consumidor real e recibos locais, sem executar modelo ou acessar produção. */
  private JsonNode bind(JsonNode raw) throws Exception {
    var consumer =
        new CustomerBpmTaskConsumer(
            "http://127.0.0.1:1", "/must-not-run", "fixture-model", "max", "/absent", "", json);
    return ReflectionTestUtils.invokeMethod(consumer, "sanitizeAgentScenario", raw, receipts(raw));
  }

  /** Simula recibos do backend a partir das mesmas capturas PNG produzidas na sandbox. */
  private List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> receipts(JsonNode raw)
      throws Exception {
    var result = new ArrayList<BpmVisualEvidenceBackendClient.UploadedVisualEvidence>();
    for (JsonNode artifact : raw.path("artifacts")) {
      Path file = Path.of(artifact.path("localPath").asText());
      byte[] bytes = Files.exists(file) ? Files.readAllBytes(file) : new byte[0];
      String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      result.add(
          new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
              10_000L + result.size(),
              artifact.path("captureSessionId").asText(),
              artifact.path("evidenceKey").asText(),
              "FULL_PAGE",
              "Fixture privada",
              artifact.path("deviceProfile").asText(),
              1,
              null,
              1440,
              900,
              900,
              0,
              "http://127.0.0.1:57181/mira-candidate",
              "http://127.0.0.1:57181/mira-candidate",
              "/local-evidence/" + result.size(),
              (long) bytes.length,
              sha,
              Instant.parse("2026-10-06T00:00:00Z"),
              file.toString()));
    }
    return result;
  }

  /** Representa o contrato mínimo da passagem visual anterior, sem identidade de produto real. */
  private JsonNode rawScenario(String keys) throws Exception {
    return json.readTree(
        """
        {"artifacts":[{"evidenceKey":"ADHERENT-DESKTOP_1440","captureSessionId":"local",
          "deviceProfile":"DESKTOP_1440","localPath":"/absent-fixture.png"}],
         "scenarios":[{"scenarioCode":"ADHERENT","screenshotEvidenceKeys":%s}]}
        """
            .formatted(keys));
  }
}
