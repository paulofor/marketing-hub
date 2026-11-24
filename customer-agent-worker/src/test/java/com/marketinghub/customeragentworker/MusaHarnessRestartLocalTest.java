package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Executa o worker e navegador reais contra gateway, controller e MySQL locais após queda abrupta.
 */
@EnabledIfEnvironmentVariable(named = "MUSA_HARNESS_LOCAL_TEST", matches = "true")
class MusaHarnessRestartLocalTest {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(MusaHarnessRestartLocalTest.class);
  @TempDir Path directory;
  final ObjectMapper json = new ObjectMapper();

  /**
   * Mata o processo após a primeira gravação e comprova recuperação da mesma operação em três
   * perfis.
   */
  @Test
  void restartsMidScenarioWithoutNewIdentityOrDuplicateOperation() throws Exception {
    var root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    var script = root.resolve("src/main/resources/browser/pde-agent-validation-harness.mjs");
    var http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    for (String scenario : List.of("ADHERENT", "RECOVERY", "SAFETY")) {
      var stopped = new AtomicBoolean();
      var generationWrites = java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
      server.createContext(
          "/",
          exchange -> {
            try {
              var body = exchange.getRequestBody().readAllBytes();
              var request =
                  HttpRequest.newBuilder(
                      URI.create("http://127.0.0.1:57188" + exchange.getRequestURI()));
              exchange
                  .getRequestHeaders()
                  .forEach(
                      (key, values) -> {
                        if (!Set.of(
                                "host", "connection", "content-length", "upgrade", "http2-settings")
                            .contains(key.toLowerCase(Locale.ROOT)))
                          values.forEach(
                              value ->
                                  request.header(
                                      key,
                                      "origin".equalsIgnoreCase(key)
                                          ? "http://127.0.0.1:57188"
                                          : value));
                      });
              var response =
                  http.send(
                      request
                          .method(
                              exchange.getRequestMethod(),
                              HttpRequest.BodyPublishers.ofByteArray(body))
                          .build(),
                      HttpResponse.BodyHandlers.ofByteArray());
              boolean generate =
                  body.length > 0
                      && exchange.getRequestURI().getPath().endsWith("/commands")
                      && "generate".equals(json.readTree(body).path("action").asText());
              if (generate && response.statusCode() == 200) {
                generationWrites.add(json.readTree(response.body()).path("operationId").asText());
                if (stopped.compareAndSet(false, true)) {
                  var nodes =
                      ProcessHandle.current()
                          .descendants()
                          .filter(
                              p ->
                                  p.info()
                                      .commandLine()
                                      .orElse("")
                                      .contains("musa-continuous-harness.mjs"))
                          .toList();
                  for (var node : nodes) {
                    node.descendants().forEach(ProcessHandle::destroyForcibly);
                    node.destroyForcibly();
                  }
                }
              }
              response
                  .headers()
                  .map()
                  .forEach(
                      (key, values) -> {
                        if (!Set.of("content-length", "transfer-encoding", "connection")
                            .contains(key.toLowerCase(Locale.ROOT)))
                          exchange.getResponseHeaders().put(key, values);
                      });
              exchange.sendResponseHeaders(response.statusCode(), response.body().length);
              exchange.getResponseBody().write(response.body());
            } catch (Exception ex) {
              // A queda intencional pode encerrar o socket; não registra corpo nem credenciais
              // sintéticas.
              log.warn(
                  "Transporte local interrompido scenario={} path={}",
                  scenario,
                  exchange.getRequestURI().getPath(),
                  ex);
            } finally {
              exchange.close();
            }
          });
      server.start();
      try {
        String url =
            "http://127.0.0.1:"
                + server.getAddress().getPort()
                + "/experiences/musa-consultor-pwa-v1/agent-validation";
        var target =
            Map.of(
                "productId",
                91004,
                "experimentId",
                96115,
                "productSlug",
                "metodo-musa-7-dias",
                "experienceVersion",
                "musa-consultor-pwa-v1",
                "publicUrl",
                url,
                "pdeContext",
                Map.of(
                    "lineage",
                    Map.of("productId", 91004, "learningCycleId", 96022, "experimentId", 96115)));
        var task =
            Map.<String, Object>of(
                "taskId", 99100, "sourceReference", "experiment:96115", "taskTarget", target);
        var work = directory.resolve(scenario);
        var initialRunner =
            new PdeAgentValidationHarnessRunner(
                json, "node", script.toString(), "vega-local-internal-only", true);
        assertThatThrownBy(() -> initialRunner.run(task, "SCENARIO", scenario, work))
            .isInstanceOf(PdeAgentValidationHarnessRunner.HarnessException.class);
        assertThat(stopped)
            .as(Files.readString(work.resolve("agent-validation-browser.log")))
            .isTrue();
        byte[] originalInput = Files.readAllBytes(work.resolve("agent-validation-input.json"));
        var runner =
            new PdeAgentValidationHarnessRunner(
                json, "node", script.toString(), "vega-local-internal-only", true);
        var result = runner.run(task, "SCENARIO", scenario, work);
        assertThat(Files.readAllBytes(work.resolve("agent-validation-input.json")))
            .isEqualTo(originalInput);
        assertThat(result.result().path("decision").asText()).isEqualTo("APPROVED");
        assertThat(generationWrites.size()).isEqualTo("SAFETY".equals(scenario) ? 1 : 2);
        var replay = runner.run(task, "SCENARIO", scenario, work);
        assertThat(replay.result().path("scenarios")).isEqualTo(result.result().path("scenarios"));
        assertThat(generationWrites.size()).isEqualTo("SAFETY".equals(scenario) ? 1 : 2);
      } finally {
        server.stop(0);
      }
    }
  }
}
