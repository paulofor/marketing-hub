package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import java.util.List;

/** Responsabilidade: apresentar a passagem atual e sua memória sem confundir ciclo e versão BPM. */
public record LearningCycleProcessContext(
    Long cycleId,
    Integer cycleNumber,
    Long experimentId,
    Long chainDefinitionId,
    String productVersion,
    String status,
    String stageLabel,
    String hypothesis,
    String mainChange,
    String cycleUrl,
    List<Learning> previousLearning,
    Work nextWork) {
  /** Responsabilidade: identificar uma evidência histórica e sua interpretação preservada. */
  public record Learning(
      Long cycleId,
      Long experimentId,
      String action,
      String summary,
      String evidenceReference,
      String learning,
      String nextHypothesis,
      String limitation) {}

  /** Responsabilidade: indicar a próxima atividade calculada pelas regras canônicas do backend. */
  public record Work(
      Long processDefinitionId,
      Integer processNumber,
      String processName,
      String activityId,
      Integer activityNumber,
      String activityName,
      String responsible,
      String state,
      String reason,
      String url) {}
}
