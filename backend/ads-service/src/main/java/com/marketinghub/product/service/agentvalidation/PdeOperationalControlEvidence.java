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
    try (var indexInput =
        resources
            .getResource("classpath:contracts/pde-operational-controls-catalog-v1.json")
            .getInputStream()) {
      var matches = new ArrayList<JsonNode>();
      for (var item : json.readTree(indexInput).path("reports"))
        if (Objects.equals(productSlug, item.path("productSlug").asText())
            && Objects.equals(prototypeVersion, item.path("prototypeVersion").asText()))
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
   * rastreável.
   */
  private boolean valid(JsonNode report, String slug, String version) {
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
    return !required.isEmpty() && codes.equals(required);
  }
}
