package com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution;

import java.util.Map;

/** Responsabilidade: transportar a decisão explícita usada para executar uma atividade humana. */
public record ProductProcessActivityExecutionRequest(
    String decision,
    String operatorName,
    String justification,
    String evidenceReference,
    String confirmationToken,
    Map<String, Object> structuredEvidence) {

  /** Mantém compatibilidade com decisões humanas que não possuem formulário especializado. */
  public ProductProcessActivityExecutionRequest(
      String decision,
      String operatorName,
      String justification,
      String evidenceReference,
      String confirmationToken) {
    this(decision, operatorName, justification, evidenceReference, confirmationToken, Map.of());
  }

  /** Preserva a evidência recebida como snapshot imutável. */
  public ProductProcessActivityExecutionRequest {
    structuredEvidence = structuredEvidence == null ? Map.of() : Map.copyOf(structuredEvidence);
  }
}
