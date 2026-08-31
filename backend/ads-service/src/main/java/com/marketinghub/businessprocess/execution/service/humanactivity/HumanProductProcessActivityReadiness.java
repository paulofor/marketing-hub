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
    List<HumanProductProcessActivityRequirement> requirements,
    String decisionMode,
    String auditEvidenceReference) {

  public static final String DETAILED_DECISION = "DETAILED";
  public static final String REVIEW_AND_ACCEPT = "REVIEW_AND_ACCEPT";

  /** Mantém compatibilidade com decisões que ainda exigem o formulário auditável completo. */
  public HumanProductProcessActivityReadiness(
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
    this(
        ready,
        reason,
        actionLabel,
        description,
        confirmationTitle,
        confirmationMessage,
        confirmationToken,
        workspaceCode,
        workspaceReferenceId,
        requirements,
        DETAILED_DECISION,
        null);
  }

  /** Preserva requisitos imutáveis na projeção enviada ao frontend. */
  public HumanProductProcessActivityReadiness {
    requirements = requirements == null ? List.of() : List.copyOf(requirements);
    decisionMode = REVIEW_AND_ACCEPT.equals(decisionMode) ? REVIEW_AND_ACCEPT : DETAILED_DECISION;
  }

  /** Informa se o backend já possui contexto suficiente para uma confirmação de um clique. */
  public boolean reviewAndAccept() {
    return REVIEW_AND_ACCEPT.equals(decisionMode);
  }
}
