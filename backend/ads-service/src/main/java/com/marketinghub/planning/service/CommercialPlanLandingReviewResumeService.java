package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.agent.v1.LandingCheckoutEvidenceResolver;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: comprovar quando uma landing aprovada pode ser reutilizada na retomada dos
 * revisores, sem nova execução do Dédalo.
 */
@Service
public class CommercialPlanLandingReviewResumeService {
  private static final Logger log =
      LoggerFactory.getLogger(CommercialPlanLandingReviewResumeService.class);
  private static final String QUALITY_REVIEW_STAGE = "landing-page-quality-review";
  private static final String QUALITY_APPROVED = "APPROVE_FOR_PUBLICATION";
  private static final String CHECKOUT_VALIDATED = "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING";
  private final ExperimentRepository experimentRepository;
  private final GeraLandingStageExecutionRepository landingExecutionRepository;
  private final LandingCheckoutEvidenceResolver checkoutEvidenceResolver;
  private final CommercialPlanApprovedCreativeEvidenceService approvedCreativeEvidenceService;
  private final ObjectMapper objectMapper;

  /** Inicializa a decisão com as fontes canônicas da landing, checkout e pacote criativo. */
  public CommercialPlanLandingReviewResumeService(
      ExperimentRepository experimentRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      LandingCheckoutEvidenceResolver checkoutEvidenceResolver,
      CommercialPlanApprovedCreativeEvidenceService approvedCreativeEvidenceService,
      ObjectMapper objectMapper) {
    this.experimentRepository = experimentRepository;
    this.landingExecutionRepository = landingExecutionRepository;
    this.checkoutEvidenceResolver = checkoutEvidenceResolver;
    this.approvedCreativeEvidenceService = approvedCreativeEvidenceService;
    this.objectMapper = objectMapper;
  }

  /**
   * Retorna o snapshot atualizado somente quando o bloqueio foi de transporte de evidência e o HTML
   * ainda coincide com a aprovação técnica independente.
   */
  @Transactional(readOnly = true)
  public Optional<String> buildResumeBrief(
      Long planId,
      Long experimentId,
      int attemptNumber,
      List<Map<String, Object>> previousAttemptBlocks,
      List<AgentTask> currentTasks) {
    Optional<AgentTask> landing = task(currentTasks, "landing-generator", "html", "COMPLETED");
    Optional<AgentTask> customer = task(currentTasks, "customer-agent", "customer", "BLOCKED");
    if (landing.isEmpty()
        || customer.isEmpty()
        || !isEvidenceTransportBlock(customer.get(), landing.get())) {
      return Optional.empty();
    }
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experimento da landing não encontrado."));
    Optional<GeraLandingStageExecution> qualityReview =
        approvedQualityReview(experimentId, landing.get().getId());
    if (qualityReview.isEmpty()
        || !matchesApprovedHtml(qualityReview.get(), experiment.getHtmlGeraLanding())) {
      return Optional.empty();
    }
    Map<String, Object> checkoutContract = checkoutEvidenceResolver.resolve(experiment);
    requireStatus(
        checkoutContract,
        "validationStatus",
        CHECKOUT_VALIDATED,
        "O checkout canônico ainda não possui vínculo comercial íntegro.");
    Map<String, Object> approvedCreativeEvidence =
        approvedCreativeEvidenceService.resolve(experimentId);
    requireStatus(
        approvedCreativeEvidence,
        "status",
        "APPROVED",
        "O pacote criativo aprovado não está disponível para a revisão.");
    return Optional.of(
        serializeResumeBrief(
            planId,
            experimentId,
            attemptNumber,
            previousAttemptBlocks,
            landing.get(),
            qualityReview.get(),
            checkoutContract,
            approvedCreativeEvidence));
  }

