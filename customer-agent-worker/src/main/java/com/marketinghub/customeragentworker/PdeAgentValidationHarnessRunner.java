package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar e validar o harness determinístico nos protótipos suportados. */
@Component
public class PdeAgentValidationHarnessRunner {
  private static final byte[] PNG_SIGNATURE =
      new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
  private static final List<String> REQUIRED_CHECKS =
      List.of(
          "sameVersion",
          "desktopAndMobile",
          "happyResultWithinTenMinutes",
          "recoveryPreserved",
          "safetyBlocked",
          "accessibilityBasic",
          "responsiveLayout",
          "privacyPreserved",
          "internalTrafficSegregated",
          "paymentDisabled",
          "publicationDisabled",
          "campaignDisabled",
          "zeroMediaSpend");
  private static final List<String> ALCYONE_CONTINUITY_CHECKS =
      List.of(
          "versionedPolicyAcknowledged",
          "credentialRotated",
          "expiredSessionRejected",
          "crossSessionPackageDenied",
          "resultUnavailableRecovered",
          "authenticatedReturn",
          "consentBeforeInput",
          "canonicalSignalsOnly",
          "nullableMilestonesPreserved",
          "safetyOutcomeExplained",
          "sixRecoveryStates",
          "contrastAa",
          "keyboardNavigation",
          "focusVisible",
          "zoom200",
          "reducedMotion",
          "mobileKeyboardSafeArea");
  private final ObjectMapper json;
  private final String nodeBinary;
  private final String scriptPath;
  private final String internalToken;
  private final boolean allowLocalUrls;

  /** Configura o script versionado, a credencial protegida e a política de rede. */
  public PdeAgentValidationHarnessRunner(
      ObjectMapper json,
      @Value("${CUSTOMER_AGENT_NODE_BIN:node}") String nodeBinary,
      @Value(
              "${CUSTOMER_AGENT_PDE_AGENT_VALIDATION_SCRIPT:/app/browser/pde-agent-validation-harness.mjs}")
          String scriptPath,
      @Value("${PDE_INTERNAL_API_TOKEN:}") String internalToken,
      @Value("${CUSTOMER_AGENT_PDE_ALLOW_LOCAL_URLS:false}") boolean allowLocalUrls) {
    this.json = json;
    this.nodeBinary = nodeBinary;
    this.scriptPath = scriptPath;
    this.internalToken = internalToken == null ? "" : internalToken.trim();
    this.allowLocalUrls = allowLocalUrls;
  }

