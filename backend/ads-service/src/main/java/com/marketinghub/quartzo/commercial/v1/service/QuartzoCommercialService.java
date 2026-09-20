package com.marketinghub.quartzo.commercial.v1.service;

import static com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.*;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
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
 * Responsabilidade: governar a comprovação e os vínculos internos da preparação comercial Quartzo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuartzoCommercialService
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
  private final QuartzoCommercialContext context;
  private final QuartzoCommercialChecks checks;
  private final BusinessProcessActivityInstanceRepository instances;
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final BusinessProcessDefinitionRepository processes;
  private final AgentTaskRepository tasks;
  private final ExperimentRepository experiments;
  private final ProductProcessActivityPredecessorService predecessors;

  /** Reconhece somente as atividades determinísticas do subprocesso especializado. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && QuartzoCommercialContext.CODE.equals(process.getProcessCode())
        && (QuartzoCommercialChecks.PREPARATION.contains(activity.getActivityId())
            || "ready".equals(activity.getActivityId()));
  }

  /** Reconhece os resultados independentes sem capturar callbacks de outro tipo. */
  @Override
  public boolean supports(AgentTask task) {
    return task != null
        && task.getProcessDefinition() != null
        && QuartzoCommercialContext.CODE.equals(task.getProcessDefinition().getProcessCode())
        && REVIEWS.contains(task.getProcessActivityId());
  }

  /** Expõe causa e destino de correção antes de qualquer escrita ou chamada de agente. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    try {
      var scope = context.scope(source, product.getId(), true);
      var prior = predecessors.readiness(process, activity, source);
      require(prior.ready(), prior.reason());
      var snapshot = context.snapshot(source);
      if ("ready".equals(activity.getActivityId())) {
        prepared(process, scope, source, snapshot);
        for (String review : REVIEWS) currentReview(process, source, review, snapshot);
      } else checks.check(activity.getActivityId(), scope, snapshot);
      return new BackendProductProcessActivityReadiness(
          true, "Fontes atuais conferidas; esta atividade não autoriza divulgação nem gasto.");
    } catch (RuntimeException ex) {
      log.warn(
          "Quartzo: preparação bloqueada productId={} source={} activity={}",
          product.getId(),
          source,
          activity.getActivityId(),
          ex);
      String url =
          "economics".equals(activity.getActivityId())
              ? "/financial/plans?productId=" + product.getId()
              : "/products/" + product.getId() + "/edit";
      return new BackendProductProcessActivityReadiness(
          false,
          ex.getMessage(),
          "Conferir preparação",
          "Resolva a pendência na fonte indicada e retome a mesma execução; aprovações anteriores permanecem auditáveis.",
          null,
          null,
          List.of(),
          null,
          url);
    }
  }

  /**
   * Registra a conclusão idempotente e configura apenas URLs provenientes da publicação auditada.
   */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    var check = readiness(process, activity, product, source);
    require(check.ready(), check.reason());
    var scope = context.scope(source, product.getId(), true);
    var snapshot = context.snapshot(source);
    if ("entry".equals(activity.getActivityId()))
      scope.experiment().setFollowUpActionUrl(snapshot.path("destinationUrl").asText());
    if ("checkout".equals(activity.getActivityId()))
      scope.experiment().setCommercialCheckoutUrl(snapshot.path("checkoutUrl").asText());
    if (Set.of("entry", "checkout").contains(activity.getActivityId()))
      experiments.saveAndFlush(scope.experiment());
    var previous =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), source);
    if (previous.filter(i -> current(i, snapshot)).isPresent())
      return new BackendProductProcessActivityExecutionResult(
          source, "COMPLETED", true, check.reason());
    var instance = new BusinessProcessActivityInstance();
    Instant now = Instant.now();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(source);
    instance.setOccurrenceNumber(previous.map(i -> i.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setEnteredAt(now);
    instance.setExitedAt(now);
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    instance.setKnownCostUsd(BigDecimal.ZERO);
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    var evidence = snapshot.deepCopy();
    evidence.put("evidenceType", "QUARTZO_COMMERCIAL_PREPARATION_V1");
    evidence.put("activity", activity.getActivityId());
    evidence.put(
        "activityFingerprint",
        QuartzoCommercialContext.activityFingerprint(activity.getActivityId(), snapshot));
    instance.setObjectiveEvidenceJson(evidence.toString());
    instances.saveAndFlush(instance);
    return new BackendProductProcessActivityExecutionResult(
        source, "COMPLETED", true, check.reason());
  }

  /**
   * Preserva reprovações funcionais e recusa aprovações incompletas ou de ativos desatualizados.
   */
  @Override
  @Transactional
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    var scope = context.scope(task.getSourceReference(), null, true);
    var snapshot = context.snapshot(task.getSourceReference());
    prepared(task.getProcessDefinition(), scope, task.getSourceReference(), snapshot);
    var proof = context.read(request.evidenceJson()).path("quartzoScope");
    require(sameScope(proof, snapshot), "Os ativos ou a versão mudaram; renove a revisão Quartzo.");
    var result = context.read(request.resultJson());
    String decision = result.path("decision").asText();
    require(
        Set.of("APPROVED", "ADJUST", "BLOCKED").contains(decision),
        "O parecer não informou uma decisão válida.");
    if ("APPROVED".equals(decision)) validReview(result);
    else
      require(
          result.path("requiredChanges").isArray() && !result.path("requiredChanges").isEmpty(),
          "O parecer reprovado precisa registrar a correção necessária.");
    return CompletionDisposition.COMPLETE;
  }

  /** Confirma fontes e comprovantes sem reservar registros durante a consulta das revisões. */
  public void prepared(
      BusinessProcessDefinition process,
      QuartzoCommercialContext.Scope scope,
      String source,
      JsonNode snapshot) {
    checks.all(scope, snapshot);
    for (String step : QuartzoCommercialChecks.PREPARATION) {
      var definition =
          definitions.findByProcessDefinitionIdAndActivityId(process.getId(), step).orElseThrow();
      require(
          instances
              .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                  definition.getId(), source)
              .filter(i -> current(i, snapshot))
              .isPresent(),
          "Conclua ou renove a atividade " + step + " para os ativos atuais.");
    }
  }

  /**
   * Exige todos os aspectos comerciais em PASS; texto de aprovação sozinho não comprova entrega.
   */
  private void validReview(JsonNode result) {
    require(
        "APPROVED".equals(result.path("decision").asText()),
        "O parecer não aprovou a preparação Quartzo.");
    require(
        result.path("requiredChanges").isArray()
            && result.path("requiredChanges").isEmpty()
            && result.path("evidence").isArray()
            && !result.path("evidence").isEmpty(),
        "A aprovação exige evidências e nenhuma correção pendente.");
    var found = new HashSet<String>();
    for (var gate : result.path("gateChecks")) {
      require(
          "PASS".equals(gate.path("status").asText()),
          "O parecer contém um gate comercial pendente.");
      require(
          !gate.path("customerEvidence").asText(gate.path("evidence").asText()).isBlank(),
          "O gate não contém evidência.");
      found.add(gate.path("gate").asText());
    }
    require(
        found.containsAll(REVIEW_GATES),
        "O parecer omitiu critérios obrigatórios da entrega Quartzo.");
  }

  /** Localiza a última tarefa da revisão exata e rejeita qualquer aprovação supersedida. */
  public void currentReview(
      BusinessProcessDefinition process, String source, String step, JsonNode snapshot) {
    var task =
        tasks
            .findLatestReviewSnapshots(
                process.getId(), source, step, org.springframework.data.domain.PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Falta o parecer " + step + "."));
    require("COMPLETED".equals(task.status()), "O parecer " + step + " ainda não foi concluído.");
    require(
        sameScope(context.read(task.evidenceJson()).path("quartzoScope"), snapshot),
        "O parecer " + step + " ficou desatualizado.");
    validReview(context.read(task.resultJson()));
  }

  /** Resolve somente a definição Quartzo v1 publicada, preservando a versão do contrato. */
  public BusinessProcessDefinition target() {
    return processes
        .findByProcessCodeAndVersionNumber(QuartzoCommercialContext.CODE, 1)
        .filter(p -> "PUBLISHED".equals(p.getStatus()))
        .orElseThrow(
            () -> new IllegalStateException("O subprocesso Quartzo v1 não está publicado."));
  }

  /** Revalida a conclusão do filho em consulta sem lock antes de reutilizar pareceres no pai. */
  @Transactional(readOnly = true)
  public boolean completed(Product product, String source) {
    var process = target();
    var ready =
        definitions.findByProcessDefinitionIdAndActivityId(process.getId(), "ready").orElseThrow();
    var snapshot = context.snapshot(source);
    context.scope(source, product.getId(), false);
    if (instances
        .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            ready.getId(), source)
        .filter(i -> current(i, snapshot))
        .isEmpty()) return false;
    checks.all(context.scope(source, product.getId(), false), snapshot);
    for (String review : REVIEWS) currentReview(process, source, review, snapshot);
    return true;
  }

  /**
   * Informa se a prova pertence à identidade e às fontes específicas ainda vigentes da atividade.
   */
  public boolean current(BusinessProcessActivityInstance instance, JsonNode snapshot) {
    if (!instance.isObjectiveAchieved() || !"COMPLETED".equals(instance.getStatus())) return false;
    var proof = context.read(instance.getObjectiveEvidenceJson());
    if (!sameIdentity(snapshot.path("productId"), proof.path("productId"))
        || !sameIdentity(snapshot.path("experimentId"), proof.path("experimentId"))
        || !snapshot.path("productVersion").equals(proof.path("productVersion"))) return false;
    String activity = proof.path("activity").asText();
    if (activity.isBlank()) return sameScope(proof, snapshot);
    String expected = QuartzoCommercialContext.activityFingerprint(activity, snapshot);
    String recorded = proof.path("activityFingerprint").asText();
    if (recorded.isBlank())
      recorded = QuartzoCommercialContext.activityFingerprint(activity, proof);
    return !expected.isBlank() && expected.equals(recorded);
  }

  /** Compara identidade pelo valor inteiro persistido e exige a mesma impressão dos insumos. */
  private boolean sameScope(JsonNode proof, JsonNode snapshot) {
    return !snapshot.path("fingerprint").asText().isBlank()
        && snapshot.path("fingerprint").equals(proof.path("fingerprint"))
        && sameIdentity(snapshot.path("productId"), proof.path("productId"))
        && sameIdentity(snapshot.path("experimentId"), proof.path("experimentId"))
        && snapshot.path("productVersion").equals(proof.path("productVersion"));
  }

  /** Aceita IntNode e LongNode equivalentes sem converter texto, fração ou identidade inválida. */
  private boolean sameIdentity(JsonNode expected, JsonNode actual) {
    return expected.isIntegralNumber()
        && actual.isIntegralNumber()
        && expected.canConvertToLong()
        && actual.canConvertToLong()
        && expected.longValue() > 0
        && expected.longValue() == actual.longValue();
  }
}
