package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import java.util.List;

/** Responsabilidade: apresentar a entrada do ciclo e seus retornos na cadeia selecionada. */
public record LearningCycleEntry(
    Long chainDefinitionId,
    String chainName,
    Long parentProcessDefinitionId,
    String parentProcessName,
    int sequenceNumber,
    String activityId,
    Long processDefinitionId,
    String processName,
    boolean integrated,
    boolean canStartCycle,
    String guidance,
    String workspaceUrl,
    String actionLabel,
    String parentUrl,
    List<ReturnRoute> returnRoutes,
    String activityName,
    Integer activitySequenceNumber) {
  /** Preserva consumidores anteriores sem numeração da atividade chamadora. */
  public LearningCycleEntry(
      Long chainDefinitionId,
      String chainName,
      Long parentProcessDefinitionId,
      String parentProcessName,
      int sequenceNumber,
      String activityId,
      Long processDefinitionId,
      String processName,
      boolean integrated,
      boolean canStartCycle,
      String guidance,
      String workspaceUrl,
      String actionLabel,
      String parentUrl,
      List<ReturnRoute> returnRoutes) {
    this(
        chainDefinitionId,
        chainName,
        parentProcessDefinitionId,
        parentProcessName,
        sequenceNumber,
        activityId,
        processDefinitionId,
        processName,
        integrated,
        canStartCycle,
        guidance,
        workspaceUrl,
        actionLabel,
        parentUrl,
        returnRoutes,
        null,
        null);
  }

  /** Identifica um destino real sem transformar orientação em decisão ou autorização. */
  public record ReturnRoute(
      String label,
      String condition,
      Long processDefinitionId,
      int sequenceNumber,
      String processName,
      String url) {}
}
