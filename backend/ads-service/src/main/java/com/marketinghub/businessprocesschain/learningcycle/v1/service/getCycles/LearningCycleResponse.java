package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Responsabilidade: expor a verdade persistida do ciclo e as ações liberadas pelo backend. */
public record LearningCycleResponse(
    Long id,
    Long productId,
    Long experimentId,
    Long previousCycleId,
    Long successorCycleId,
    Long chainDefinitionId,
    Long processDefinitionId,
    long revision,
    String stage,
    String stageLabel,
    String status,
    boolean baseline,
    String productVersion,
    BigDecimal budgetLimitBrl,
    Instant windowStart,
    Instant windowEnd,
    String nextAction,
    String responsible,
    Long returnProcessId,
    String returnActivityId,
    String workUrl,
    JsonNode diagram,
    JsonNode brief,
    JsonNode inheritedLearning,
    List<Event> events,
    List<ApprovalOption> approvalOptions,
    Map<String, List<ApprovalOption>> evidenceOptions,
    List<WorkLink> workLinks,
    List<CommandOption> commands,
    boolean canCreateSuccessor,
    Instant createdAt,
    Instant closedAt) {
  /** Orienta execução por telas oficiais sem gerar consumo ao navegar. */
  public record WorkLink(String label, String url) {}

  /** Identifica uma aprovação real elegível para a mesma versão do ciclo. */
  public record ApprovalOption(Long id, String label) {}

  /** Apresenta um movimento permitido com sua orientação e disponibilidade atuais. */
  public record CommandOption(String action, String label, boolean available, String reason) {}

  /** Apresenta uma transição histórica com entrada, saída e fonte estruturadas. */
  public record Event(
      Long id,
      long revision,
      String fromStage,
      String toStage,
      String action,
      String operatorName,
      String summary,
      String evidenceReference,
      JsonNode evidence,
      Instant createdAt) {}
}
