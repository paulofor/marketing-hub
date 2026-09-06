package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar as fronteiras do executor determinístico multiagente. */
class PdeAgentValidationHarnessRunnerTest {
  @TempDir Path temporaryDirectory;
  private final ObjectMapper json = new ObjectMapper();

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
