package com.marketinghub.mcpserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Valida a consulta MCP ao health real do Operador de Crescimento. */
class GrowthOperatorWorkerHealthServiceTest {
  private HttpServer server;

  /** Encerra o servidor HTTP temporário após cada teste. */
  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  /** Garante que o payload do health seja preservado de forma estruturada. */
  @Test
  void shouldReadGrowthOperatorWorkerHealth() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/health",
        exchange -> {
          byte[] body = "{\"status\":\"UP\"}".getBytes();
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/health";
    GrowthOperatorWorkerHealthService service =
        new GrowthOperatorWorkerHealthService(url, new ObjectMapper());

    Map<String, Object> result = service.readHealth();

    assertEquals("growth-operator-worker", result.get("module"));
    assertEquals(true, result.get("reachable"));
    assertEquals("UP", ((Map<?, ?>) result.get("payload")).get("status"));
  }
}
