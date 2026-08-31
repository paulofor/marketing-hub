package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import java.util.List;

/**
 * Responsabilidade: descrever como a tela deve orientar e acionar uma atividade sem inferir regras.
 */
public record ProductProcessActivityExecutionControlResponse(
    String executorType,
    String interactionType,
    String actionLabel,
    String description,
    boolean actionAvailable,
    String availabilityReason,
    boolean confirmationRequired,
    String confirmationTitle,
    String confirmationMessage,
    String confirmationToken,
    String workspaceCode,
    Long workspaceReferenceId,
    Long targetProcessDefinitionId,
    List<ProductProcessActivityRequirementResponse> requirements,
    String decisionMode,
    String auditEvidenceReference) {

  /** Mantém o contrato anterior para controles que não representam uma decisão simplificada. */
  public ProductProcessActivityExecutionControlResponse(
      String executorType,
      String interactionType,
      String actionLabel,
      String description,
      boolean actionAvailable,
      String availabilityReason,
      boolean confirmationRequired,
      String confirmationTitle,
      String confirmationMessage,
      String confirmationToken,
      String workspaceCode,
      Long workspaceReferenceId,
      Long targetProcessDefinitionId,
      List<ProductProcessActivityRequirementResponse> requirements) {
    this(
        executorType,
        interactionType,
        actionLabel,
        description,
        actionAvailable,
        availabilityReason,
        confirmationRequired,
        confirmationTitle,
        confirmationMessage,
        confirmationToken,
        workspaceCode,
        workspaceReferenceId,
        targetProcessDefinitionId,
        requirements,
        "DETAILED",
        null);
  }

  /** Garante uma coleção imutável mesmo quando o executor não possui requisitos detalhados. */
  public ProductProcessActivityExecutionControlResponse {
    requirements = requirements == null ? List.of() : List.copyOf(requirements);
  }
}
