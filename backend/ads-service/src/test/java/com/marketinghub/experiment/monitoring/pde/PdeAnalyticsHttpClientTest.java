package com.marketinghub.experiment.monitoring.pde;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Valida o cliente HTTP que consulta o analytics administrativo do PDE. */
class PdeAnalyticsHttpClientTest {

  private HttpServer server;
  private ExecutorService executor;

  /** Encerra o servidor HTTP local usado pelos testes. */
  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  /** Garante que o client suporta summaries saudáveis que demoram mais que quatro segundos. */
  @Test
  void fetchSummaryWaitsForHealthySlowPdeSummary() throws Exception {
    server =
        HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0),
            0);
    server.createContext(
        "/api/pde/access/analytics/metodo-musa-7-dias/summary",
        this::respondSlowSummary);
    executor = Executors.newSingleThreadExecutor();
    server.setExecutor(executor);
    server.start();

    PdeAnalyticsHttpClient client =
        new PdeAnalyticsHttpClient(
            "http://127.0.0.1:" + server.getAddress().getPort(),
            Duration.ofSeconds(1),
            Duration.ofSeconds(7));

    PdeAnalyticsSummary summary = client.fetchSummary("metodo-musa-7-dias");

    assertThat(summary.productSlug()).isEqualTo("metodo-musa-7-dias");
    assertThat(summary.currentExperienceVersion())
        .isEqualTo("musa-pde-entry-v6-video-motivacional");
    assertThat(summary.pedEntries()).isEqualTo(29);
  }

  /** Responde um summary mínimo após atraso compatível com a produção do PDE v6. */
  private void respondSlowSummary(HttpExchange exchange) throws IOException {
    try {
      Thread.sleep(5_100);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Servidor de teste interrompido", ex);
    }
    byte[] body =
        """
        {
          "productSlug": "metodo-musa-7-dias",
          "currentExperienceVersion": "musa-pde-entry-v6-video-motivacional",
          "totalEvents": 371,
          "rawTotalEvents": 371,
          "uniqueVisitors": 29,
          "sessions": 29,
          "rawSessions": 29,
          "humanSessions": 29,
          "botSuspectedSessions": 0,
          "platformCrawlerSessions": 0,
          "internalQaSessions": 0,
          "unknownSessions": 0,
          "pedEntries": 29,
          "pageViews": 29,
          "loginStarted": 0,
          "loginCompleted": 0,
          "paywallViewed": 0,
          "subscriptionClicked": 0,
          "subscriptionApproved": 0,
          "accessReleased": 0,
          "firstUse": 0,
          "checkoutStarted": 0,
          "totalVisibleMs": 13375288,
          "lastEventAt": "2026-07-30T21:51:38-03:00",
          "events": [],
          "experienceVersions": [],
          "trafficSources": [],
          "trafficQualityBreakdown": [],
          "deviceBreakdown": [],
          "screenSizeBreakdown": [],
          "recentJourneys": []
        }
        """
            .getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }
}
