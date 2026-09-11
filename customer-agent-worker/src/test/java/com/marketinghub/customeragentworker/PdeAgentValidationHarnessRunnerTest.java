package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar as fronteiras do executor determinístico multiagente. */
class PdeAgentValidationHarnessRunnerTest {
  @TempDir Path temporaryDirectory;
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz #377 e explica ausência de implementação antes de iniciar navegador ou integração. */
  @Test
  void rejectsPlannedCycleWithoutExecutableUrl() {
    var target = new HashMap<String, Object>((Map<String, Object>) task().get("taskTarget"));
    target.put("publicUrl", null);
    var task = new HashMap<>(task());
    task.put("taskTarget", target);
    task.put("sourceReference", "experiment:92");
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "test", true);

    assertThatThrownBy(() -> runner.run(task, "TECHNICAL", null, temporaryDirectory))
        .hasMessageContaining("não possui URL executável")
        .hasMessageContaining("implementação");
  }

  /** Impede atribuir cenários específicos de Mira a outro produto mesmo com URL preenchida. */
  @Test
  void rejectsAnotherProductBeforeLaunchingMiraScenarios() {
    var target = new HashMap<String, Object>((Map<String, Object>) task().get("taskTarget"));
    target.put("productId", 4L);
    target.put("productSlug", "metodo-musa");
    target.put("experienceVersion", "vega-v8");
    var task = new HashMap<>(task());
    task.put("taskTarget", target);
    task.put("sourceReference", "product:4@agent-validation-v1");
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "test", true);

    assertThatThrownBy(() -> runner.run(task, "TECHNICAL", null, temporaryDirectory))
        .hasMessageContaining("cenários somente para o protótipo privado de Mira");
  }

  /** Exige que a referência e o alvo declarem a mesma identidade de produto. */
  @Test
  void rejectsProductReferenceFromAnotherProduct() {
    var task = new HashMap<>(task());
    task.put("sourceReference", "product:4@agent-validation-v1");
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "test", true);

    assertThatThrownBy(() -> runner.run(task, "TECHNICAL", null, temporaryDirectory))
        .hasMessageContaining("não corresponde ao produto alvo");
  }

  /** Aceita somente cobertura completa, PNG local e efeitos comerciais nulos. */
  @Test
  void acceptsCompleteTechnicalHarnessWithoutPersistingSecret() throws Exception {
    Path script = fakeHarness(false, true);
    var runner =
        new PdeAgentValidationHarnessRunner(
            json, "/bin/sh", script.toString(), "protected-internal-token", true);

    var execution = runner.run(task(), "TECHNICAL", null, temporaryDirectory.resolve("execution"));

    assertThat(execution.result().path("decision").asText()).isEqualTo("APPROVED");
    assertThat(execution.visualEvidence().capture().artifacts()).hasSize(5);
    assertThat(execution.serializedInput()).doesNotContain("protected-internal-token");
    assertThat(execution.result().toString()).doesNotContain("protected-internal-token");
    assertThat(
            PdeAgentValidationHarnessConsumer.supportsContract(
                "pde-construction-approval", "technicalHomologation"))
        .isTrue();
  }

  /** Vincula o executável do sucessor ao experimento e despacha os cenários próprios de Vega. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"9", "10", "11", "12", "20", "100"})
  void acceptsVegaCycleWithOwnHarnessAndExplicitLineage(String version) throws Exception {
    Path mira = fakeHarness(false, true);
    Path vega = mira.resolveSibling("vega-agent-validation-harness.mjs");
    Files.writeString(
        vega,
        Files.readString(mira)
            .replace("product:10@agent-validation-v1", "experiment:92")
            .replace("\"productId\":10", "\"productId\":4")
            .replace("orientacao-digital-rotina-pele-madura", "metodo-musa-7-dias")
            .replace("mira-private-v1", "musa-pde-entry-v" + version + "-primeiro-ajuste-aplicavel")
            .replace("/mira-private", "/vega-private"));
    var target = new HashMap<String, Object>();
    target.put("productId", 4L);
    target.put("experimentId", 92L);
    target.put("productSlug", "metodo-musa-7-dias");
    target.put("experienceVersion", "musa-pde-entry-v" + version + "-primeiro-ajuste-aplicavel");
    target.put("publicUrl", "http://127.0.0.1:5176/vega-private");
    target.put(
        "pdeContext",
        Map.of("lineage", Map.of("productId", 4L, "experimentId", 92L, "learningCycleId", 2L)));
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/bin/sh", mira.toString(), "synthetic", true);
    var execution =
        runner.run(
            Map.of("taskId", 901L, "sourceReference", "experiment:92", "taskTarget", target),
            "TECHNICAL",
            null,
            temporaryDirectory.resolve("vega"));
    assertThat(execution.result().path("decision").asText()).isEqualTo("APPROVED");
    assertThat(json.readTree(execution.serializedInput()).path("cycleId").asLong()).isEqualTo(2L);
    assertThat(execution.visualEvidence().capture().artifacts())
        .allMatch(a -> "FULL_PAGE".equals(a.evidenceType()));
  }

  /**
   * Recusa especificações antigas e nomes fora da família executável antes de iniciar navegador.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"8", "012", "12-outra-familia"})
  void rejectsUnsupportedVegaVersion(String version) {
    var target =
        Map.of(
            "productId",
            4L,
            "experimentId",
            92L,
            "productSlug",
            "metodo-musa-7-dias",
            "experienceVersion",
            "musa-pde-entry-v" + version + "-primeiro-ajuste-aplicavel",
            "publicUrl",
            "http://127.0.0.1:5176/vega-private",
            "pdeContext",
            Map.of("lineage", Map.of("productId", 4L, "experimentId", 92L, "learningCycleId", 2L)));
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "test", true);
    assertThatThrownBy(
            () ->
                runner.run(
                    Map.of(
                        "taskId", 900L, "sourceReference", "experiment:92", "taskTarget", target),
                    "TECHNICAL",
                    null,
                    temporaryDirectory))
        .hasMessageContaining("não corresponde ao produto alvo");
  }

  /** Recusa metadado que o backend não aceita, antes de enviar screenshots ao callback. */
  @Test
  void rejectsUnsupportedScreenshotTypeBeforeCallback() throws Exception {
    Path script = fakeHarness(false, true);
    Files.writeString(
        script,
        Files.readString(script)
            .replace("\"evidenceType\":\"FULL_PAGE\"", "\"evidenceType\":\"SCREENSHOT\""));
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/bin/sh", script.toString(), "synthetic", true);
    assertThatThrownBy(
            () -> runner.run(task(), "TECHNICAL", null, temporaryDirectory.resolve("invalid-type")))
        .isInstanceOf(PdeAgentValidationHarnessRunner.HarnessException.class);
  }

  /** Rejeita uma saída que tente transformar o harness em prova humana. */
  @Test
  void rejectsHumanEvidenceClaimBeforeCallback() throws Exception {
    Path script = fakeHarness(true, true);
    var runner =
        new PdeAgentValidationHarnessRunner(
            json, "/bin/sh", script.toString(), "protected-internal-token", true);

    assertThatThrownBy(
            () ->
                runner.run(
                    task(), "TECHNICAL", null, temporaryDirectory.resolve("forged-execution")))
        .isInstanceOf(PdeAgentValidationHarnessRunner.HarnessException.class)
        .hasMessageContaining("Contrato funcional");
  }

  /** Preserva a reprovação estruturada para o callback registrar a causa funcional. */
  @Test
  void returnsCompleteBlockedHarnessForAuditableFailureCallback() throws Exception {
    Path script = fakeHarness(false, false);
    var runner =
        new PdeAgentValidationHarnessRunner(
            json, "/bin/sh", script.toString(), "protected-internal-token", true);

    var execution =
        runner.run(task(), "TECHNICAL", null, temporaryDirectory.resolve("blocked-execution"));

    assertThat(execution.result().path("decision").asText()).isEqualTo("BLOCKED");
    assertThat(execution.result().path("checks").path("responsiveLayout").asBoolean()).isFalse();
  }

  /** Cria uma tarefa sintética com o alvo público exato do produto. */
  private Map<String, Object> task() {
    return Map.of(
        "taskId",
        900L,
        "sourceReference",
        "product:10@agent-validation-v1",
        "taskTarget",
        Map.of(
            "productId",
            10L,
            "productSlug",
            "orientacao-digital-rotina-pele-madura",
            "experienceVersion",
            "mira-private-v1",
            "publicUrl",
            "http://127.0.0.1:5176/mira-private"));
  }

  /** Materializa um processo falso que devolve o mesmo contrato usado pelo script real. */
  private Path fakeHarness(boolean humanEvidenceClaimed, boolean approved) throws Exception {
    Path script =
        temporaryDirectory.resolve("fake-harness-" + humanEvidenceClaimed + "-" + approved + ".sh");
    Files.writeString(
        script,
        """
        #!/bin/sh
        set -eu
        input="$1"
        output="$2"
        evidence="$3"
        mkdir -p "$evidence"
        png="$evidence/adherent-desktop.png"
        printf '\\211PNG\\r\\n\\032\\n' > "$png"
        capture=$(grep -o '"captureSessionId":"[^"]*"' "$input" | cut -d'"' -f4)
        cat > "$output" <<JSON
        {
          "contractVersion":"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
          "mode":"TECHNICAL",
          "decision":"%s",
          "sourceReference":"product:10@agent-validation-v1",
          "productId":10,
          "productSlug":"orientacao-digital-rotina-pele-madura",
          "publicUrl":"http://127.0.0.1:5176/mira-private",
          "prototypeVersion":"mira-private-v1",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "humanEvidenceClaimed":%s,
          "commercialEvidenceClaimed":false,
          "checks":{
            "sameVersion":true,"desktopAndMobile":true,"happyResultWithinTenMinutes":true,
            "recoveryPreserved":true,"safetyBlocked":true,"accessibilityBasic":true,
            "responsiveLayout":%s,"privacyPreserved":true,"internalTrafficSegregated":true,
            "paymentDisabled":true,"publicationDisabled":true,"campaignDisabled":true,
            "zeroMediaSpend":true
          },
          "devices":[
            {"deviceProfile":"DESKTOP_1440","status":"PASS"},
            {"deviceProfile":"IPHONE_15_PRO","status":"PASS"},
            {"deviceProfile":"PIXEL_7","status":"PASS"}
          ],
          "scenarios":[
            {"scenarioCode":"ADHERENT","status":"PASS","humanEvidenceClaimed":false,"commercialEvidenceClaimed":false,"sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}},
            {"scenarioCode":"ADHERENT","status":"PASS","humanEvidenceClaimed":false,"commercialEvidenceClaimed":false,"sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}},
            {"scenarioCode":"ADHERENT","status":"PASS","humanEvidenceClaimed":false,"commercialEvidenceClaimed":false,"sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}},
            {"scenarioCode":"RECOVERY","status":"PASS","humanEvidenceClaimed":false,"commercialEvidenceClaimed":false,"sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}},
            {"scenarioCode":"SAFETY","status":"PASS","humanEvidenceClaimed":false,"commercialEvidenceClaimed":false,"sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}}
          ],
          "artifacts":[
            {"captureSessionId":"$capture","evidenceKey":"ADHERENT-DESKTOP_1440-FULL_PAGE","evidenceType":"FULL_PAGE","deviceProfile":"DESKTOP_1440","pageNumber":1,"foldNumber":null,"viewportWidth":1440,"viewportHeight":900,"pageHeightPx":900,"scrollY":0,"sourceUrl":"http://127.0.0.1:5176/mira-private","finalUrl":"http://127.0.0.1:5176/mira-private","capturedAt":"2026-09-06T12:00:00Z","localPath":"$png"},
            {"captureSessionId":"$capture","evidenceKey":"ADHERENT-IPHONE_15_PRO-FULL_PAGE","evidenceType":"FULL_PAGE","deviceProfile":"IPHONE_15_PRO","pageNumber":1,"foldNumber":null,"viewportWidth":393,"viewportHeight":852,"pageHeightPx":900,"scrollY":0,"sourceUrl":"http://127.0.0.1:5176/mira-private","finalUrl":"http://127.0.0.1:5176/mira-private","capturedAt":"2026-09-06T12:00:01Z","localPath":"$png"},
            {"captureSessionId":"$capture","evidenceKey":"ADHERENT-PIXEL_7-FULL_PAGE","evidenceType":"FULL_PAGE","deviceProfile":"PIXEL_7","pageNumber":1,"foldNumber":null,"viewportWidth":412,"viewportHeight":915,"pageHeightPx":900,"scrollY":0,"sourceUrl":"http://127.0.0.1:5176/mira-private","finalUrl":"http://127.0.0.1:5176/mira-private","capturedAt":"2026-09-06T12:00:02Z","localPath":"$png"},
            {"captureSessionId":"$capture","evidenceKey":"RECOVERY-IPHONE_15_PRO-FULL_PAGE","evidenceType":"FULL_PAGE","deviceProfile":"IPHONE_15_PRO","pageNumber":1,"foldNumber":null,"viewportWidth":393,"viewportHeight":852,"pageHeightPx":900,"scrollY":0,"sourceUrl":"http://127.0.0.1:5176/mira-private","finalUrl":"http://127.0.0.1:5176/mira-private","capturedAt":"2026-09-06T12:00:03Z","localPath":"$png"},
            {"captureSessionId":"$capture","evidenceKey":"SAFETY-PIXEL_7-FULL_PAGE","evidenceType":"FULL_PAGE","deviceProfile":"PIXEL_7","pageNumber":1,"foldNumber":null,"viewportWidth":412,"viewportHeight":915,"pageHeightPx":900,"scrollY":0,"sourceUrl":"http://127.0.0.1:5176/mira-private","finalUrl":"http://127.0.0.1:5176/mira-private","capturedAt":"2026-09-06T12:00:04Z","localPath":"$png"}
          ],
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}
        }
        JSON
        """
            .formatted(approved ? "APPROVED" : "BLOCKED", humanEvidenceClaimed, approved));
    script.toFile().setExecutable(true);
    return script;
  }
}
