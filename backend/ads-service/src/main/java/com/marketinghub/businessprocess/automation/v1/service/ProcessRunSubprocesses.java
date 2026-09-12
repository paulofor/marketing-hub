package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: registrar no pai o resultado funcional comprovado de sua chamada ao filho. */
@Component
@RequiredArgsConstructor
public class ProcessRunSubprocesses {
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ObjectMapper json;

  /** Confere identidade, objetivo e chamada antes de materializar uma conclusão idempotente. */
  public void complete(
      ProcessRun parent,
      ProductProcessActivityExecutionGroupResponse activity,
      ProcessRun child,
      ProductProcessActivityExecutionHistoryResponse proof) {
    if (!Objects.equals(parent.getProductId(), child.getProductId())
        || !Objects.equals(parent.getChainDefinitionId(), child.getChainDefinitionId())
        || !Objects.equals(parent.getLearningCycleId(), child.getLearningCycleId())
        || !Objects.equals(parent.getSourceReference(), child.getSourceReference())
        || !Objects.equals(parent.getId(), child.getParentRunId())
        || !Objects.equals(child.getProductId(), proof.productId())
        || !Objects.equals(child.getProcessDefinitionId(), proof.selectedProcessDefinitionId())
        || !Objects.equals(child.getSourceReference(), proof.currentExecutionReference())
        || !"COMPLETED".equals(child.getStatus())
        || !proof.objectiveAchieved()
        || proof.remainingActivityCount() != 0
        || proof.completedActivityCount() == 0
        || proof.activities().stream()
            .noneMatch(a -> a.selectedVersionActivity() && a.objectiveAchieved())
        || proof.activities().stream()
            .anyMatch(
                a -> java.util.Set.of("PENDING", "IN_PROGRESS").contains(a.operationalState())))
      throw new IllegalStateException(
          "O subprocesso não comprovou a conclusão no mesmo contexto do pai.");
    var definition =
        definitions
            .findByProcessDefinitionIdAndActivityId(
                parent.getProcessDefinitionId(), activity.activityId())
            .orElseThrow();
    if (!Objects.equals(definition.getSubprocessCode(), proof.processCode()))
      throw new IllegalStateException(
          "O resultado não pertence ao subprocesso chamado pela atividade.");
    var latest =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            definition.getId(), parent.getSourceReference());
    if (latest
        .filter(i -> "COMPLETED".equals(i.getStatus()) && i.isObjectiveAchieved())
        .isPresent()) return;
    Instant now = Instant.now();
    var instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(definition);
    instance.setSourceReference(parent.getSourceReference());
    instance.setOccurrenceNumber(latest.map(i -> i.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setCreatedAt(now);
    instance.setEnteredAt(child.getCreatedAt());
    instance.setExitedAt(now);
    instance.setUpdatedAt(now);
    instance.setKnownCostUsd(BigDecimal.ZERO);
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    var evidence = json.createObjectNode();
    evidence.put("evidenceType", "SUBPROCESS_OBJECTIVE_ACHIEVED_V1");
    evidence.put("parentRunId", parent.getId());
    evidence.put("childRunId", child.getId());
    evidence.put("childProcessDefinitionId", child.getProcessDefinitionId());
    evidence.put("sourceReference", child.getSourceReference());
    evidence.put("completedActivities", proof.completedActivityCount());
    evidence.put("remainingActivities", proof.remainingActivityCount());
    evidence.put("costRecordedInChild", true);
    var activities = evidence.putArray("activityEvidence");
    proof.activities().stream()
        .filter(a -> a.selectedVersionActivity())
        .forEach(
            a -> {
              var item = activities.addObject();
              item.put("activityId", a.activityId());
              item.put("instanceId", a.activityInstanceId());
              item.put("status", a.operationalState());
              item.put("objectiveAchieved", a.objectiveAchieved());
              var taskIds = item.putArray("taskIds");
              a.tasks().forEach(t -> taskIds.add(t.taskId()));
            });
    instance.setObjectiveEvidenceJson(evidence.toString());
    instances.saveAndFlush(instance);
  }
}
