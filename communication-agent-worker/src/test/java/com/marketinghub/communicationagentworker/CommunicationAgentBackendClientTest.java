package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o handshake da renderização sem alterar as demais filas de Íris. */
class CommunicationAgentBackendClientTest {
  /** Expõe suporte ao estado novo apenas à atividade capaz de renderizar e persistir os pixels. */
  @Test
  void declaresRenderPlanContractOnlyOnNonAudiovisualQueue() throws Exception {
    var requests = new ArrayList<String>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requests.add(exchange.getRequestURI().toString());
          byte[] response = "[]".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var properties = new CommunicationAgentProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      var client = new CommunicationAgentBackendClient(properties);
      assertThat(client.claim("creative-production-approval", "nonAudiovisual")).isNull();
      assertThat(client.claim("pde-communication-sales-journey", "communicationContract")).isNull();
      assertThat(client.claim("landing-page-generation", "html")).isNull();
      assertThat(requests).hasSize(3);
      assertThat(requests.getFirst()).contains("workerContract=IRIS_RENDER_PLAN_V1");
      assertThat(requests.get(1)).doesNotContain("workerContract=");
      assertThat(requests.get(2)).doesNotContain("workerContract=");
    } finally {
      server.stop(0);
    }
  }
}
