package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Responsabilidade: proteger a rotina de reconexão de Íris contra chamadas HTTP bloqueadas. */
class CodexAuthReconnectSchedulerTest {
  private HttpServer server;
  private ExecutorService serverExecutor;
  private CountDownLatch releaseResponse;

  /** Encerra o servidor de teste e libera qualquer resposta deliberadamente suspensa. */
  @AfterEach
  void stopServer() {
    if (releaseResponse != null) releaseResponse.countDown();
    if (server != null) server.stop(0);
    if (serverExecutor != null) serverExecutor.shutdownNow();
  }

  /** Garante que uma lentidão do backend não ocupe para sempre a thread de reconexão. */
  @Test
  void shouldReleaseSchedulerAfterBackendReadTimeout() throws Exception {
    CountDownLatch requestReceived = new CountDownLatch(1);
    releaseResponse = new CountDownLatch(1);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    serverExecutor = Executors.newSingleThreadExecutor();
    server.setExecutor(serverExecutor);
    server.createContext(
        "/api/internal/agents/executor-health/communication-director/codex-auth/reconnections/pending",
        exchange -> {
          requestReceived.countDown();
          try {
            releaseResponse.await(2, TimeUnit.SECONDS);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    CodexAuthReconnectScheduler scheduler =
        new CodexAuthReconnectScheduler(
            RestClient.builder(),
            "http://127.0.0.1:" + server.getAddress().getPort(),
            "/workspace/marketing-hub",
            "communication-director",
            Duration.ofMillis(100),
            Duration.ofMillis(100));

    assertTimeout(Duration.ofSeconds(1), scheduler::processPending);

    assertThat(requestReceived.await(1, TimeUnit.SECONDS)).isTrue();
  }
}
