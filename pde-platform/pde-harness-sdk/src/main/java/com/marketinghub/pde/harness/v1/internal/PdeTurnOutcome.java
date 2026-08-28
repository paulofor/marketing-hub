package com.marketinghub.pde.harness.v1.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.harness.v1.PdeHarnessEvent;
import java.util.List;
import java.util.Objects;

/** Consolida os sinais terminais observados para um único turno do App Server. */
public record PdeTurnOutcome(
    String turnId,
    String status,
    String output,
    String errorMessage,
    List<PdeHarnessEvent> events,
    JsonNode tokenUsage) {

  /** Congela listas e payloads antes da criação do resultado público. */
  public PdeTurnOutcome {
    turnId = Objects.requireNonNull(turnId, "turnId");
    status = Objects.requireNonNull(status, "status");
    events = List.copyOf(Objects.requireNonNull(events, "events"));
    tokenUsage = tokenUsage == null ? null : tokenUsage.deepCopy();
  }

  /** Devolve uma cópia do uso para preservar o snapshot terminal do turno. */
  @Override
  public JsonNode tokenUsage() {
    return tokenUsage == null ? null : tokenUsage.deepCopy();
  }
}
