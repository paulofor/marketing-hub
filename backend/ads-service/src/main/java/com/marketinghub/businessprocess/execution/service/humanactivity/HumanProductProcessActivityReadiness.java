package com.marketinghub.businessprocess.execution.service.humanactivity;

import java.util.List;

/** Responsabilidade: explicar a prontidão e a confirmação exigida por uma decisão humana. */
public record HumanProductProcessActivityReadiness(
    boolean ready,
    String reason,
    String actionLabel,
    String description,
    String confirmationTitle,
    String confirmationMessage,
    String confirmationToken,
    String workspaceCode,
    Long workspaceReferenceId,
    List<HumanProductProcessActivityRequirement> requirements) {

  /** Preserva requisitos imutáveis na projeção enviada ao frontend. */
  public HumanProductProcessActivityReadiness {
    requirements = requirements == null ? List.of() : List.copyOf(requirements);
  }
}