  /** Localiza a tarefa mais nova com a identidade e o estado funcional esperados. */
  private Optional<AgentTask> task(
      List<AgentTask> tasks, String agentKey, String activityId, String status) {
    return tasks.stream()
        .filter(task -> task.getAssignedAgent() != null)
        .filter(task -> agentKey.equals(task.getAssignedAgent().getAgentKey()))
        .filter(task -> activityId.equals(task.getProcessActivityId()))
        .filter(task -> status.equals(task.getStatus()))
        .max(java.util.Comparator.comparing(AgentTask::getId));
  }

  /** Distingue falta de transporte de evidência de defeitos reais na landing ou no contrato. */
  private boolean isEvidenceTransportBlock(AgentTask customer, AgentTask landing) {
    if (containsCanonicalCheckout(landing.getEvidenceJson())) return false;
    try {
      JsonNode result = objectMapper.readTree(customer.getResultJson());
      String remediationTarget = result.path("remediationTarget").asText();
      if (StringUtils.hasText(remediationTarget)) {
        return "EVIDENCE_TRANSPORT".equals(remediationTarget);
      }
      JsonNode changes = result.path("requiredChanges");
      if (!changes.isArray() || changes.isEmpty()) return false;
      for (JsonNode change : changes) {
        if (!describesEvidenceTransport(change.asText())) return false;
      }
      return true;
    } catch (Exception ex) {
      log.error(
          "Falha ao classificar bloqueio da Psique para retomada da landing. customerTaskId={} landingTaskId={}",
          customer.getId(),
          landing.getId(),
          ex);
      return false;
    }
  }

  /** Confirma se a tarefa antiga já possuía o contrato que motivou o bloqueio. */
  private boolean containsCanonicalCheckout(String evidenceJson) {
    if (!StringUtils.hasText(evidenceJson)) return false;
    try {
      JsonNode evidence = objectMapper.readTree(evidenceJson);
      return evidence.path("checkoutContract").isObject()
          && CHECKOUT_VALIDATED.equals(
              evidence.path("checkoutContract").path("validationStatus").asText());
    } catch (Exception ex) {
      log.error("Falha ao ler evidência da tarefa de landing para retomada.", ex);
      return false;
    }
  }

  /** Reconhece as descrições legadas do bloqueio do Rigel sem generalizar defeitos de conteúdo. */
  private boolean describesEvidenceTransport(String text) {
    String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
    return normalized.contains("checkouturl")
        || normalized.contains("evidência auditável")
        || normalized.contains("evidencia auditavel")
        || normalized.contains("campo auditável")
        || normalized.contains("campo auditavel")
        || normalized.contains("binding comercial")
        || (normalized.contains("checkout")
            && (normalized.contains("persistir")
                || normalized.contains("expor")
                || normalized.contains("fornecer")
                || normalized.contains("comprovar")
                || normalized.contains("confirmar")
                || normalized.contains("audit")));
  }

