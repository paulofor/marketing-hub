package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTask;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/** Responsabilidade: vincular suplemento de prova ao parecer exato, sem conceder aprovação. */
final class PdeReviewEvidenceReevaluation {
  private static final Set<String> SCENARIO_REVIEWS =
      Set.of("psiqueAdherent", "psiqueRecovery", "psiqueSafety");

  /** Impede instância de uma política sem estado. */
  private PdeReviewEvidenceReevaluation() {}

  /**
   * Permite somente reavaliar a lacuna documentada com a implementação preservada; novo bloqueio,
   * outro contexto ou mudança funcional não reutilizam a autorização deste suplemento.
   */
  static boolean matches(JsonNode report, AgentTask rejected, Long productId, String reference)
      throws java.security.NoSuchAlgorithmException {
    JsonNode supplement = report.path("reviewReevaluation");
    if (!SCENARIO_REVIEWS.contains(rejected.getProcessActivityId())
        || !"PDE_REVIEW_EVIDENCE_SUPPLEMENT_V1"
            .equals(supplement.path("contractVersion").asText())
        || !"MISSING_EXECUTION_EVIDENCE".equals(supplement.path("reasonCode").asText())
        || supplement.path("implementationChanged").asBoolean(true)
        || !supplement.path("independentReviewRequired").asBoolean(false)
        || report.path("agentApprovalClaimed").asBoolean(true)
        || !report.path("reportSha256").asText().matches("[a-f0-9]{64}")
        || productId == null
        || productId != supplement.path("productId").asLong(-1)
        || !reference.equals(supplement.path("sourceReference").asText())
        || rejected.getId() != supplement.path("blockedTaskId").asLong(-1)
        || !rejected.getProcessActivityId().equals(supplement.path("activityId").asText())
        || rejected.getResultJson() == null) return false;
    String resultHash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(rejected.getResultJson().getBytes(StandardCharsets.UTF_8)));
    return resultHash.equals(supplement.path("blockedResultSha256").asText());
  }
}
