package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/** Responsabilidade: validar os limites HTTP que impedem o travamento operacional de Têmis. */
class BackendRestClientFactoryTest {

  /** Interrompe a leitura quando o backend aceita a conexão, mas não responde no prazo. */
  @Test
  void timesOutUnresponsiveBackend() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/slow",
        exchange -> {
          try {
            Thread.sleep(500);
            exchange.sendResponseHeaders(204, -1);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    try {
      MetaAdApproverProperties properties = new MetaAdApproverProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      properties.setBackendConnectTimeout(Duration.ofMillis(100));
      properties.setBackendReadTimeout(Duration.ofMillis(100));

      assertThatThrownBy(
              () ->
                  BackendRestClientFactory.create(properties)
                      .get()
                      .uri("/slow")
                      .retrieve()
                      .toBodilessEntity())
          .isInstanceOf(ResourceAccessException.class);
    } finally {
      server.stop(0);
    }
  }

  /** Permite que o Estúdio use um limite maior sem afrouxar os demais callbacks. */
  @Test
  void acceptsDedicatedReadTimeoutForLargeArtifacts() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/artifact",
        exchange -> {
          try {
            Thread.sleep(150);
            exchange.sendResponseHeaders(204, -1);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    try {
      MetaAdApproverProperties properties = new MetaAdApproverProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      properties.setBackendConnectTimeout(Duration.ofMillis(100));
      properties.setBackendReadTimeout(Duration.ofMillis(50));

      BackendRestClientFactory.create(properties, Duration.ofSeconds(1))
          .get()
          .uri("/artifact")
          .retrieve()
          .toBodilessEntity();
    } finally {
      server.stop(0);
    }
  }
}
