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

  /** Garante que o client consulta a identidade de build na mesma origem PDE monitorada. */
  @Test
  void fetchBuildIdentityUsesMonitoredPdeBaseUrl() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/pde/build-identity", this::respondBuildIdentity);
    executor = Executors.newSingleThreadExecutor();
    server.setExecutor(executor);
    server.start();

    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    PdeAnalyticsHttpClient client =
        new PdeAnalyticsHttpClient("https://v5.clubemusa.com.br", Duration.ofSeconds(1), Duration.ofSeconds(7));

    PdeBuildIdentity identity = client.fetchBuildIdentity(baseUrl);

    assertThat(identity.commitSha()).isEqualTo("abc123");
    assertThat(identity.branch()).isEqualTo("main");
    assertThat(identity.backendUrl()).isEqualTo("http://163.245.200.7:8096");
    assertThat(identity.marketingHubBaseUrl()).contains("191.252.181.168");
  }

  /** Garante fallback para versões PDE publicadas antes da rota dedicada de identidade. */
  @Test
  void fetchBuildIdentityFallsBackToDeployStatusWhenBuildIdentityRouteIsMissing() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/pde/build-identity", this::respondMissingBuildIdentity);
    server.createContext("/api/pde/deploy/status", this::respondDeployStatus);
    executor = Executors.newSingleThreadExecutor();
    server.setExecutor(executor);
    server.start();

    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    PdeAnalyticsHttpClient client =
        new PdeAnalyticsHttpClient("https://v5.clubemusa.com.br", Duration.ofSeconds(1), Duration.ofSeconds(7));

    PdeBuildIdentity identity = client.fetchBuildIdentity(baseUrl);

    assertThat(identity.commitSha()).isEqualTo("401a911");
    assertThat(identity.imageTag()).isEqualTo("401a911");
    assertThat(identity.backendImage()).isEqualTo("ghcr.io/paulofor/pde-platform-backend:401a911");
    assertThat(identity.backendUrl()).isEqualTo("http://163.245.200.7:8096");
    assertThat(identity.frontendUrl()).isEqualTo("https://v6.clubemusa.com.br");
    assertThat(identity.environment()).isEqualTo("production");
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

  /** Responde uma identidade mínima da build PDE para auditoria do cockpit. */
  private void respondBuildIdentity(HttpExchange exchange) throws IOException {
    byte[] body =
        """
        {
          "applicationName": "pde-platform-backend",
          "artifact": "pde-platform-backend",
          "buildVersion": "0.0.1-SNAPSHOT",
          "commitSha": "abc123",
          "branch": "main",
          "imageTag": "pde-v6-abc123",
          "backendImage": "registry/pde-platform-backend:pde-v6-abc123",
          "environment": "production",
          "backendUrl": "http://163.245.200.7:8096",
          "frontendUrl": "https://v6.clubemusa.com.br",
          "marketingHubBaseUrl": "http://191.252.181.168:8000,http://191.252.181.168",
          "deployedAt": "2026-07-31T10:00:00Z"
        }
        """
            .getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  /** Simula a build PDE em produção que ainda não publicou a rota dedicada. */
  private void respondMissingBuildIdentity(HttpExchange exchange) throws IOException {
    byte[] body = "{\"error\":\"Falha técnica na API PDE\"}".getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(500, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  /** Responde o manifesto legado usado como origem alternativa de identidade. */
  private void respondDeployStatus(HttpExchange exchange) throws IOException {
    byte[] body =
        """
        {
          "environment": "production",
          "commitSha": "401a911",
          "imageTag": "401a911",
          "frontendUrl": "https://v6.clubemusa.com.br",
          "backendUrl": "http://163.245.200.7:8096",
          "deployedAt": "2026-07-31T14:10:13Z",
          "services": [
            {
              "name": "pde-platform-backend",
              "image": "ghcr.io/paulofor/pde-platform-backend:401a911",
              "role": "backend"
            }
          ]
        }
        """
            .getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }
}
