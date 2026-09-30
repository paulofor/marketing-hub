package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que uma chamada lenta ao backend congele os schedulers de Dédalo. */
class LandingGeneratorBackendRestClientFactoryTest {

  /** Encerra a leitura presa dentro do prazo e permite que a próxima rodada seja executada. */
  @Test
  void shouldBoundBackendReadAndReleasePollingThread() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch requestArrived = new CountDownLatch(1);
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/pending",
        exchange -> {
          requestArrived.countDown();
          try {
            release.await(2, TimeUnit.SECONDS);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    try {
      var client =
          LandingGeneratorBackendRestClientFactory.create(
              "http://127.0.0.1:" + server.getAddress().getPort(),
              Duration.ofMillis(100),
              Duration.ofMillis(150));
      long startedAt = System.nanoTime();

      assertThatThrownBy(() -> client.get().uri("/pending").retrieve().toBodilessEntity())
          .isInstanceOf(RuntimeException.class);

      assertThat(requestArrived.await(100, TimeUnit.MILLISECONDS)).isTrue();
      assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(1));
    } finally {
      release.countDown();
      server.stop(0);
    }
  }
}
