package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsabilidade: identificar a correção vigente e validar seu contrato antes da revalidação PDE.
 */
@Slf4j
final class PdeAgentValidationCorrectionPolicy {
  private static final String CANCELLED = "CANCELLED";
  private static final String COMPLETED = "COMPLETED";

  private final ObjectMapper json;

  /** Configura o leitor do resultado estruturado da correção. */
  PdeAgentValidationCorrectionPolicy(ObjectMapper json) {
    this.json = json;
  }

  /**
   * Seleciona a tentativa mais recente ainda aplicável, mantendo cancelamentos somente no
   * histórico.
   */
  Optional<CorrectionAttempt> latestApplicableAttempt(List<CorrectionAttempt> attempts) {
    return attempts.stream()
        .filter(java.util.Objects::nonNull)
        .filter(attempt -> attempt.id() != null)
        .filter(attempt -> !CANCELLED.equals(attempt.status()))
        .max(Comparator.comparing(CorrectionAttempt::id));
  }

  /** Exige conclusão e contrato válido da tentativa aplicável para a versão aceita. */
  Optional<CorrectionAttempt> latestValidCompletedAttempt(
      List<CorrectionAttempt> attempts, String expectedVersion) {
    return latestApplicableAttempt(attempts)
        .filter(attempt -> COMPLETED.equals(attempt.status()))
        .filter(attempt -> validForVersion(attempt, expectedVersion));
  }

  /** Confirma versão sucessora, retorno à homologação e ausência de efeitos externos. */
  boolean validForVersion(CorrectionAttempt attempt, String expectedVersion) {
    if (attempt == null
        || expectedVersion == null
        || expectedVersion.isBlank()
        || attempt.resultJson() == null) return false;
    try {
      JsonNode result = json.readTree(attempt.resultJson());
      JsonNode plan = result.path("correctionPlan");
      return "READY".equals(result.path("decision").asText())
          && expectedVersion.equals(plan.path("correctedPrototypeVersion").asText())
          && !expectedVersion.equals(plan.path("previousPrototypeVersion").asText())
          && "technicalHomologation".equals(plan.path("nextActivityId").asText())
          && plan.path("verification").path("technicalRevalidationRequired").asBoolean(false)
          && plan.path("verification").path("noExternalSideEffects").asBoolean(false);
    } catch (Exception ex) {
      log.error("Falha ao ler correção PDE. taskId={}", attempt.id(), ex);
      return false;
    }
  }

  /** Transporta somente os campos necessários para decidir a vigência da correção. */
  record CorrectionAttempt(Long id, String status, String resultJson, Instant deliveredAt) {}
}