  /** Valida a família executável e a identidade versionada antes de executar cenários isolados. */
  HarnessExecution run(
      Map<String, Object> task, String mode, String scenarioCode, Path workDirectory)
      throws Exception {
    if (internalToken.isBlank()) {
      throw HarnessException.executor(
          "PDE_INTERNAL_API_TOKEN não está configurado no worker de Psique.");
    }
    JsonNode target = json.valueToTree(task.get("taskTarget"));
    String sourceUrl = target.path("publicUrl").asText("").trim();
    if (sourceUrl.isBlank()) {
      throw new HarnessException(
          "O protótipo desta passagem não possui URL executável. Conclua a implementação "
              + "e registre a aceitação privada antes de repetir a homologação.");
    }
    validateUrl(sourceUrl);
    String sourceReference = String.valueOf(task.get("sourceReference"));
    if (!sourceReference.matches(
        "product:[1-9][0-9]*@agent-validation-v1|experiment:[1-9][0-9]*")) {
      throw new HarnessException("A tarefa não pertence à referência multiagente canônica.");
    }
    long productId = target.path("productId").asLong();
    String productSlug = target.path("productSlug").asText("").trim();
    String prototypeVersion = target.path("experienceVersion").asText("").trim();
    if (productId < 1 || productSlug.isBlank() || prototypeVersion.isBlank()) {
      throw new HarnessException("O alvo da tarefa multiagente está incompleto.");
    }
    JsonNode lineage = target.path("pdeContext").path("lineage");
    JsonNode acceptance = target.path("pdeContext").path("privatePrototypeAcceptance");
    boolean privateKit =
        "DETERMINISTIC_PRIVATE_KIT_V1".equals(acceptance.path("runtimeKind").asText())
            && List.of("nails-v1", "barber-v1").contains(acceptance.path("profileCode").asText())
            && prototypeVersion.equals(acceptance.path("prototypeVersion").asText())
            && List.of("/mh-api/pde/kit/private/v1/prototype", "/api/pde/kit/private/v1/prototype")
                .contains(URI.create(sourceUrl).getPath())
            && sourceReference.equals("experiment:" + target.path("experimentId").asLong())
            && lineage.path("learningCycleId").asLong() > 0
            && lineage.path("experimentId").asLong() == target.path("experimentId").asLong()
            && lineage.path("productId").asLong() == productId;
    boolean vega =
        "metodo-musa-7-dias".equals(productSlug)
            && prototypeVersion.matches(
                "musa-pde-entry-v(?:9|[1-9][0-9]+)-primeiro-ajuste-aplicavel")
            && List.of("/vega-private", "/agent-validation")
                .contains(URI.create(sourceUrl).getPath())
            && sourceReference.equals("experiment:" + target.path("experimentId").asLong())
            && lineage.path("learningCycleId").asLong() > 0
            && lineage.path("experimentId").asLong() == target.path("experimentId").asLong()
            && lineage.path("productId").asLong() == productId;
    boolean mira =
        "orientacao-digital-rotina-pele-madura".equals(productSlug)
            && prototypeVersion.startsWith("mira-private-v")
            && "/mira-private".equals(URI.create(sourceUrl).getPath());
    boolean miraCandidate =
        "pde-planejado-36".equals(productSlug)
            && miraCandidateVersion(prototypeVersion)
            && "/mira-candidate".equals(URI.create(sourceUrl).getPath())
            && sourceReference.equals("experiment:" + target.path("experimentId").asLong())
            && lineage.path("learningCycleId").asLong() > 0
            && lineage.path("experimentId").asLong() == target.path("experimentId").asLong()
            && lineage.path("productId").asLong() == productId;
    boolean alcyone =
        "pde-planejado-46".equals(productSlug)
            && prototypeVersion.matches("alcyone-private-v(?:[2-9]|[1-9][0-9]+)")
            && List.of("", "/").contains(URI.create(sourceUrl).getPath());
    if (!("product:" + productId + "@agent-validation-v1").equals(sourceReference)
        && !vega
        && !miraCandidate
        && !privateKit) {
      throw new HarnessException("A referência da homologação não corresponde ao produto alvo.");
    }
    if (!vega && !mira && !alcyone && !miraCandidate && !privateKit) {
      throw HarnessException.executor(
          "O harness instalado não possui cenários próprios para este produto. "
              + "Implemente-os antes da homologação; não reutilize outro PDE.");
    }
    Files.createDirectories(workDirectory);
    Path inputPath = workDirectory.resolve("agent-validation-input.json");
    Path outputPath = workDirectory.resolve("agent-validation-output.json");
    Path evidenceDirectory = workDirectory.resolve("agent-validation-evidence");
    String captureSessionId = UUID.randomUUID().toString();
    Map<String, Object> input =
        new java.util.LinkedHashMap<>(
            Map.of(
                "mode",
                mode,
                "scenarioCode",
                scenarioCode == null ? "" : scenarioCode,
                "captureSessionId",
                captureSessionId,
                "sourceUrl",
                sourceUrl,
                "sourceReference",
                sourceReference,
                "productId",
                productId,
                "productSlug",
                productSlug,
                "prototypeVersion",
                prototypeVersion));
    if (vega || miraCandidate || privateKit) {
      input.put("cycleId", lineage.path("learningCycleId").asLong());
      if (privateKit) {
        input.put("runtimeKind", "DETERMINISTIC_PRIVATE_KIT_V1");
        input.put("profileCode", acceptance.path("profileCode").asText());
      }
      var videoIntegration = target.path("pdeContext").path("videoIntegration");
      if (videoIntegration.isObject()) input.put("videoIntegration", videoIntegration);
    }
    String executionScript;
    if (privateKit) {
      executionScript = Path.of(scriptPath).resolveSibling("private-kit-harness.mjs").toString();
    } else if (miraCandidate) {
      executionScript = Path.of(scriptPath).resolveSibling("mira-candidate-harness.mjs").toString();
    } else if (vega) {
      executionScript =
          Path.of(scriptPath).resolveSibling("vega-agent-validation-harness.mjs").toString();
    } else if (alcyone) {
      executionScript =
          Path.of(scriptPath).resolveSibling("alcyone-agent-validation-harness.mjs").toString();
    } else {
      executionScript = scriptPath;
    }
    String serializedInput = json.writeValueAsString(input);
    Files.writeString(inputPath, serializedInput, StandardCharsets.UTF_8);
    ProcessBuilder builder =
        new ProcessBuilder(
                nodeBinary,
                executionScript,
                inputPath.toString(),
                outputPath.toString(),
                evidenceDirectory.toString())
            .redirectErrorStream(true)
            .redirectOutput(workDirectory.resolve("agent-validation-browser.log").toFile());
    builder.environment().put("PDE_INTERNAL_API_TOKEN", internalToken);
    Process process = builder.start();
    if (!process.waitFor(10, TimeUnit.MINUTES)) {
      process.destroyForcibly();
      throw new HarnessException("Timeout ao homologar o PDE nos dispositivos suportados.");
    }
    if (process.exitValue() != 0 || !Files.isRegularFile(outputPath)) {
      throw HarnessException.executor(
          "Falha no harness multiagente: "
              + Files.readString(
                  workDirectory.resolve("agent-validation-browser.log"), StandardCharsets.UTF_8));
    }
    JsonNode result = json.readTree(outputPath.toFile());
    List<BpmVisualEvidenceRunner.VisualArtifact> artifacts =
        validateOutput(result, captureSessionId, evidenceDirectory, mode, scenarioCode, input);
    BpmVisualEvidenceRunner.CaptureOutput capture =
        new BpmVisualEvidenceRunner.CaptureOutput(
            captureSessionId, "MULTI_DEVICE", List.of(), artifacts);
    return new HarnessExecution(
        result,
        new BpmVisualEvidenceRunner.VisualEvidenceBundle(capture, workDirectory),
        serializedInput,
        sourceUrl);
  }