  /** Seleciona somente a aprovação técnica ligada à mesma tarefa de Dédalo. */
  private Optional<GeraLandingStageExecution> approvedQualityReview(
      Long experimentId, Long landingTaskId) {
    return landingExecutionRepository
        .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
            experimentId, QUALITY_REVIEW_STAGE, "agent-task:" + landingTaskId)
        .stream()
        .filter(execution -> "CONCLUIDO".equals(execution.getStatus()))
        .filter(this::wasApproved)
        .findFirst();
  }

  /** Confirma a recomendação explícita do Quality Review sem inferir aprovação pelo score. */
  private boolean wasApproved(GeraLandingStageExecution execution) {
    try {
      return QUALITY_APPROVED.equals(
          objectMapper
              .readTree(execution.getModelResponse())
              .path("approvalRecommendation")
              .asText());
    } catch (Exception ex) {
      log.error(
          "Falha ao ler parecer do Quality Review para retomada. experimentId={} cycleId={}",
          execution.getExperimentId(),
          execution.getAutonomousCycleId(),
          ex);
      return false;
    }
  }

  /** Impede reutilizar uma aprovação técnica depois de qualquer alteração no HTML persistido. */
  private boolean matchesApprovedHtml(GeraLandingStageExecution execution, String currentHtml) {
    if (!StringUtils.hasText(currentHtml)) return false;
    try {
      String approvedHash =
          objectMapper
              .readTree(execution.getQualityReviewAudit())
              .path("landingHtmlSha256")
              .asText();
      return StringUtils.hasText(approvedHash) && approvedHash.equals(sha256(currentHtml));
    } catch (Exception ex) {
      log.error(
          "Falha ao validar integridade da landing aprovada. experimentId={} cycleId={}",
          execution.getExperimentId(),
          execution.getAutonomousCycleId(),
          ex);
      return false;
    }
  }

  /** Bloqueia a retomada quando uma evidência canônica ainda não satisfaz seu próprio contrato. */
  private void requireStatus(
      Map<String, Object> evidence, String statusField, String expectedStatus, String message) {
    if (!expectedStatus.equals(String.valueOf(evidence.get(statusField)))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
  }

  /**
   * Monta a nova entrada auditável para Psique e Têmis sem autorizar reconstrução ou publicação.
   */
  private String serializeResumeBrief(
      Long planId,
      Long experimentId,
      int attemptNumber,
      List<Map<String, Object>> previousAttemptBlocks,
      AgentTask landing,
      GeraLandingStageExecution qualityReview,
      Map<String, Object> checkoutContract,
      Map<String, Object> approvedCreativeEvidence) {
    Map<String, Object> brief = new LinkedHashMap<>();
    brief.put("resumeMode", "REUSE_APPROVED_LANDING_WITH_FRESH_CANONICAL_EVIDENCE");
    brief.put("commercialPlanId", planId);
    brief.put("experimentId", experimentId);
    brief.put("journeyAttempt", attemptNumber);
    brief.put("sourceLandingTaskId", landing.getId());
    brief.put("qualityReviewExecutionId", readableJobId(qualityReview.getIdJob()));
    brief.put("qualityReview", parseJson(qualityReview.getModelResponse(), "parecer técnico"));
    brief.put(
        "qualityReviewAudit",
        parseJson(qualityReview.getQualityReviewAudit(), "auditoria técnica"));
    brief.put("checkoutContract", checkoutContract);
    brief.put("approvedCreativeEvidence", approvedCreativeEvidence);
    brief.put("previousAttemptBlocks", previousAttemptBlocks);
    brief.put("landingRegenerationAuthorized", false);
    brief.put("publicationAuthorized", false);
    brief.put("mediaSpendAuthorized", false);
    brief.put(
        "reviewInstruction",
        "Reavaliar a landing aprovada usando estas evidências canônicas atualizadas, sem solicitar reconstrução quando conteúdo e HTML permanecem íntegros.");
    try {
      return objectMapper.writeValueAsString(brief);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao montar snapshot de retomada dos revisores. planId={} experimentId={} landingTaskId={}",
          planId,
          experimentId,
          landing.getId(),
          ex);
      throw new IllegalStateException("Não foi possível montar o snapshot da retomada", ex);
    }
  }

  /** Converte o JSON auditável sem criar JSON serializado dentro de outro JSON. */
  private JsonNode parseJson(String json, String label) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler {} da retomada dos revisores.", label, ex);
      throw new IllegalStateException("JSON inválido em " + label, ex);
    }
  }

  /** Preserva o identificador textual original do job quando ele foi persistido como binário. */
  private String readableJobId(byte[] jobId) {
    return jobId == null ? null : new String(jobId, StandardCharsets.UTF_8);
  }

  /** Calcula a assinatura canônica usada pelo Quality Review para provar o HTML aprovado. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("Algoritmo SHA-256 indisponível ao validar a landing aprovada.", ex);
      throw new IllegalStateException("Não foi possível validar a assinatura da landing", ex);
    }
  }
}
