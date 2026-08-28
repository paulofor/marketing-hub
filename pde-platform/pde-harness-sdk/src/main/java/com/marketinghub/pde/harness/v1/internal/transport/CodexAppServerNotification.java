package com.marketinghub.pde.harness.v1.internal.transport;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/** Preserva método e parâmetros de uma notificação recebida do App Server. */
public record CodexAppServerNotification(String method, JsonNode params) {

  /** Congela uma cópia do payload antes de distribuí-lo aos observers. */
  public CodexAppServerNotification {
    method = Objects.requireNonNull(method, "method");
    params = params == null ? null : params.deepCopy();
  }

  /** Devolve uma cópia dos parâmetros para isolar listeners concorrentes. */
  @Override
  public JsonNode params() {
    return params == null ? null : params.deepCopy();
  }
}
