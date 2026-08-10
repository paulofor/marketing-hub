package com.marketinghub.creative.convergence.v1;

import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: expor o relatório funcional e auditável do ciclo de convergência. */
public record CreativeConvergenceReport(
    Long cycleId,
    Long experimentId,
    Long rootCreativeId,
    ConvergenceCycleStatus status,
    Integer iterationCount,
    Integer repeatedIssueCount,
    Integer lastScore,
    Integer bestScore,
    BigDecimal costUsd,
    String stopReason,
    List<Task> tasks) {
  /** Responsabilidade: expor uma pendência, seu aceite e sua evidência funcional. */
  public record Task(
      Long id,
      Long creativeId,
      ConvergenceTaskTarget target,
      ConvergenceTaskStatus status,
      String issueCode,
      String requirement,
      String acceptanceCriterion,
      String evidenceJson) {}
}
