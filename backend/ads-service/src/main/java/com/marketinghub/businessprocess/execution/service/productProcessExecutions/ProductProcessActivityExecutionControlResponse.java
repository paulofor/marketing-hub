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
    List<ProductProcessActivityRequirementResponse> requirements) {

  /** Garante uma coleção imutável mesmo quando o executor não possui requisitos detalhados. */
  public ProductProcessActivityExecutionControlResponse {
    requirements = requirements == null ? List.of() : List.copyOf(requirements);
  }
}
