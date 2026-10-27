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

  /** A v16 usa nove combinações próprias e recusa o recibo genérico de uma versão anterior. */
  @Test
  void contextualVersionRequiresOwnRulesAndNineDistinctCombinations() throws Exception {
    Path template = fakeHarness(false, true);
    String script = Files.readString(template);
    int start = script.indexOf("{\n", script.indexOf("<<JSON"));
    int end = script.lastIndexOf("\nJSON");
    var proof =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(script.substring(start, end));
    String version = "musa-pde-entry-v16-modelagem-internacional";
    String url = "http://127.0.0.1:5176/experiences/" + version + "/agent-validation";
    proof
        .put("sourceReference", "experiment:91092")
        .put("productId", 91004)
        .put("productSlug", "metodo-musa-7-dias")
        .put("publicUrl", url)
        .put("prototypeVersion", version)
        .put("runtimeContract", "PDE_CONTEXTUAL_STYLE_EXECUTION_V1")
        .put("generationMode", "DETERMINISTIC_CONTEXTUAL_RULES")
        .put("providerCalls", 0)
        .put("inputComparisonApplicable", false);
    ((com.fasterxml.jackson.databind.node.ObjectNode) proof.path("checks"))
        .put("sevenDayJourney", true);
    var scenario =
        (com.fasterxml.jackson.databind.node.ObjectNode) proof.path("scenarios").get(0).deepCopy();
    var capture =
        (com.fasterxml.jackson.databind.node.ObjectNode) proof.path("artifacts").get(0).deepCopy();
    var scenarios = proof.putArray("scenarios");
    var captures = proof.putArray("artifacts");
    for (String code : java.util.List.of("ADHERENT", "RECOVERY", "SAFETY"))
      for (String device : java.util.List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
        var item = scenario.deepCopy();
        item.put("scenarioCode", code).put("deviceProfile", device);
        item.putObject("operation").put("status", "SAFETY".equals(code) ? "BLOCKED" : "COMPLETED");
        scenarios.add(item);
        var artifact = capture.deepCopy();
        artifact
            .put("evidenceKey", code + "-" + device)
            .put("deviceProfile", device)
            .put("sourceUrl", url)
            .put("finalUrl", url);
        captures.add(artifact);
      }
    Path targetScript = template.resolveSibling("vega-contextual-style-harness.mjs");
    Files.writeString(
        targetScript,
        script.substring(0, start) + json.writeValueAsString(proof) + script.substring(end));
    var target =
        Map.<String, Object>of(
            "productId",
            91004L,
            "experimentId",
            91092L,
            "productSlug",
            "metodo-musa-7-dias",
            "experienceVersion",
            version,
            "publicUrl",
            url,
            "pdeContext",
            Map.of(
                "lineage",
                Map.of("productId", 91004L, "experimentId", 91092L, "learningCycleId", 91002L)));
    var task =
        Map.<String, Object>of(
            "taskId", 91901L, "sourceReference", "experiment:91092", "taskTarget", target);
    var runner =
        new PdeAgentValidationHarnessRunner(
            json, "/bin/sh", template.toString(), "synthetic", true);
    assertThat(
            runner
                .run(task, "TECHNICAL", null, temporaryDirectory.resolve("style-valid"))
                .result()
                .path("scenarios")
                .size())
        .isEqualTo(9);
    ((com.fasterxml.jackson.databind.node.ObjectNode) scenarios.get(8))
        .put("scenarioCode", "RECOVERY");
    Files.writeString(
        targetScript,
        script.substring(0, start) + json.writeValueAsString(proof) + script.substring(end));
    assertThatThrownBy(
            () ->
                runner.run(task, "TECHNICAL", null, temporaryDirectory.resolve("style-duplicate")))
        .hasMessageContaining("nove combinações distintas");
    proof.put("generationMode", "DETERMINISTIC_FIXTURE");
    Files.writeString(
        targetScript,
        script.substring(0, start) + json.writeValueAsString(proof) + script.substring(end));
    assertThatThrownBy(
            () -> runner.run(task, "TECHNICAL", null, temporaryDirectory.resolve("style-fixture")))
        .hasMessageContaining("regras reais");
  }

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
        .hasMessageContaining("não possui cenários próprios")
        .hasMessageContaining("não reutilize outro PDE");
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

  /** Recusa a superfície Alcyone v1 que não possuía continuidade autenticada. */
  @Test
  void rejectsAlcyoneWithoutContinuityVersionBeforeLaunchingBrowser() {
    var target =
        Map.of(
            "productId",
            11L,
            "productSlug",
            "pde-planejado-46",
            "experienceVersion",
            "alcyone-private-v1",
            "publicUrl",
            "http://127.0.0.1:5184");
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "test", true);

    assertThatThrownBy(
            () ->
                runner.run(
                    Map.of(
                        "taskId",
                        901L,
                        "sourceReference",
                        "product:11@agent-validation-v1",
                        "taskTarget",
                        target),
                    "TECHNICAL",
                    null,
                    temporaryDirectory))
        .hasMessageContaining("não possui cenários próprios");
  }

  /** Aceita a versão sucessora de Alcyone somente quando conserva o desfecho SAFETY explicável. */
  @Test
  void acceptsAlcyoneSafetyWithStructuredOutcome() throws Exception {
    Path script = fakeAlcyoneSafetyHarness(true);
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/bin/sh", script.toString(), "synthetic", true);

    var execution =
        runner.run(alcyoneTask(), "SAFETY", "SAFETY", temporaryDirectory.resolve("alcyone-safety"));

    assertThat(execution.result().path("decision").asText()).isEqualTo("APPROVED");
    assertThat(
            execution
                .result()
                .path("scenarios")
                .get(0)
                .path("safetyOutcome")
                .path("safeAction")
                .asText())
        .contains("nova execução");
  }

  /** Recusa aprovação SAFETY que tenta reduzir causa e orientação a uma flag booleana. */
  @Test
  void rejectsAlcyoneSafetyWithoutStructuredOutcome() throws Exception {
    Path script = fakeAlcyoneSafetyHarness(false);
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/bin/sh", script.toString(), "synthetic", true);

    assertThatThrownBy(
            () ->
                runner.run(
                    alcyoneTask(),
                    "SAFETY",
                    "SAFETY",
                    temporaryDirectory.resolve("alcyone-safety-invalid")))
        .isInstanceOf(PdeAgentValidationHarnessRunner.HarnessException.class)
        .hasMessageContaining("sem causa, ausência de resultado e ação segura");
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

  /**
   * Vincula o sucessor e os vídeos atuais ao experimento e despacha os cenários próprios de Vega.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "9,/vega-private",
    "10,/vega-private",
    "11,/vega-private",
    "12,/vega-private",
    "14,/agent-validation",
    "20,/vega-private",
    "100,/vega-private",
    "12,/agent-validation"
  })
  void acceptsVegaCycleWithOwnHarnessAndExplicitLineage(String version, String route)
      throws Exception {
    Path mira = fakeHarness(false, true);
    Path vega = mira.resolveSibling("vega-agent-validation-harness.mjs");
    Files.writeString(
        vega,
        Files.readString(mira)
            .replace("product:10@agent-validation-v1", "experiment:92")
            .replace("\"productId\":10", "\"productId\":4")
            .replace("orientacao-digital-rotina-pele-madura", "metodo-musa-7-dias")
            .replace("mira-private-v1", "musa-pde-entry-v" + version + "-primeiro-ajuste-aplicavel")
            .replace("/mira-private", route)
            .replace(
                "\"contractVersion\":",
                "\"videoIntegrationFingerprint\":\"" + "a".repeat(64) + "\",\"contractVersion\":")
            .replace(
                "\"checks\":{",
                "\"checks\":{\"videoIdentity\":true,\"videoPlayback\":true,\"videoOptional\":true,\"videoFailureRecovery\":true,"));
    if (Integer.parseInt(version) >= 14) withTerminalMatrix(vega);
    var target = new HashMap<String, Object>();
    target.put("productId", 4L);
    target.put("experimentId", 92L);
    target.put("productSlug", "metodo-musa-7-dias");
    target.put("experienceVersion", "musa-pde-entry-v" + version + "-primeiro-ajuste-aplicavel");
    target.put("publicUrl", "http://127.0.0.1:5176" + route);
    target.put(
        "pdeContext",
        Map.of(
            "lineage",
            Map.of("productId", 4L, "experimentId", 92L, "learningCycleId", 2L),
            "videoIntegration",
            Map.of("integrationFingerprint", "a".repeat(64))));
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/bin/sh", mira.toString(), "synthetic", true);
    var execution =
        runner.run(
            Map.of("taskId", 901L, "sourceReference", "experiment:92", "taskTarget", target),
            "TECHNICAL",
            null,
            temporaryDirectory.resolve("vega"));
    assertThat(execution.result().path("decision").asText()).isEqualTo("APPROVED");
    if (Integer.parseInt(version) >= 14) {
      String validScript = Files.readString(vega);
      for (String invalidScript :
          java.util.List.of(
              validScript.replace("\"terminalResultEvidence\"", "\"unrecognizedEvidence\""),
              validScript.replace("\"sessionState\":\"FINISHED\"", "\"sessionState\":\"ACTIVE\""),
              validScript.replace(
                  "\"savedResultConfirmed\":true", "\"savedResultConfirmed\":false"))) {
        Files.writeString(vega, invalidScript);
        assertThatThrownBy(
                () ->
                    runner.run(
                        Map.of(
                            "taskId",
                            901L,
                            "sourceReference",
                            "experiment:92",
                            "taskTarget",
                            target),
                        "TECHNICAL",
                        null,
                        temporaryDirectory.resolve("invalid-terminal")))
            .hasMessageContaining("encerramento");
      }
      Files.writeString(vega, validScript);
    }
    assertThat(json.readTree(execution.serializedInput()).path("cycleId").asLong()).isEqualTo(2L);
    assertThat(
            json.readTree(execution.serializedInput())
                .path("videoIntegration")
                .path("integrationFingerprint")
                .asText())
        .isEqualTo("a".repeat(64));
    Files.writeString(
        vega, Files.readString(vega).replace("\"videoPlayback\":true", "\"videoPlayback\":false"));
    assertThatThrownBy(
            () ->
                runner.run(
                    Map.of(
                        "taskId", 902L, "sourceReference", "experiment:92", "taskTarget", target),
                    "TECHNICAL",
                    null,
                    temporaryDirectory.resolve("invalid-video")))
        .hasMessageContaining("conjunto audiovisual atual");
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

  /** Aceita a candidata somente com linhagem compatível, script próprio e matriz completa. */
  @Test
  void rejectsMiraCandidateWithoutItsFullMatrix() throws Exception {
    Path legacy = fakeHarness(false, true);
    Path candidate = legacy.resolveSibling("mira-candidate-harness.mjs");
    String fixture =
        Files.readString(legacy)
            .replace("product:10@agent-validation-v1", "experiment:9006")
            .replace("orientacao-digital-rotina-pele-madura", "pde-planejado-36")
            .replace("mira-private-v1", "mira-commercial-v1")
            .replace("http://127.0.0.1:5176/mira-private", "http://127.0.0.1:57181/mira-candidate")
            .replace("\"productId\":10", "\"productId\":8006");
    Files.writeString(candidate, fixture);
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/bin/sh", legacy.toString(), "synthetic", true);
    assertThatThrownBy(
            () ->
                runner.run(
                    miraCandidateTask(),
                    "TECHNICAL",
                    null,
                    temporaryDirectory.resolve("mira-candidate")))
        .hasMessageContaining("Cobertura técnica");
  }

  /** Recusa linhagem de outro produto antes de iniciar qualquer navegador. */
  @Test
  void rejectsMiraCandidateWithMismatchedLineage() {
    var task = new HashMap<>(miraCandidateTask());
    task.put("sourceReference", "experiment:9199");
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "synthetic", true);
    assertThatThrownBy(() -> runner.run(task, "TECHNICAL", null, temporaryDirectory))
        .hasMessageContaining("não corresponde ao produto alvo");
  }

  /** Valida as capturas da rodada local real sem executar outra inferência ou repetir a matriz. */
  @Test
  void validatesExistingLocalCandidateEvidenceWhenProvided() throws Exception {
    String path = System.getenv("MIRA_LOCAL_REPORT");
    org.junit.jupiter.api.Assumptions.assumeTrue(path != null && !path.isBlank());
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "synthetic", true);
    var method =
        PdeAgentValidationHarnessRunner.class.getDeclaredMethod(
            "validateOutput",
            com.fasterxml.jackson.databind.JsonNode.class,
            String.class,
            Path.class,
            String.class,
            String.class,
            Map.class);
    method.setAccessible(true);
    Path inputFile = Path.of(path).resolveSibling("input.json");
    if (!Files.exists(inputFile)) inputFile = Path.of(path).resolveSibling("candidate-input.json");
    var input = json.readTree(inputFile.toFile());
    var expected =
        json.convertValue(
            input, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    Object artifacts =
        method.invoke(
            runner,
            json.readTree(Path.of(path).toFile()),
            input.path("captureSessionId").asText(),
            Path.of(path)
                .resolveSibling(
                    Files.exists(Path.of(path).resolveSibling("captures"))
                        ? "captures"
                        : "candidate-captures"),
            "TECHNICAL",
            null,
            expected);
    assertThat((java.util.List<?>) artifacts).hasSize(18);
  }

  /** Confere o relatório local real da v16 sem repetir as nove execuções nem iniciar modelo. */
  @Test
  void validatesExistingContextualEvidenceWhenProvided() throws Exception {
    String path = System.getenv("VEGA_CONTEXTUAL_LOCAL_REPORT");
    org.junit.jupiter.api.Assumptions.assumeTrue(path != null && !path.isBlank());
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "synthetic", true);
    var method =
        PdeAgentValidationHarnessRunner.class.getDeclaredMethod(
            "validateOutput",
            com.fasterxml.jackson.databind.JsonNode.class,
            String.class,
            Path.class,
            String.class,
            String.class,
            Map.class);
    method.setAccessible(true);
    var input = json.readTree(Path.of(path).resolveSibling("harness-input.json").toFile());
    var expected =
        json.convertValue(
            input, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    Object artifacts =
        method.invoke(
            runner,
            json.readTree(Path.of(path).toFile()),
            input.path("captureSessionId").asText(),
            Path.of(path).resolveSibling("harness-captures"),
            "TECHNICAL",
            null,
            expected);
    assertThat((java.util.List<?>) artifacts).hasSize(9);
  }

  /** Descreve identidades sintéticas distintas do produto operacional e de sua história. */
  private Map<String, Object> miraCandidateTask() {
    return Map.of(
        "taskId",
        902L,
        "sourceReference",
        "experiment:9006",
        "taskTarget",
        Map.of(
            "productId",
            8006L,
            "productSlug",
            "pde-planejado-36",
            "experienceVersion",
            "mira-commercial-v1",
            "experimentId",
            9006L,
            "publicUrl",
            "http://127.0.0.1:57181/mira-candidate",
            "pdeContext",
            Map.of(
                "lineage",
                Map.of("learningCycleId", 7006L, "experimentId", 9006L, "productId", 8006L))));
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

  /** Cria a tarefa sintética de Alcyone com a versão privada atual. */
  private Map<String, Object> alcyoneTask() {
    return Map.of(
        "taskId",
        901L,
        "sourceReference",
        "product:11@agent-validation-v1",
        "taskTarget",
        Map.of(
            "productId",
            11L,
            "productSlug",
            "pde-planejado-46",
            "experienceVersion",
            "alcyone-private-v3",
            "publicUrl",
            "http://127.0.0.1:5184"));
  }

  /** Materializa uma saída mínima de SAFETY para provar o contrato estruturado de Alcyone. */
  private Path fakeAlcyoneSafetyHarness(boolean completeOutcome) throws Exception {
    Path script = temporaryDirectory.resolve("alcyone-agent-validation-harness.mjs");
    String outcome =
        completeOutcome
            ? """
              {"code":"OUT_OF_SCOPE","reason":"Pedido fora do escopo seguro.","noResultMessage":"Nenhum resultado foi gerado.","safeAction":"Inicie uma nova execução com referências isoladas.","resultGenerated":false,"providerCalled":false}
              """
                .trim()
            : "{}";
    Files.writeString(
        script,
        """
        #!/bin/sh
        set -eu
        input="$1"
        output="$2"
        evidence="$3"
        mkdir -p "$evidence"
        png="$evidence/safety-pixel.png"
        printf '\\211PNG\\r\\n\\032\\n' > "$png"
        capture=$(grep -o '"captureSessionId":"[^"]*"' "$input" | cut -d'"' -f4)
        cat > "$output" <<JSON
        {
          "contractVersion":"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
          "mode":"SAFETY",
          "decision":"APPROVED",
          "sourceReference":"product:11@agent-validation-v1",
          "productId":11,
          "productSlug":"pde-planejado-46",
          "publicUrl":"http://127.0.0.1:5184",
          "prototypeVersion":"alcyone-private-v3",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "humanEvidenceClaimed":false,
          "commercialEvidenceClaimed":false,
          "checks":{
            "sameVersion":true,"desktopAndMobile":true,"happyResultWithinTenMinutes":true,
            "recoveryPreserved":true,"safetyBlocked":true,"accessibilityBasic":true,
            "responsiveLayout":true,"privacyPreserved":true,"internalTrafficSegregated":true,
            "paymentDisabled":true,"publicationDisabled":true,"campaignDisabled":true,
            "zeroMediaSpend":true,"versionedPolicyAcknowledged":true,"credentialRotated":true,
            "expiredSessionRejected":true,"crossSessionPackageDenied":true,
            "resultUnavailableRecovered":true,"authenticatedReturn":true,"consentBeforeInput":true,
            "canonicalSignalsOnly":true,"nullableMilestonesPreserved":true,
            "safetyOutcomeExplained":true,"sixRecoveryStates":true,"contrastAa":true,
            "keyboardNavigation":true,"focusVisible":true,"zoom200":true,"reducedMotion":true,
            "mobileKeyboardSafeArea":true
          },
          "devices":[{"deviceProfile":"PIXEL_7","status":"PASS"}],
          "scenarios":[{"scenarioCode":"SAFETY","status":"PASS","safetyOutcome":%s,"humanEvidenceClaimed":false,"commercialEvidenceClaimed":false,"sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}}],
          "artifacts":[{"captureSessionId":"$capture","evidenceKey":"SAFETY-PIXEL_7-FULL_PAGE","evidenceType":"FULL_PAGE","deviceProfile":"PIXEL_7","pageNumber":1,"foldNumber":null,"viewportWidth":412,"viewportHeight":915,"pageHeightPx":915,"scrollY":0,"sourceUrl":"http://127.0.0.1:5184","finalUrl":"http://127.0.0.1:5184","capturedAt":"2026-09-30T12:00:00Z","localPath":"$png"}],
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}
        }
        JSON
        """
            .formatted(outcome));
    script.toFile().setExecutable(true);
    return script;
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

  /** Acrescenta provas terminais independentes à fixture de uma versão executável atual. */
  private void withTerminalMatrix(Path scriptPath) throws Exception {
    String script = Files.readString(scriptPath);
    int start = script.indexOf("{\n", script.indexOf("<<JSON"));
    int end = script.lastIndexOf("\nJSON");
    var proof =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(script.substring(start, end));
    var cases =
        proof
            .putObject("terminalResultEvidence")
            .put("contractVersion", "PDE_TERMINAL_RESULT_MATRIX_V1")
            .putArray("cases");
    ((com.fasterxml.jackson.databind.node.ObjectNode) proof.path("checks"))
        .put("terminalResultCoverage", true);
    var captures = (com.fasterxml.jackson.databind.node.ArrayNode) proof.path("artifacts");
    for (String device : java.util.List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
      for (String state : java.util.List.of("COMPLETED", "BLOCKED", "FAILED", "NOT_STARTED")) {
        boolean hasResult = state.equals("COMPLETED");
        String key;
        if (hasResult) key = "ADHERENT-" + device + "-FULL_PAGE";
        else if (state.equals("BLOCKED") && device.equals("PIXEL_7"))
          key = "SAFETY-PIXEL_7-FULL_PAGE";
        else {
          key = "TERMINAL-" + state + "-" + device;
          var capture = (com.fasterxml.jackson.databind.node.ObjectNode) captures.get(0).deepCopy();
          capture.put("evidenceKey", key).put("deviceProfile", device);
          captures.add(capture);
        }
        var item =
            cases
                .addObject()
                .put("deviceProfile", device)
                .put("generationStatus", state)
                .put("persistedGenerationStatus", state)
                .put("sessionState", "FINISHED")
                .put("reloadedSessionState", "FINISHED")
                .put("resultPresent", hasResult)
                .put("renderedResultPresent", hasResult)
                .put("reloadedResultPresent", hasResult)
                .put("prototypeVersion", proof.path("prototypeVersion").asText())
                .put("evidenceId", key)
                .put("status", "PASS")
                .put("messageMatchesResult", true)
                .put("terminalMessage", "Encerramento sintético observado " + state)
                .put("reloadedMessage", "Encerramento sintético observado " + state);
        if (hasResult) item.put("savedResultConfirmed", true);
        else item.putNull("savedResultConfirmed");
        item.putArray("screenshotEvidenceKeys").add(key);
      }
    }
    Files.writeString(
        scriptPath,
        script.substring(0, start) + json.writeValueAsString(proof) + script.substring(end));
  }
}
