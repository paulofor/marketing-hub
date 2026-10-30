package com.marketinghub.agenttask.service.pending;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.marketinghub.agenttask.AgentTaskPendingResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview;
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
      String commercialNotes,
      @JsonInclude(JsonInclude.Include.NON_NULL) PreparationBudgetContext preparationBudget) {}

  /** Responsabilidade: transportar o recibo nativo vigente sem serializar JSON dentro de texto. */
  public record PreparationBudgetContext(
      String contractVersion,
      Long productId,
      Long cycleId,
      Long experimentId,
      String currency,
      boolean includesPreviousCosts,
      boolean authorizesMediaOrAudiovisual,
      String financialReviewUrl,
      CycleFinancialReview.Authorization authorization) {}
}
