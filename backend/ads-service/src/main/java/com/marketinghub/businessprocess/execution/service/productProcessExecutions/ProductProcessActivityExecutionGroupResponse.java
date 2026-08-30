package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import java.util.List;

/**
 * Responsabilidade: apresentar a situação da atividade e suas tarefas reais no processo do produto.
 */
public record ProductProcessActivityExecutionGroupResponse(
    Long activityDefinitionId,
    String activityId,
    String activityName,
    String activityObjective,
    String activityOwnerName,
    Integer sequenceNumber,
    boolean selectedVersionActivity,
    String operationalState,
    String stateReason,
    boolean objectiveAchieved,
    String stateEvidence,
    Long activityInstanceId,
    Integer occurrenceNumber,
    int taskCount,
    List<BusinessProcessActivityExecutionResponse> tasks,
    boolean executionRequestAvailable,
    String executionRequestReason,
    ProductProcessActivityExecutionControlResponse executionControl) {

  /** Mantém compatibilidade com projeções que ainda não oferecem início pela tela do produto. */
  public ProductProcessActivityExecutionGroupResponse(
      Long activityDefinitionId,
      String activityId,
      String activityName,
      String activityObjective,
      String activityOwnerName,
      Integer sequenceNumber,
      boolean selectedVersionActivity,
      String operationalState,
      String stateReason,
      boolean objectiveAchieved,
      String stateEvidence,
      Long activityInstanceId,
      Integer occurrenceNumber,
      int taskCount,
      List<BusinessProcessActivityExecutionResponse> tasks) {
    this(
        activityDefinitionId,
        activityId,
        activityName,
        activityObjective,
        activityOwnerName,
        sequenceNumber,
        selectedVersionActivity,
        operationalState,
        stateReason,
        objectiveAchieved,
        stateEvidence,
        activityInstanceId,
        occurrenceNumber,
        taskCount,
        tasks,
        false,
        "Esta atividade não possui comando de execução configurado.",
        null);
  }
}
