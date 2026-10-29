package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTask;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

/** Vincula uma revalidação independente de segurança à correção de controles privados exatos. */
final class PdeScopedSafetyRevalidation {
  /** Impede instâncias de uma política sem estado. */
  private PdeScopedSafetyRevalidation() {}

  /**
   * Reconhece somente restauração de controles fora do contexto privado válido, com aceite técnico
   * rastreável; não permite mudar experiência, oferta ou condições do experimento.
   */
  static boolean matches(
      JsonNode report,
      AgentTask rejected,
      AgentTask previousSafety,
      Long productId,
      String reference,
      String version)
      throws Exception {
    var scope = report.path("scopedRevalidation");
    var acceptance = scope.path("technicalAcceptance");
    if (rejected == null
        || previousSafety == null
        || productId == null
        || reference == null
        || version == null
        || rejected.getUpdatedAt() == null
        || rejected.getId() == null
        || previousSafety.getId() == null
        || rejected.getResultJson() == null
        || previousSafety.getResultJson() == null
        || !"BLOCKED".equals(rejected.getStatus())
        || !"FUNCTIONAL_ADJUSTMENT".equals(rejected.getBlockerCategory())
        || !"commercialIntegrityReview".equals(rejected.getProcessActivityId())
        || !"COMPLETED".equals(previousSafety.getStatus())
        || !"psiqueSafety".equals(previousSafety.getProcessActivityId())
        || !reference.equals(rejected.getSourceReference())
        || !reference.equals(previousSafety.getSourceReference())
        || !version.equals(report.path("prototypeVersion").asText())
        || !"PDE_CONTEXTUAL_EXECUTION_CONTROLS_V1"
            .equals(report.path("operationalControlsContract").asText())
        || !"PDE_SCOPED_SAFETY_REVALIDATION_V1".equals(scope.path("contractVersion").asText())
        || !"PRIVATE_CONTEXT_CONTROLS".equals(scope.path("scope").asText())
        || scope.path("customerExperienceChanged").asBoolean(true)
        || !scope.path("independentReviewRequired").asBoolean(false)
        || !report
            .path("implementationChange")
            .path("closedContextProtectionChanged")
            .asBoolean(false)
        || report.path("implementationChange").path("validContextExperienceChanged").asBoolean(true)
        || report.path("agentApprovalClaimed").asBoolean(true)
        || !report.path("reportSha256").asText().matches("[a-f0-9]{64}")
        || productId != scope.path("productId").asLong(-1)
        || !reference.equals(scope.path("sourceReference").asText())
        || rejected.getId() != scope.path("blockedTaskId").asLong(-1)
        || previousSafety.getId() != scope.path("previousSafetyTaskId").asLong(-1)
        || !hash(rejected.getResultJson()).equals(scope.path("blockedResultSha256").asText())
        || !hash(previousSafety.getResultJson())
            .equals(scope.path("previousSafetyResultSha256").asText())
        || !"OPERATOR_VALIDATION".equals(acceptance.path("origin").asText())
        || !"ACCEPTED_CONTROLS".equals(acceptance.path("decision").asText())
        || !acceptance.path("backendRevision").asText().matches("[a-f0-9]{40}")
        || acceptance.path("evidenceReference").asText().isBlank()
        || !acceptance.path("testReceiptSha256").asText().matches("[a-f0-9]{64}")
        || !acceptance.path("sourceSha256").asText().matches("[a-f0-9]{64}")
        || acceptance.path("sourcePath").asText().isBlank()
        || !report.path("testReceiptSha256").equals(acceptance.path("testReceiptSha256"))
        || !Instant.parse(report.path("generatedAt").asText()).isAfter(rejected.getUpdatedAt()))
      return false;
    var retained = new java.util.HashSet<String>();
    scope.path("retainedActivities").forEach(value -> retained.add(value.asText()));
    if (!retained.equals(Set.of("psiqueAdherent", "psiqueRecovery"))
        || !"psiqueSafety".equals(scope.path("affectedActivityId").asText())) return false;
    for (var source : report.path("sourceFiles"))
      if (source.path("path").equals(acceptance.path("sourcePath"))
          && source.path("sha256").equals(acceptance.path("sourceSha256"))) return true;
    return false;
  }

  /** Exige novo parecer aprovado, posterior e com a citação literal da prova técnica atual. */
  static boolean reviewed(
      JsonNode report, AgentTask current, com.fasterxml.jackson.databind.ObjectMapper json)
      throws Exception {
    if (current == null
        || current.getId() == null
        || current.getUpdatedAt() == null
        || !"COMPLETED".equals(current.getStatus())
        || current.getResultJson() == null
        || current.getId()
            <= report.path("scopedRevalidation").path("previousSafetyTaskId").asLong(Long.MAX_VALUE)
        || !current.getUpdatedAt().isAfter(Instant.parse(report.path("generatedAt").asText())))
      return false;
    var result = json.readTree(current.getResultJson());
    if (!"APPROVED".equals(result.path("decision").asText())
        || !report.path("prototypeVersion").equals(result.path("prototypeVersion"))) return false;
    for (var citation : result.path("evidence"))
      if (citation.asText().contains(report.path("reportSha256").asText())) return true;
    return false;
  }

  /** Usa os bytes persistidos do parecer para impedir troca de decisão ou identidade. */
  private static String hash(String value) throws Exception {
    if (value == null) return "";
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
