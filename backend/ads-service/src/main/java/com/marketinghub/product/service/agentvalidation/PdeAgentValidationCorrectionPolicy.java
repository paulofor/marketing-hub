package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsabilidade: identificar a correção vigente e validar seu contrato antes da revalidação PDE.
 */
@Slf4j
public final class PdeAgentValidationCorrectionPolicy {
  private static final String CANCELLED = "CANCELLED";
  private static final String COMPLETED = "COMPLETED";
  private static final Set<String> LEGACY_EXECUTOR_ERRORS =
      Set.of(
          "O harness instalado não possui cenários próprios para este produto.",
          "A referência da homologação não corresponde ao produto alvo.",
          "Timeout ao homologar o PDE nos dispositivos suportados.");

  private final ObjectMapper json;

  /** Configura o leitor do resultado estruturado da correção. */
  public PdeAgentValidationCorrectionPolicy(ObjectMapper json) {
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

  /**
   * Reconhece falhas do executor e diagnósticos legados sem alterar a classificação salva. A
   * divergência antiga permite apenas nova verificação determinística de identidade e jornada;
   * nunca constitui aprovação ou dispensa de prova funcional.
   */
  public static boolean retryableExecutorFailure(PdeValidationTaskSnapshot task) {
    if (task == null || !"BLOCKED".equals(task.status())) return false;
    if ("EXECUTOR_FAILURE".equals(task.blockerCategory())) return true;
    return "TECHNICAL_FAILURE".equals(task.blockerCategory())
        && "technicalHomologation".equals(task.processActivityId())
        && task.executionError() != null
        && LEGACY_EXECUTOR_ERRORS.stream().anyMatch(task.executionError()::contains);
  }

  /**
   * Vincula a correção sem nova versão à falha exclusiva do executor e à homologação posterior. O
   * gate continua responsável por validar integralmente a técnica e as revisões independentes.
   */
  public Optional<ObjectNode> executorCorrectionResolution(
      List<PdeValidationTaskSnapshot> history,
      PdeValidationTaskSnapshot correction,
      PdeValidationTaskSnapshot technical,
      Long productId,
      String sourceReference,
      String prototypeVersion) {
    if (correction == null
        || technical == null
        || productId == null
        || sourceReference == null
        || prototypeVersion == null
        || !"prototypeCorrection".equals(correction.processActivityId())
        || !"BLOCKED".equals(correction.status())
        || !"technicalHomologation".equals(technical.processActivityId())
        || !COMPLETED.equals(technical.status())
        || correction.id() == null
        || technical.id() == null
        || technical.id() <= correction.id()
        || correction.processDefinitionId() == null
        || !correction.processDefinitionId().equals(technical.processDefinitionId())
        || correction.updatedAt() == null
        || technical.updatedAt() == null
        || !technical.updatedAt().isAfter(correction.updatedAt())
        || correction.resultJson() == null
        || technical.resultJson() == null) return Optional.empty();
    try {
      var draft = json.readTree(correction.resultJson());
      var plan = draft.path("correctionPlan");
      var verification = plan.path("verification");
      var failed =
          history.stream()
              .filter(
                  task ->
                      Objects.equals(task.processDefinitionId(), correction.processDefinitionId()))
              .filter(
                  task -> task.id() != null && task.id() == plan.path("sourceTaskId").asLong(-1))
              .findFirst()
              .orElse(null);
      if (!"BLOCKED".equals(draft.path("decision").asText())
          || !"technicalHomologation".equals(plan.path("rejectedActivityId").asText())
          || !"technicalHomologation".equals(plan.path("nextActivityId").asText())
          || !prototypeVersion.equals(plan.path("previousPrototypeVersion").asText())
          || !verification.path("technicalRevalidationRequired").asBoolean(false)
          || !verification.path("noExternalSideEffects").asBoolean(false)
          || !verification.path("newVersionPublished").isBoolean()
          || verification.path("newVersionPublished").asBoolean()
          || !retryableExecutorFailure(failed)
          || !"technicalHomologation".equals(failed.processActivityId())
          || failed.id() >= correction.id()
          || failed.updatedAt() == null
          || failed.updatedAt().isAfter(correction.updatedAt())) return Optional.empty();
      var approved = json.readTree(technical.resultJson());
      if (!"APPROVED".equals(approved.path("decision").asText())
          || productId != approved.path("productId").asLong(-1)
          || !sourceReference.equals(approved.path("sourceReference").asText())
          || !prototypeVersion.equals(approved.path("prototypeVersion").asText()))
        return Optional.empty();
      var resolution =
          json.createObjectNode()
              .put("contractVersion", "PDE_EXECUTOR_CORRECTION_RESOLUTION_V1")
              .put("productId", productId)
              .put("sourceReference", sourceReference)
              .put("prototypeVersion", prototypeVersion)
              .put("processDefinitionId", correction.processDefinitionId())
              .put("correctionTaskId", correction.id())
              .put("correctionResultSha256", sha(correction.resultJson()))
              .put("failedTaskId", failed.id())
              .put("failedBlockerCategory", failed.blockerCategory())
              .put("failedExecutionErrorSha256", sha(failed.executionError()))
              .put("failedResultSha256", sha(failed.resultJson()))
              .put("technicalTaskId", technical.id())
              .put("technicalResultSha256", sha(technical.resultJson()));
      // Conserva a representação numérica da leitura persistida para comparar recibos sem
      // duplicá-los.
      return Optional.of((ObjectNode) json.readTree(resolution.toString()));
    } catch (Exception ex) {
      log.warn(
          "Resolução de correção do executor indisponível. productId={} sourceReference={} correctionTaskId={} technicalTaskId={}",
          productId,
          sourceReference,
          correction.id(),
          technical.id(),
          ex);
      return Optional.empty();
    }
  }

  /** Calcula a identidade dos dados originais sem reescrever falhas nem saídas históricas. */
  private String sha(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256")
                .digest(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8)));
  }

  /** Transporta somente os campos necessários para decidir a vigência da correção. */
  record CorrectionAttempt(Long id, String status, String resultJson, Instant deliveredAt) {}
}
