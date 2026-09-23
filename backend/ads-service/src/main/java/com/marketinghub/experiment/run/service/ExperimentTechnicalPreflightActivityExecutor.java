package com.marketinghub.experiment.run.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: materializar sequencialmente no BPM as provas do preflight técnico vigente. */
@Service
@Slf4j
public class ExperimentTechnicalPreflightActivityExecutor
    implements BackendProductProcessActivityExecutor {
  private final ProductProcessActivityPredecessorService predecessors;
  private final ExperimentTechnicalPreflightEvidenceService evidenceService;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ObjectMapper json;
  private final Clock clock;

  /** Configura ordem, evidências e ledger de atividades do subprocesso. */
  @Autowired
  public ExperimentTechnicalPreflightActivityExecutor(
      ProductProcessActivityPredecessorService predecessors,
      ExperimentTechnicalPreflightEvidenceService evidenceService,
      BusinessProcessActivityInstanceRepository instances,
      ObjectMapper json) {
    this(predecessors, evidenceService, instances, json, Clock.systemUTC());
  }

  /** Permite validar horários e idempotência com relógio controlado. */
  ExperimentTechnicalPreflightActivityExecutor(
      ProductProcessActivityPredecessorService predecessors,
      ExperimentTechnicalPreflightEvidenceService evidenceService,
      BusinessProcessActivityInstanceRepository instances,
      ObjectMapper json,
      Clock clock) {
    this.predecessors = predecessors;
    this.evidenceService = evidenceService;
    this.instances = instances;
    this.json = json;
    this.clock = clock;
  }

  /** Reconhece somente as quatro atividades da homologação técnica canônica. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && ExperimentTechnicalPreflightEvidenceService.PROCESS_CODE.equals(process.getProcessCode())
        && ExperimentTechnicalPreflightEvidenceService.ACTIVITIES.contains(
            activity.getActivityId());
  }

  /** Exige predecessoras concluídas e uma prova vigente antes de liberar o comando. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    var predecessor = predecessors.readiness(process, activity, sourceReference);
    if (!predecessor.ready()) {
      return response(false, predecessor.reason(), false, predecessor.reason(), null);
    }
    try {
      var evidence = evidenceService.evaluate(activity.getActivityId(), product, sourceReference);
      return response(
          true,
          "A evidência do run produtivo vigente pode comprovar esta atividade.",
          true,
          "Run #" + evidence.runId() + " e fontes da atividade estão vigentes.",
          evidence.experimentId());
    } catch (RuntimeException ex) {
      log.warn(
          "Preflight técnico aguarda evidência. productId={} processDefinitionId={} activityId={} sourceReference={}",
          product == null ? null : product.getId(),
          process == null ? null : process.getId(),
          activity == null ? null : activity.getActivityId(),
          sourceReference,
          ex);
      return response(false, ex.getMessage(), false, ex.getMessage(), null);
    }
  }

  /** Registra custo incremental zero sem repetir gates, transação ou tráfego. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    var predecessor = predecessors.readiness(process, activity, sourceReference);
    if (!predecessor.ready()) throw new IllegalStateException(predecessor.reason());
    var evidence = evidenceService.evaluate(activity.getActivityId(), product, sourceReference);
    var latest =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), sourceReference);
    if (latest.filter(value -> sameCompletedEvidence(value, evidence)).isEmpty()) {
      Instant now = Instant.now(clock);
      BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
      instance.setActivityDefinition(activity);
      instance.setSourceReference(sourceReference);
      instance.setOccurrenceNumber(latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1));
      instance.setStatus("COMPLETED");
      instance.setObjectiveAchieved(true);
      instance.setObjectiveEvidenceJson(evidence.objectiveEvidence().toString());
      instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
      instance.setCostCoverage("COMPLETE");
      instance.setEvidenceQuality("REUSED_DIRECT");
      instance.setEnteredAt(now);
      instance.setExitedAt(now);
      instance.setCreatedAt(now);
      instance.setUpdatedAt(now);
      instances.saveAndFlush(instance);
    }
    return new BackendProductProcessActivityExecutionResult(
        sourceReference,
        "COMPLETED",
        true,
        "Objetivo técnico comprovado pelo run produtivo vigente, sem repetir custo ou tráfego.");
  }

  /** Monta a explicação usada pela tela sem duplicar a regra funcional do avaliador. */
  private BackendProductProcessActivityReadiness response(
      boolean ready,
      String reason,
      boolean evidenceReady,
      String evidenceDetail,
      Long experimentId) {
    return new BackendProductProcessActivityReadiness(
        ready,
        reason,
        "Comprovar atividade",
        "Reutiliza o run produtivo vigente e registra somente a evidência desta atividade, com custo incremental zero.",
        experimentId == null ? null : "EXPERIMENT_PREFLIGHT",
        experimentId,
        List.of(
            new ProductProcessActivityRequirementResponse(
                "CURRENT_TECHNICAL_EVIDENCE",
                "Evidência técnica vigente",
                evidenceReady,
                evidenceDetail,
                evidenceReady
                    ? "Preserve o run e as versões comprovadas."
                    : "Conclua ou renove a homologação técnica antes de avançar.")));
  }

  /** Confirma idempotência por estado e impressão dos mesmos insumos verificados. */
  private boolean sameCompletedEvidence(
      BusinessProcessActivityInstance instance,
      ExperimentTechnicalPreflightEvidenceService.Evidence evidence) {
    if (!"COMPLETED".equals(instance.getStatus()) || !instance.isObjectiveAchieved()) return false;
    try {
      var persisted = json.readTree(instance.getObjectiveEvidenceJson());
      return evidence.runId().equals(persisted.path("runId").asLong())
          && evidence.inputFingerprint().equals(persisted.path("inputFingerprint").asText());
    } catch (Exception ex) {
      log.error(
          "Falha lendo evidência do preflight técnico. activityInstanceId={} runId={}",
          instance.getId(),
          evidence.runId(),
          ex);
      return false;
    }
  }
}
