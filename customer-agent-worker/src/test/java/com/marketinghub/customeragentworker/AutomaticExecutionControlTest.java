package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar PLAY, STOP e falha fechada no gate usado pelos executores Java. */
class AutomaticExecutionControlTest {
  private HttpServer server;

  /** Libera a porta local depois de cada cenário. */
  @AfterEach
  void stopServer() {
    if (server != null) server.stop(0);
  }

  /** Permite a rotina quando o backend comprova PLAY. */
  @Test
  void shouldAllowAutomaticExecutionOnPlay() throws Exception {
    AutomaticExecutionControl control = control(200, "{\"automaticExecutionEnabled\":true}");

    assertThat(control.allowsAutomaticExecution()).isTrue();
  }

  /** Bloqueia a rotina quando o backend informa STOP. */
  @Test
  void shouldBlockAutomaticExecutionOnStop() throws Exception {
    AutomaticExecutionControl control = control(200, "{\"automaticExecutionEnabled\":false}");

    assertThat(control.allowsAutomaticExecution()).isFalse();
  }

  /** Falha fechado quando o estado não pode ser comprovado. */
  @Test
  void shouldFailClosedWhenBackendIsUnavailable() {
    AutomaticExecutionControl control = new AutomaticExecutionControl("http://127.0.0.1:1");

    assertThat(control.allowsAutomaticExecution()).isFalse();
  }

  /** Sobe um backend local mínimo com a resposta operacional solicitada pelo cenário. */
  private AutomaticExecutionControl control(int status, String body) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agents/executor-health/customer-agent/automatic-execution",
        exchange -> {
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    return new AutomaticExecutionControl("http://127.0.0.1:" + server.getAddress().getPort());
  }
}