  /**
   * Exige contrato, formato do pacote, mídias integradas, condições de entrada e capturas
   * vinculadas aos cenários antes do parecer.
   */
  private List<BpmVisualEvidenceRunner.VisualArtifact> validateOutput(
      JsonNode result,
      String captureSessionId,
      Path evidenceDirectory,
      String mode,
      String scenarioCode,
      Map<String, Object> expected)
      throws Exception {
    if (!"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(result.path("contractVersion").asText())
        || !mode.equals(result.path("mode").asText())
        || !List.of("APPROVED", "BLOCKED").contains(result.path("decision").asText())
        || !String.valueOf(expected.get("sourceReference"))
            .equals(result.path("sourceReference").asText())
        || ((Number) expected.get("productId")).longValue() != result.path("productId").asLong()
        || !String.valueOf(expected.get("productSlug")).equals(result.path("productSlug").asText())
        || !String.valueOf(expected.get("sourceUrl")).equals(result.path("publicUrl").asText())
        || !String.valueOf(expected.get("prototypeVersion"))
            .equals(result.path("prototypeVersion").asText())
        || !"AGENT_VALIDATION".equals(result.path("trafficClass").asText())
        || !"mh_internal_test".equals(result.path("internalMarker").asText())
        || result.path("humanEvidenceClaimed").asBoolean(true)
        || result.path("commercialEvidenceClaimed").asBoolean(true)) {
      throw new HarnessException("Contrato funcional do harness multiagente foi reprovado.");
    }
    if (expected.containsKey("videoIntegration")) {
      JsonNode binding = json.valueToTree(expected.get("videoIntegration"));
      if (!binding.path("integrationFingerprint").equals(result.path("videoIntegrationFingerprint"))
          || java.util.List.of(
                  "videoIdentity", "videoPlayback", "videoOptional", "videoFailureRecovery")
              .stream()
              .anyMatch(key -> !result.path("checks").path(key).asBoolean(false)))
        throw new HarnessException("A homologação não comprovou o conjunto audiovisual atual.");
    }
    JsonNode checks = result.path("checks");
    if (!checks.isObject()
        || REQUIRED_CHECKS.stream().anyMatch(check -> !checks.path(check).isBoolean())) {
      throw new HarnessException("O harness não informou todos os gates determinísticos.");
    }
    boolean approved = "APPROVED".equals(result.path("decision").asText());
    if (approved
        && REQUIRED_CHECKS.stream().anyMatch(check -> !checks.path(check).asBoolean(false))) {
      throw new HarnessException("O harness aprovou a execução com gate reprovado.");
    }
    boolean privateKit = "DETERMINISTIC_PRIVATE_KIT_V1".equals(expected.get("runtimeKind"));
    if (privateKit) {
      if (!"PDE_PRIVATE_KIT_PACKAGE_V2".equals(result.path("packageContractVersion").asText())
          || !"PDE_PRIVATE_KIT_FIXTURES_V1".equals(result.path("fixtureContract").asText()))
        throw new HarnessException(
            "A prova do kit não corresponde ao pacote corrigido de 36 arquivos.");
      for (JsonNode scenario : result.path("scenarios")) {
        int requiredFiles = "SAFETY".equals(scenario.path("scenarioCode").asText()) ? 0 : 36;
        if (scenario.path("packageFileCount").asInt(-1) != requiredFiles
            || !scenario.path("screenshotEvidenceKeys").isArray()
            || scenario.path("screenshotEvidenceKeys").isEmpty())
          throw new HarnessException(
              "O cenário do kit não comprovou arquivos e vínculo de captura antes do parecer.");
      }
    }
    boolean miraCandidate =
        "pde-planejado-36".equals(String.valueOf(expected.get("productSlug")))
            && miraCandidateVersion(String.valueOf(expected.get("prototypeVersion")));
    boolean alcyone = "pde-planejado-46".equals(String.valueOf(expected.get("productSlug")));
    if (alcyone
        && (ALCYONE_CONTINUITY_CHECKS.stream().anyMatch(check -> !checks.path(check).isBoolean())
            || (approved
                && ALCYONE_CONTINUITY_CHECKS.stream()
                    .anyMatch(check -> !checks.path(check).asBoolean(false))))) {
      throw new HarnessException(
          "A homologação Alcyone não comprovou todos os gates de continuidade autenticada.");
    }
    if (alcyone && approved) {
      for (JsonNode scenario : result.path("scenarios")) {
        if (!"SAFETY".equals(scenario.path("scenarioCode").asText())) continue;
        JsonNode outcome = scenario.path("safetyOutcome");
        if (!"OUT_OF_SCOPE".equals(outcome.path("code").asText())
            || outcome.path("reason").asText().isBlank()
            || outcome.path("noResultMessage").asText().isBlank()
            || outcome.path("safeAction").asText().isBlank()
            || outcome.path("resultGenerated").asBoolean(true)
            || outcome.path("providerCalled").asBoolean(true)) {
          throw new HarnessException(
              "A homologação Alcyone aprovou SAFETY sem causa, ausência de resultado e ação"
                  + " segura.");
        }
      }
    }
    requireNoExternalSideEffects(result.path("sideEffects"));
    for (JsonNode scenario : result.path("scenarios")) {
      if (scenario.path("humanEvidenceClaimed").asBoolean(true)
          || scenario.path("commercialEvidenceClaimed").asBoolean(true)) {
        throw new HarnessException("Um cenário tentou declarar evidência humana ou comercial.");
      }
      requireNoExternalSideEffects(scenario.path("sideEffects"));
    }
    if ("TECHNICAL".equals(mode)) {
      Set<String> devices = textSet(result.path("devices"), "deviceProfile", null);
      Set<String> scenarios = textSet(result.path("scenarios"), "scenarioCode", null);
      int expectedScenarioDeviceGates = miraCandidate ? 18 : (alcyone || privateKit ? 9 : 5);
      if (result.path("devices").size() != 3
          || result.path("scenarios").size() != expectedScenarioDeviceGates
          || result.path("artifacts").size() != expectedScenarioDeviceGates
          || !devices.equals(Set.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"))
          || !scenarios.equals(Set.of("ADHERENT", "RECOVERY", "SAFETY"))) {
        throw new HarnessException(
            "Cobertura técnica de dispositivos ou cenários está incompleta.");
      }
      if (approved
          && (result.path("devices").findValues("status").stream()
                  .anyMatch(status -> !"PASS".equals(status.asText()))
              || result.path("scenarios").findValues("status").stream()
                  .anyMatch(status -> !"PASS".equals(status.asText())))) {
        throw new HarnessException("O harness aprovou uma cobertura com percurso reprovado.");
      }
      if (privateKit) {
        var combinations = new java.util.HashSet<String>();
        for (String scenario : List.of("ADHERENT", "RECOVERY", "SAFETY"))
          for (String device : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"))
            combinations.add(scenario + "|" + device);
        var observed = new java.util.HashSet<String>();
        result
            .path("scenarios")
            .forEach(
                s ->
                    observed.add(
                        s.path("scenarioCode").asText() + "|" + s.path("deviceProfile").asText()));
        if (!combinations.equals(observed)
            || result.path("providerCalls").asInt(-1) != 0
            || !"PDE_PRIVATE_KIT_FIXTURES_V1".equals(result.path("fixtureContract").asText())
            || !String.valueOf(expected.get("profileCode"))
                .equals(result.path("profileCode").asText()))
          throw new HarnessException(
              "O kit não comprovou as nove combinações, perfil registrado e ausência de integração"
                  + " paga.");
      }
      if (miraCandidate) {
        var expectedCases = new java.util.HashSet<String>();
        for (String scenario : List.of("ADHERENT", "RECOVERY", "SAFETY"))
          for (String device : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"))
            for (String condition : List.of("REFERENCE", "REDUCED"))
              expectedCases.add(scenario + "|" + device + "|" + condition);
        var observedCases = new java.util.HashSet<String>();
        result
            .path("scenarios")
            .forEach(
                scenario ->
                    observedCases.add(
                        scenario.path("scenarioCode").asText()
                            + "|"
                            + scenario.path("deviceProfile").asText()
                            + "|"
                            + scenario.path("condition").asText()));
        if (!expectedCases.equals(observedCases)
            || !supportedFixtureContract(
                String.valueOf(expected.get("prototypeVersion")),
                result.path("fixtureContract").asText())
            || !"DETERMINISTIC_DOCUMENTED_LABELS".equals(result.path("generationMode").asText())
            || result.path("providerCalls").asInt(-1) != 0) {
          throw new HarnessException(
              "Mira não comprovou as dezoito combinações segregadas sem provedor pago.");
        }
      }
      if (alcyone
          && (!"PDE_STATIC_RESULT_FIXTURES_V1".equals(result.path("fixtureContract").asText())
              || !result.path("checks").path("staticFixturesValid").asBoolean(false)
              || !result.path("checks").path("providerCallsZero").asBoolean(false)
              || !result.path("checks").path("nineScenarioDeviceGates").asBoolean(false))) {
        throw new HarnessException(
            "Alcyone não comprovou fixtures estáticas, custo zero e os nove gates.");
      }
    } else if (result.path("scenarios").size() != 1
        || result.path("artifacts").size() != 1
        || !scenarioCode.equals(result.path("scenarios").get(0).path("scenarioCode").asText())) {
      throw new HarnessException("A execução de Psique misturou cenários sintéticos.");
    } else if (approved
        && !"PASS".equals(result.path("scenarios").get(0).path("status").asText())) {
      throw new HarnessException("O harness aprovou um cenário sintético reprovado.");
    }
    Path realEvidenceDirectory = evidenceDirectory.toRealPath();
    List<BpmVisualEvidenceRunner.VisualArtifact> artifacts = new ArrayList<>();
    for (JsonNode artifact : result.path("artifacts")) {
      Path file = Path.of(artifact.path("localPath").asText()).toAbsolutePath().normalize();
      if (!"FULL_PAGE".equals(artifact.path("evidenceType").asText())
          || !artifact.path("foldNumber").isNull()
          || !captureSessionId.equals(artifact.path("captureSessionId").asText())
          || !file.startsWith(evidenceDirectory.toAbsolutePath().normalize())
          || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
          || !file.toRealPath().startsWith(realEvidenceDirectory)
          || !pngSignature(file)) {
        throw new HarnessException("Screenshot do harness está ausente ou fora da execução.");
      }
      artifacts.add(
          new BpmVisualEvidenceRunner.VisualArtifact(
              captureSessionId,
              artifact.path("evidenceKey").asText(),
              artifact.path("evidenceType").asText(),
              artifact.path("deviceProfile").asText(),
              artifact.path("pageNumber").asInt(),
              artifact.path("foldNumber").isNull() ? null : artifact.path("foldNumber").asInt(),
              artifact.path("viewportWidth").asInt(),
              artifact.path("viewportHeight").asInt(),
              artifact.path("pageHeightPx").asInt(),
              artifact.path("scrollY").asInt(),
              artifact.path("sourceUrl").asText(),
              artifact.path("finalUrl").asText(),
              Instant.parse(artifact.path("capturedAt").asText()),
              file.toString()));
    }
    if (artifacts.isEmpty()) throw new HarnessException("O harness não produziu screenshots.");
    return List.copyOf(artifacts);
  }

  /** Mantém relatórios históricos e exige o contrato de sinais das candidatas novas. */
  private boolean supportedFixtureContract(String version, String contract) {
    if ("mira-commercial-v1".equals(version))
      return Set.of(
              "PDE_DOCUMENTED_INPUT_COMPARISON_V1",
              "PDE_DOCUMENTED_INPUT_COMPARISON_V2",
              "PDE_DOCUMENTED_INPUT_COMPARISON_V3")
          .contains(contract);
    if ("mira-private-candidate-v2".equals(version))
      return Set.of("PDE_DOCUMENTED_INPUT_COMPARISON_V2", "PDE_DOCUMENTED_INPUT_COMPARISON_V3")
          .contains(contract);
    return "PDE_DOCUMENTED_INPUT_COMPARISON_V3".equals(contract);
  }

  /** Reconhece candidatas privadas versionadas e mantém a leitura da versão histórica. */
  private boolean miraCandidateVersion(String version) {
    return "mira-commercial-v1".equals(version)
        || version.matches("mira-private-candidate-v(?:[2-9]|[1-9][0-9]+)");
  }

  /** Exige que a homologação continue sem compra, publicação, campanha ou gasto. */
  private void requireNoExternalSideEffects(JsonNode sideEffects) {
    if (!sideEffects.isObject()
        || sideEffects.path("paymentEnabled").asBoolean(true)
        || sideEffects.path("published").asBoolean(true)
        || sideEffects.path("campaignCreated").asBoolean(true)
        || sideEffects.path("mediaSpendBrl").asInt(-1) != 0) {
      throw new HarnessException("A homologação declarou um efeito comercial externo.");
    }
  }

  /** Extrai identidades filtradas por status e impede duplicidade silenciosa no contrato. */
  private Set<String> textSet(JsonNode values, String field, String requiredStatus) {
    java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
    if (!values.isArray()) return Set.of();
    values.forEach(
        value -> {
          if (requiredStatus == null || requiredStatus.equals(value.path("status").asText())) {
            result.add(value.path(field).asText());
          }
        });
    return Set.copyOf(result);
  }

  /** Confirma a assinatura PNG antes de enviar um arquivo ao backend. */
  private boolean pngSignature(Path file) throws Exception {
    try (var input = Files.newInputStream(file)) {
      return Arrays.equals(PNG_SIGNATURE, input.readNBytes(PNG_SIGNATURE.length));
    }
  }

  /** Bloqueia credenciais, parâmetros, fragmentos e redes privadas fora da homologação local. */
  private void validateUrl(String value) throws Exception {
    URI uri = URI.create(value == null ? "" : value.trim());
    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null) {
      throw new HarnessException("A URL do PDE é inválida ou contém parâmetros não permitidos.");
    }
    if (allowLocalUrls) return;
    for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
      byte[] bytes = address.getAddress();
      boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()
          || uniqueLocalIpv6) {
        throw new HarnessException("O harness não pode acessar uma URL de rede privada.");
      }
    }
  }

  /** Preserva resultado, arquivos temporários e entrada auditável até o callback. */
  record HarnessExecution(
      JsonNode result,
      BpmVisualEvidenceRunner.VisualEvidenceBundle visualEvidence,
      String serializedInput,
      String sourceUrl) {}

  /** Diferencia falha do harness de uma reprovação funcional posterior de Psique. */
  static final class HarnessException extends IllegalStateException {
    private final boolean executorFailure;

    /** Cria um bloqueio técnico explícito e recuperável. */
    HarnessException(String message) {
      this(message, false);
    }

    /** Cria um bloqueio identificado como indisponibilidade do próprio executor. */
    private HarnessException(String message, boolean executorFailure) {
      super(message);
      this.executorFailure = executorFailure;
    }

    /** Marca falha de catálogo, configuração ou execução do harness sem culpar o protótipo. */
    static HarnessException executor(String message) {
      return new HarnessException(message, true);
    }

    /** Informa se a mesma versão pode ser repetida depois da correção do executor. */
    boolean isExecutorFailure() {
      return executorFailure;
    }
  }
}
