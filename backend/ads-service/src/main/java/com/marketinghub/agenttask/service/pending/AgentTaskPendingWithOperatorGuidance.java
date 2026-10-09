package com.marketinghub.agenttask.service.pending;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.marketinghub.agenttask.AgentTaskPendingResponse;
import java.time.Instant;

/**
 * Responsabilidade: acrescentar a orientação persistida sem mudar o contrato original da tarefa.
 */
public record AgentTaskPendingWithOperatorGuidance(
    @JsonUnwrapped AgentTaskPendingResponse task,
    @JsonInclude(JsonInclude.Include.NON_NULL) OperatorGuidance operatorGuidance) {

  /** Responsabilidade: identificar a fonte das notas do próprio produto entregues ao executor. */
  public record OperatorGuidance(
      String contractVersion,
      Long productId,
      String evidenceReference,
      Instant retrievedAt,
      String commercialNotes) {}
}
