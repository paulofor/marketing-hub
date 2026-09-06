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

/** Responsabilidade: executar e validar o harness determinístico multiagente de um PDE real. */
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

  /** Executa caminho feliz, recuperação e segurança sem enviar segredo no arquivo de entrada. */
  HarnessExecution run(
      Map<String, Object> task, String mode, String scenarioCode, Path workDirectory)
      throws Exception {
    if (internalToken.isBlank()) {
      throw new HarnessException(
          "PDE_INTERNAL_API_TOKEN não está configurado no worker de Psique.");
    }
    JsonNode target = json.valueToTree(task.get("taskTarget"));
    String sourceUrl = target.path("publicUrl").asText("").trim();
    validateUrl(sourceUrl);
    String sourceReference = String.valueOf(task.get("sourceReference"));
    if (!sourceReference.matches("product:[1-9][0-9]*@agent-validation-v1")) {
      throw new HarnessException("A tarefa não pertence à referência multiagente canônica.");
    }
    long productId = target.path("productId").asLong();
    String productSlug = target.path("productSlug").asText("").trim();
    String prototypeVersion = target.path("experienceVersion").asText("").trim();
    if (productId < 1 || productSlug.isBlank() || prototypeVersion.isBlank()) {
      throw new HarnessException("O alvo da tarefa multiagente está incompleto.");
    }
    Files.createDirectories(workDirectory);
    Path inputPath = workDirectory.resolve("agent-validation-input.json");
    Path outputPath = workDirectory.resolve("agent-validation-output.json");
    Path evidenceDirectory = workDirectory.resolve("agent-validation-evidence");
    String captureSessionId = UUID.randomUUID().toString();
    Map<String, Object> input =
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
            prototypeVersion);
    String serializedInput = json.writeValueAsString(input);
    Files.writeString(inputPath, serializedInput, StandardCharsets.UTF_8);
    ProcessBuilder builder =
        new ProcessBuilder(
                nodeBinary,
                scriptPath,
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
      throw new HarnessException(
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

  /** Exige contrato, cenários, dispositivos, efeitos nulos e arquivos PNG da mesma execução. */
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
      if (result.path("devices").size() != 3
          || result.path("scenarios").size() != 5
          || result.path("artifacts").size() != 5
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
      if (!captureSessionId.equals(artifact.path("captureSessionId").asText())
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

  /** Bloqueia credenciais na URL e redes privadas fora da homologação local explícita. */
  private void validateUrl(String value) throws Exception {
    URI uri = URI.create(value == null ? "" : value.trim());
    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getRawQuery() != null) {
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
    /** Cria um bloqueio técnico explícito e recuperável. */
    HarnessException(String message) {
      super(message);
    }
  }
}
