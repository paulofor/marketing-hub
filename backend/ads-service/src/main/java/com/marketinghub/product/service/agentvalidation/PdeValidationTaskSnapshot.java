package com.marketinghub.product.service.agentvalidation;

/** Responsabilidade: transportar somente os dados persistidos necessários ao gate de retrabalho. */
public record PdeValidationTaskSnapshot(
    Long id,
    Long processDefinitionId,
    String processActivityId,
    String status,
    String blockerCategory,
    String blockerAction,
    String resultJson,
    String executionError,
    java.time.Instant updatedAt) {
  /** Preserva consumidores históricos que não consultam a revisão temporal da prova. */
  public PdeValidationTaskSnapshot(
      Long id,
      Long processDefinitionId,
      String processActivityId,
      String status,
      String blockerCategory,
      String blockerAction,
      String resultJson,
      String executionError) {
    this(
        id,
        processDefinitionId,
        processActivityId,
        status,
        blockerCategory,
        blockerAction,
        resultJson,
        executionError,
        null);
  }

  /**
   * Cria somente a visão transitória exigida pela política, sem consultar nem persistir entidade.
   */
  com.marketinghub.agenttask.AgentTask reviewProof(String reference) {
    var task = new com.marketinghub.agenttask.AgentTask();
    task.setId(id);
    task.setSourceReference(reference);
    task.setProcessActivityId(processActivityId);
    task.setStatus(status);
    task.setBlockerCategory(blockerCategory);
    task.setResultJson(resultJson);
    task.setUpdatedAt(updatedAt);
    return task;
  }
}
