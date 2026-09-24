package com.marketinghub.safira.commercial.v1.service;

import static com.marketinghub.safira.commercial.v1.service.SafiraCommercialContext.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: governar as provas, revisões independentes e consolidação comercial de Safira.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafiraCommercialService
    implements BackendProductProcessActivityExecutor, AgentTaskCompletionHook {
  public static final List<String> REVIEWS =
      List.of("humanExperienceReview", "commercialIntegrityReview");
  public static final Set<String> REVIEW_GATES =
      Set.of(
          "OFFER",
          "PRICE",
          "PRODUCT_PROOF",
          "CHECKOUT",
          "DELIVERY",
          "PERSONALIZATION",
          "REFUND_SUPPORT",
          "TRACKING",
          "ECONOMICS",
          "IDENTITY");

  private final SafiraCommercialContext context;
  private final SafiraCommercialChecks checks;
  private final BusinessProcessActivityInstanceRepository instances;
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final BusinessProcessDefinitionRepository processes;
  private final AgentTaskRepository tasks;
  private final ProductProcessActivityPredecessorService predecessors;

  /** Reconhece as atividades determinísticas do subprocesso Safira publicado. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && SafiraCommercialContext.CODE.equals(process.getProcessCode())
        && (SafiraCommercialChecks.PREPARATION.contains(activity.getActivityId())
            || "ready".equals(activity.getActivityId()));
  }

  /** Reconhece exclusivamente callbacks de Psique e Têmis do percurso Safira. */
  @Override
  public boolean supports(AgentTask task) {
    return task != null
        && task.getProcessDefinition() != null
        && SafiraCommercialContext.CODE.equals(task.getProcessDefinition().getProcessCode())
        && REVIEWS.contains(task.getProcessActivityId());
  }

  /** Expõe a causa concreta antes de gravar atividade ou liberar chamada paga. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    Long productId = product == null ? null : product.getId();
    try {
      require(productId != null, "Selecione o produto Safira antes da preparação comercial.");
      var scope = context.scope(source, productId, true);
      var prior = predecessors.readiness(process, activity, source);
      require(prior.ready(), prior.reason());
      JsonNode snapshot = context.snapshot(source);
      if ("ready".equals(activity.getActivityId())) {
        prepared(process, scope, source, snapshot);
        for (String review : REVIEWS) currentReview(process, source, review, snapshot);
      } else {
        checks.check(activity.getActivityId(), scope, snapshot);
      }
      return new BackendProductProcessActivityReadiness(
          true,
          "Fontes Safira atuais conferidas; esta atividade não autoriza campanha, publicação ou gasto.");
    } catch (RuntimeException ex) {
      log.warn(
          "Safira: preparação bloqueada productId={} source={} activity={}",
          productId,
          source,
          activity == null ? null : activity.getActivityId(),
          ex);
      String url =
          activity != null && "economics".equals(activity.getActivityId())
              ? "/financial/plans?productId=" + productId
              : productId == null ? null : "/products/" + productId + "/edit";
      return new BackendProductProcessActivityReadiness(
          false,
          ex.getMessage(),
          "Resolver pendência Safira",
          "Corrija a fonte indicada e retome a mesma execução; provas privadas e atividades válidas permanecem preservadas.",
          null,
          null,
          List.of(),
          null,
          url);
    }
  }

  /** Registra uma conclusão determinística idempotente e sem efeitos externos. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    var readiness = readiness(process, activity, product, source);
    require(readiness.ready(), readiness.reason());
    JsonNode snapshot = context.snapshot(source);
    var previous =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), source);
    if (previous.filter(instance -> current(instance, snapshot)).isPresent())
      return completed(source, readiness.reason());
    Instant now = Instant.now();
    var instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(source);
    instance.setOccurrenceNumber(previous.map(value -> value.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setEnteredAt(now);
    instance.setExitedAt(now);
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    com.fasterxml.jackson.databind.node.ObjectNode evidence = snapshot.deepCopy();
    evidence.put("evidenceType", "SAFIRA_COMMERCIAL_PREPARATION_V1");
    evidence.put("activity", activity.getActivityId());
    evidence.put(
        "activityFingerprint",
        SafiraCommercialContext.activityFingerprint(activity.getActivityId(), snapshot));
    instance.setObjectiveEvidenceJson(evidence.toString());
    instances.saveAndFlush(instance);
    return completed(source, readiness.reason());
  }

  /** Recusa aprovação de outra versão e preserva reprovações com correções explícitas. */
  @Override
  @Transactional
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    var scope = context.scope(task.getSourceReference(), null, true);
    JsonNode snapshot = context.snapshot(task.getSourceReference());
    prepared(task.getProcessDefinition(), scope, task.getSourceReference(), snapshot);
    JsonNode proof = context.read(request.evidenceJson()).path("safiraScope");
    require(sameScope(proof, snapshot), "Os ativos ou a versão mudaram; renove a revisão Safira.");
    JsonNode result = context.read(request.resultJson());
    String decision = result.path("decision").asText();
    require(
        Set.of("APPROVED", "ADJUST", "BLOCKED").contains(decision),
        "O parecer Safira não informou decisão válida.");
    if ("APPROVED".equals(decision)) validReview(result);
    else
      require(
          result.path("requiredChanges").isArray() && !result.path("requiredChanges").isEmpty(),
          "O parecer reprovado precisa registrar a correção necessária.");
    return CompletionDisposition.COMPLETE;
  }

  /** Confere que jornada e economia atuais já foram persistidas antes das revisões. */
  public void prepared(
      BusinessProcessDefinition process,
      SafiraCommercialContext.Scope scope,
      String source,
      JsonNode snapshot) {
    checks.all(scope, snapshot);
    for (String step : SafiraCommercialChecks.PREPARATION) {
      var definition =
          definitions.findByProcessDefinitionIdAndActivityId(process.getId(), step).orElseThrow();
      require(
          instances
              .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                  definition.getId(), source)
              .filter(instance -> current(instance, snapshot))
              .isPresent(),
          "Conclua ou renove a atividade " + step + " para os ativos Safira atuais.");
    }
  }

  /** Exige os dez aspectos comerciais em PASS e evidência concreta em cada um. */
  private void validReview(JsonNode result) {
    require("APPROVED".equals(result.path("decision").asText()), "O parecer não aprovou Safira.");
    require(
        result.path("requiredChanges").isArray()
            && result.path("requiredChanges").isEmpty()
            && result.path("evidence").isArray()
            && !result.path("evidence").isEmpty(),
        "A aprovação exige evidências e nenhuma correção pendente.");
    Set<String> found = new HashSet<>();
    for (JsonNode gate : result.path("gateChecks")) {
      require("PASS".equals(gate.path("status").asText()), "O parecer contém gate pendente.");
      require(
          !gate.path("customerEvidence").asText(gate.path("evidence").asText()).isBlank(),
          "O gate Safira não contém evidência.");
      found.add(gate.path("gate").asText());
    }
    require(
        found.containsAll(REVIEW_GATES),
        "O parecer omitiu critérios obrigatórios da oferta Safira.");
  }

  /** Localiza a revisão mais recente e rejeita qualquer aprovação supersedida. */
  public void currentReview(
      BusinessProcessDefinition process, String source, String step, JsonNode snapshot) {
    var task =
        tasks
            .findLatestReviewSnapshots(
                process.getId(), source, step, org.springframework.data.domain.PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Falta o parecer Safira " + step + "."));
    require("COMPLETED".equals(task.status()), "O parecer " + step + " ainda não foi concluído.");
    require(
        sameScope(context.read(task.evidenceJson()).path("safiraScope"), snapshot),
        "O parecer " + step + " ficou desatualizado.");
    validReview(context.read(task.resultJson()));
  }

  /** Resolve somente a definição publicada do subprocesso Safira v2. */
  public BusinessProcessDefinition target() {
    return processes
        .findByProcessCodeAndVersionNumber(SafiraCommercialContext.CODE, 2)
        .filter(process -> "PUBLISHED".equals(process.getStatus()))
        .orElseThrow(
            () -> new IllegalStateException("O subprocesso Safira v2 não está publicado."));
  }

  /** Revalida a conclusão integral antes de permitir reutilização no processo pai. */
  @Transactional(readOnly = true)
  public boolean completed(Product product, String source) {
    BusinessProcessDefinition process = target();
    var ready =
        definitions.findByProcessDefinitionIdAndActivityId(process.getId(), "ready").orElseThrow();
    JsonNode snapshot = context.snapshot(source);
    var scope = context.scope(source, product.getId(), false);
    if (instances
        .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            ready.getId(), source)
        .filter(instance -> current(instance, snapshot))
        .isEmpty()) return false;
    checks.all(scope, snapshot);
    for (String review : REVIEWS) currentReview(process, source, review, snapshot);
    return true;
  }

  /** Confirma que a ocorrência persistida ainda pertence à mesma atividade e fotografia. */
  public boolean current(BusinessProcessActivityInstance instance, JsonNode snapshot) {
    if (!instance.isObjectiveAchieved() || !"COMPLETED".equals(instance.getStatus())) return false;
    JsonNode proof = context.read(instance.getObjectiveEvidenceJson());
    if (!sameIdentity(snapshot.path("productId"), proof.path("productId"))
        || !sameIdentity(snapshot.path("experimentId"), proof.path("experimentId"))
        || !snapshot.path("productVersion").equals(proof.path("productVersion"))) return false;
    String activity = proof.path("activity").asText();
    String expected = SafiraCommercialContext.activityFingerprint(activity, snapshot);
    String recorded = proof.path("activityFingerprint").asText();
    if (recorded.isBlank()) recorded = SafiraCommercialContext.activityFingerprint(activity, proof);
    return !expected.isBlank() && expected.equals(recorded);
  }

  /** Compara a identidade e a impressão completa da candidata comercial. */
  private boolean sameScope(JsonNode proof, JsonNode snapshot) {
    return !snapshot.path("fingerprint").asText().isBlank()
        && snapshot.path("fingerprint").equals(proof.path("fingerprint"))
        && sameIdentity(snapshot.path("productId"), proof.path("productId"))
        && sameIdentity(snapshot.path("experimentId"), proof.path("experimentId"))
        && snapshot.path("productVersion").equals(proof.path("productVersion"));
  }

  /** Aceita nós inteiros equivalentes sem converter texto, fração ou identidade inválida. */
  private boolean sameIdentity(JsonNode expected, JsonNode actual) {
    return expected.isIntegralNumber()
        && actual.isIntegralNumber()
        && expected.canConvertToLong()
        && actual.canConvertToLong()
        && expected.longValue() > 0
        && expected.longValue() == actual.longValue();
  }

  /** Monta o retorno comum das atividades determinísticas concluídas. */
  private BackendProductProcessActivityExecutionResult completed(String source, String message) {
    return new BackendProductProcessActivityExecutionResult(source, "COMPLETED", true, message);
  }
}
