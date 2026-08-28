package com.marketinghub.pde.harness.v1.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.harness.v1.PdeExecutionObserver;
import com.marketinghub.pde.harness.v1.PdeHarnessEvent;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import com.marketinghub.pde.harness.v1.internal.transport.CodexAppServerNotification;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Correlaciona eventos, saída, uso e término de um turno sem expor decisão de pipeline. */
public final class PdeTurnCollector {
  private static final System.Logger LOGGER = System.getLogger(PdeTurnCollector.class.getName());

  private final String threadId;
  private final PdeExecutionObserver observer;
  private final CopyOnWriteArrayList<PdeHarnessEvent> events = new CopyOnWriteArrayList<>();
  private final StringBuffer output = new StringBuffer();
  private final AtomicReference<String> turnId = new AtomicReference<>();
  private final AtomicReference<JsonNode> tokenUsage = new AtomicReference<>();
  private final CompletableFuture<PdeTurnOutcome> completion = new CompletableFuture<>();

  /** Cria um coletor limitado a uma única thread já confirmada pelo App Server. */
  public PdeTurnCollector(String threadId, PdeExecutionObserver observer) {
    this.threadId = java.util.Objects.requireNonNull(threadId, "threadId");
    this.observer = java.util.Objects.requireNonNull(observer, "observer");
  }

  /** Vincula o identificador retornado por `turn/start` e valida sinais antecipados. */
  public void bindTurn(String confirmedTurnId) {
    if (confirmedTurnId == null || confirmedTurnId.isBlank()) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Resposta turn/start não contém turn.id");
    }
    String observed = turnId.get();
    if (observed != null && !observed.equals(confirmedTurnId)) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Evento de turno diverge da resposta turn/start");
    }
    turnId.set(confirmedTurnId);
  }

  /** Registra o resultado do start/resume como evento auditável do harness. */
  public void recordThreadReady(JsonNode payload) {
    recordEvent("pde/thread/ready", payload, threadId, null, null);
  }

  /** Processa somente notificações que pertencem à thread e ao turno observados. */
  public void accept(CodexAppServerNotification notification) {
    JsonNode params = notification.params();
    String eventThreadId = extractText(params, "threadId", "thread", "id");
    if (eventThreadId != null && !threadId.equals(eventThreadId)) {
      return;
    }
    if (eventThreadId == null) {
      return;
    }

    String eventTurnId = extractText(params, "turnId", "turn", "id");
    String confirmedTurnId = turnId.get();
    if (confirmedTurnId != null && eventTurnId != null && !confirmedTurnId.equals(eventTurnId)) {
      return;
    }
    if (confirmedTurnId == null && eventTurnId != null) {
      turnId.compareAndSet(null, eventTurnId);
    }

    String itemId = extractText(params, "itemId", "item", "id");
    recordEvent(notification.method(), params, eventThreadId, eventTurnId, itemId);
    if ("item/agentMessage/delta".equals(notification.method())) {
      output.append(params.path("delta").asText(""));
    } else if ("item/completed".equals(notification.method())) {
      captureCompletedAgentMessage(params);
    } else if ("thread/tokenUsage/updated".equals(notification.method())) {
      tokenUsage.set(params.path("tokenUsage").deepCopy());
    } else if ("turn/completed".equals(notification.method())) {
      complete(params);
    }
  }

  /** Aguarda o evento terminal respeitando o teto configurado pelo worker. */
  public PdeTurnOutcome await(Duration timeout) {
    try {
      return completion.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Timeout aguardando turn/completed; threadId=" + threadId + ", turnId=" + turnId.get(),
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.TIMEOUT, "Tempo esgotado aguardando a conclusão do turno", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Espera do turno foi interrompida; threadId=" + threadId + ", turnId=" + turnId.get(),
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE, "Espera do turno foi interrompida", ex);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause() == null ? ex : ex.getCause();
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Coleta do turno falhou; threadId=" + threadId + ", turnId=" + turnId.get(),
          cause);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.EXECUTION_FAILED,
          "Falha ao consolidar o turno do App Server",
          cause);
    }
  }

  /** Retorna o identificador conhecido para permitir interrupção segura em timeout. */
  public String turnId() {
    return turnId.get();
  }

  /** Registra o evento e notifica telemetria sem deixar o observer derrubar a execução. */
  private void recordEvent(
      String method, JsonNode payload, String eventThreadId, String eventTurnId, String itemId) {
    PdeHarnessEvent event =
        new PdeHarnessEvent(
            Instant.now(),
            method,
            eventThreadId == null ? threadId : eventThreadId,
            eventTurnId,
            itemId,
            payload);
    events.add(event);
    try {
      observer.onEvent(event);
    } catch (RuntimeException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Observer PDE falhou; method="
              + method
              + ", threadId="
              + threadId
              + ", turnId="
              + eventTurnId,
          ex);
    }
  }

  /** Usa o item final como fallback quando o servidor não enviou deltas incrementais. */
  private void captureCompletedAgentMessage(JsonNode params) {
    JsonNode item = params.path("item");
    if (output.isEmpty()
        && "agentMessage".equals(item.path("type").asText())
        && item.hasNonNull("text")) {
      output.append(item.path("text").asText());
    }
  }

  /** Converte a notificação terminal em um outcome imutável. */
  private void complete(JsonNode params) {
    JsonNode turn = params.path("turn");
    String terminalTurnId = turn.path("id").asText(turnId.get());
    if (terminalTurnId == null || terminalTurnId.isBlank()) {
      completion.completeExceptionally(
          new PdeHarnessException(
              PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
              "turn/completed não contém turn.id"));
      return;
    }
    turnId.set(terminalTurnId);
    String status = turn.path("status").asText("failed");
    String errorMessage = extractError(turn.path("error"));
    completion.complete(
        new PdeTurnOutcome(
            terminalTurnId,
            status,
            output.toString(),
            errorMessage,
            List.copyOf(events),
            tokenUsage.get()));
  }

  /** Extrai mensagem útil de erro sem serializar JSON dentro de outro JSON funcional. */
  private String extractError(JsonNode error) {
    if (error == null || error.isMissingNode() || error.isNull()) {
      return null;
    }
    if (error.isTextual()) {
      return SecretSanitizer.sanitize(error.asText());
    }
    if (error.hasNonNull("message")) {
      return SecretSanitizer.sanitize(error.path("message").asText());
    }
    if (error.hasNonNull("additionalDetails")) {
      return SecretSanitizer.sanitize(error.path("additionalDetails").asText());
    }
    return "Erro estruturado do App Server sem mensagem textual";
  }

  /** Procura primeiro um campo direto e depois um identificador dentro do objeto indicado. */
  private String extractText(
      JsonNode params, String directField, String nestedObject, String nestedField) {
    if (params == null || params.isNull()) {
      return null;
    }
    if (params.hasNonNull(directField)) {
      return params.path(directField).asText();
    }
    JsonNode nested = params.path(nestedObject);
    return nested.hasNonNull(nestedField) ? nested.path(nestedField).asText() : null;
  }
}
