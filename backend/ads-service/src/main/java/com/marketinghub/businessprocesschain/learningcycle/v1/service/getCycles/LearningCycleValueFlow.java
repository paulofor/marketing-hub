package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Capability;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Responsabilidade: apresentar andamento, dependência, decisões e prova comercial no contexto
 * exato.
 */
public record LearningCycleValueFlow(
    String situation,
    String saleBlocker,
    String resolvingResponsible,
    String awaitingResponsible,
    boolean activeExecution,
    Boolean decisionNeeded,
    String decisionReason,
    String acceptance,
    Instant stalledSince,
    Long stalledSeconds,
    Instant observedAt,
    Capability implementation,
    boolean prototypeRegistered,
    int readyPackages,
    List<Deliverable> deliverables,
    List<Scenario> costScenarios,
    BigDecimal contributionTargetPercent,
    JsonNode marketMeasurement,
    String measurementSource) {
  /** Separa conclusão de contrato, arquivo utilizável e revisão independente. */
  public record Deliverable(
      Long taskId,
      String activity,
      String responsible,
      String status,
      String usableOutput,
      Instant deliveredAt) {}

  /** Apresenta premissas de Plutus sem chamar projeção de lucro ou receita realizada. */
  public record Scenario(
      String name,
      BigDecimal priceBrl,
      BigDecimal variableCostBrl,
      BigDecimal remainderBeforeAcquisitionBrl,
      BigDecimal percentBeforeAcquisition,
      String assumptions,
      Long sourceTaskId) {}
}
