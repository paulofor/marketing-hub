package com.marketinghub.businessprocesschain.learningcycle.v1.service.getSalesFlow;

import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor a posição e as passagens comprovadas do produto no Processo 6. */
public record SalesFlowResponse(
    Long productId,
    Long experimentId,
    Long cycleId,
    Long cycleProcessDefinitionId,
    Long chainDefinitionId,
    Long modelProcessDefinitionId,
    String currentActivityId,
    String currentActivityName,
    Integer currentActivitySequenceNumber,
    String state,
    String reason,
    String navigationUrl,
    String modelUrl,
    List<Activity> activities,
    List<Transition> transitions) {

  /** Responsabilidade: descrever uma atividade sem confundir dispensa com objetivo atingido. */
  public record Activity(
      String activityId,
      int sequenceNumber,
      String name,
      String state,
      boolean objectiveAchieved,
      String reason,
      String evidenceReference,
      Instant enteredAt,
      Instant exitedAt) {}

  /** Responsabilidade: relacionar um evento imutável à sua passagem ou retorno no BPM. */
  public record Transition(
      Long eventId,
      long revision,
      String action,
      String flowId,
      String from,
      String to,
      boolean returnFlow,
      String reason,
      String responsible,
      String evidenceReference,
      Instant occurredAt) {}
}
