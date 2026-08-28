package com.marketinghub.pde.harness.v1.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.PdeHarnessEvent;
import com.marketinghub.pde.harness.v1.internal.transport.CodexAppServerNotification;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/** Protege a correlação de eventos para que uma execução nunca absorva sinais de outra. */
class PdeTurnCollectorTest {
  private final ObjectMapper mapper = new ObjectMapper();

  /** Ignora erro sem thread em vez de replicá-lo para todos os clientes ativos. */
  @Test
  void ignoresUncorrelatedGlobalError() {
    List<PdeHarnessEvent> observed = new CopyOnWriteArrayList<>();
    PdeTurnCollector collector = new PdeTurnCollector("thread-cliente-a", observed::add);
    ObjectNode params = mapper.createObjectNode();
    params.putObject("error").put("message", "erro sem correlação");

    collector.accept(new CodexAppServerNotification("error", params));

    assertTrue(observed.isEmpty());
  }

  /** Aceita evento somente quando o identificador pertence à thread observada. */
  @Test
  void acceptsOnlyMatchingThreadEvent() {
    List<PdeHarnessEvent> observed = new CopyOnWriteArrayList<>();
    PdeTurnCollector collector = new PdeTurnCollector("thread-cliente-a", observed::add);
    collector.accept(notification("thread-cliente-b"));
    collector.accept(notification("thread-cliente-a"));

    assertEquals(1, observed.size());
    assertEquals("thread-cliente-a", observed.getFirst().threadId());
  }

  /** Monta uma notificação sintética com correlação explícita. */
  private CodexAppServerNotification notification(String threadId) {
    ObjectNode params = mapper.createObjectNode();
    params.put("threadId", threadId);
    params.put("turnId", "turn-1");
    params.put("itemId", "item-1");
    params.put("delta", "trecho");
    return new CodexAppServerNotification("item/agentMessage/delta", params);
  }
}
