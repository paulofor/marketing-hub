package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a auditoria dos callbacks devolvidos por Têmis ao backend. */
class MetaAdApproverBackendClientTest {

  /**
   * Preserva interação bruta quando o callback funcional falha, sem reaplicar a decisão recusada.
   */
  @Test
  void preservesRawAuditWhenReportingCallbackFailure() throws Exception {
    AtomicReference<String> body = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/creatives/529/agent-review/result",
        exchange -> {
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();
    try {
      MetaAdApproverProperties properties = new MetaAdApproverProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      MetaAdApproverBackendClient client = new MetaAdApproverBackendClient(properties);
      Map<String, Object> rejectedCallback =
          Map.of(
              "decision", "ADJUST",
              "model", "gpt-test",
              "requestJson", "{\"prompt\":\"auditado\"}",
              "responseJson", "{\"decision\":\"ADJUST\"}",
              "correctionTargets", java.util.List.of(Map.of("target", "LANDING")));

      client.fail(
          529L, rejectedCallback, new IllegalStateException("callback rejeitado pelo backend"));

      var payload = new ObjectMapper().readTree(body.get());
      assertThat(payload.path("decision").asText()).isEqualTo("FAILED");
      assertThat(payload.path("error").asText()).isEqualTo("callback rejeitado pelo backend");
      assertThat(payload.path("model").asText()).isEqualTo("gpt-test");
      assertThat(payload.path("requestJson").asText()).contains("auditado");
      assertThat(payload.path("responseJson").asText()).contains("ADJUST");
      assertThat(payload.has("correctionTargets")).isFalse();
    } finally {
      server.stop(0);
    }
  }
}
