package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: entregar provas determinísticas versionadas dos controles da candidata exata.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdeOperationalControlEvidence {
  private final ObjectMapper json;
  private final ResourceLoader resources;

  /**
   * Confere identidade, hash, origem e cobertura sem converter teste local em aprovação de agente.
   */
  public Optional<JsonNode> resolve(String productSlug, String prototypeVersion) {
    return resolveCatalog(productSlug, prototypeVersion, null);
  }

  /** Recupera os bytes exatos referidos por um parecer, sem promover a prova histórica a atual. */
  public Optional<JsonNode> resolveReviewed(
      String productSlug, String prototypeVersion, String reportSha256) {
    if (reportSha256 == null || !reportSha256.matches("[a-f0-9]{64}")) return Optional.empty();
    return resolveCatalog(productSlug, prototypeVersion, reportSha256);
  }

  /** Confere índice, identidade e hash; arquivos históricos só participam de consulta explícita. */
  private Optional<JsonNode> resolveCatalog(
      String productSlug, String prototypeVersion, String reportSha256) {
    try (var indexInput =
        resources
            .getResource("classpath:contracts/pde-operational-controls-catalog-v1.json")
            .getInputStream()) {
      var matches = new ArrayList<JsonNode>();
      var index = json.readTree(indexInput);
      var sections =
          reportSha256 == null ? List.of("reports") : List.of("reports", "historicalReports");
      for (String section : sections)
        for (var item : index.path(section))
          if (Objects.equals(productSlug, item.path("productSlug").asText())
              && Objects.equals(prototypeVersion, item.path("prototypeVersion").asText())
              && (reportSha256 == null || reportSha256.equals(item.path("sha256").asText())))
            matches.add(item);
      if (matches.size() != 1) return Optional.empty();
      var item = matches.getFirst();
      String path = item.path("resource").asText();
      if (!path.matches("contracts/operational-evidence/[a-z0-9-]+\\.json"))
        return Optional.empty();
      byte[] raw;
      try (var input = resources.getResource("classpath:" + path).getInputStream()) {
        raw = input.readAllBytes();
      }
      String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
      if (!digest.equals(item.path("sha256").asText())) return Optional.empty();
      var report = json.readTree(raw);
      if (!valid(report, productSlug, prototypeVersion)) return Optional.empty();
      ObjectNode evidence = report.deepCopy();
      evidence.put("reportSha256", digest);
      evidence.put("resource", path);
      evidence.put("agentApprovalClaimed", false);
      evidence.put("commercialEvidenceClaimed", false);
      return Optional.of(evidence);
    } catch (Exception ex) {
      log.warn(
          "Prova de controles indisponível productSlug={} prototypeVersion={}",
          productSlug,
          prototypeVersion,
          ex);
      return Optional.empty();
    }
  }

  /**
   * Recusa provas incompletas, origem ambígua, efeitos externos e critérios sem resultado
   * rastreável, incluindo os controles executados quando o relatório declarar esse contrato.
   */
  private boolean valid(JsonNode report, String slug, String version) throws java.io.IOException {
    if (!"PDE_OPERATIONAL_CONTROLS_EVIDENCE_V1".equals(report.path("contractVersion").asText())
        || !slug.equals(report.path("productSlug").asText())
        || !version.equals(report.path("prototypeVersion").asText())
        || !"PASS".equals(report.path("status").asText())
        || !"LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES".equals(report.path("origin").asText())
        || report.path("providerCalls").asInt(-1) != 0
        || report.path("commercialSideEffects").asBoolean(true)
        || !report.path("frontendSourceFingerprint").asText().matches("[a-f0-9]{64}")
        || !report.path("testReceiptSha256").asText().matches("[a-f0-9]{64}")
        || !report.path("criteria").isArray()
        || report.path("criteria").isEmpty()) return false;
    if (Instant.parse(report.path("generatedAt").asText()).isAfter(Instant.now().plusSeconds(60)))
      return false;
    Set<String> codes = new HashSet<>();
    for (var criterion : report.path("criteria")) {
      if (!codes.add(criterion.path("code").asText())
          || criterion.path("code").asText().isBlank()
          || !"PASS".equals(criterion.path("status").asText())
          || criterion.path("testMethod").asText().isBlank()
          || !criterion.path("resultSha256").asText().matches("[a-f0-9]{64}")) return false;
    }
    Set<String> required = new HashSet<>();
    report.path("requiredCriteria").forEach(node -> required.add(node.asText()));
    return !required.isEmpty() && codes.equals(required) && validOperationalControls(report);
  }

  /** Impede que contagens observadas substituam testes de limites, concorrência e acesso. */
  private boolean validOperationalControls(JsonNode report) throws java.io.IOException {
    JsonNode declared = report.path("operationalControlsContract");
    if (declared.isMissingNode()) return true;
    JsonNode policy;
    try (var input =
        resources
            .getResource("classpath:contracts/pde-private-execution-controls-v1.json")
            .getInputStream()) {
      policy = json.readTree(input);
    }
    if (!policy.path("contractVersion").equals(declared)) return false;
    Set<String> required = new HashSet<>();
    policy.path("requiredCriteria").forEach(value -> required.add(value.asText()));
    for (JsonNode criterion : report.path("criteria")) {
      String code = criterion.path("code").asText();
      if (!required.contains(code)) continue;
      JsonNode result = criterion.path("result");
      if (!"EXECUTION".equals(criterion.path("verificationType").asText())
          || !"PASS".equals(result.path("status").asText())
          || !result.path("contexts").isArray()) return false;
      Set<Long> identities = new HashSet<>();
      for (JsonNode context : result.path("contexts")) {
        if (context.path("productId").asLong() < 1
            || !"PASS".equals(context.path("status").asText())
            || !context.path("observed").isObject()
            || context.path("observed").isEmpty()) return false;
        identities.add(context.path("productId").asLong());
      }
      if (identities.size() < policy.path("minimumIndependentContexts").asInt()) return false;
      required.remove(code);
    }
    return required.isEmpty();
  }
}
