package com.marketinghub.pde.harness.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Objects;

/** Representa um evento auditável do App Server correlacionado à execução PDE. */
public record PdeHarnessEvent(
    Instant occurredAt,
    String method,
    String threadId,
    String turnId,
    String itemId,
    JsonNode payload) {

  /** Congela o instante, o método e uma cópia independente do payload observado. */
  public PdeHarnessEvent {
    occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    method = Objects.requireNonNull(method, "method");
    payload = payload == null ? null : payload.deepCopy();
  }

  /** Devolve uma cópia do payload para manter a trilha observada imutável. */
  @Override
  public JsonNode payload() {
    return payload == null ? null : payload.deepCopy();
  }
}
