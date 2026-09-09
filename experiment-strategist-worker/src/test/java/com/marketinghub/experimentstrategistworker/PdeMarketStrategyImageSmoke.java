package com.marketinghub.experimentstrategistworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Responsabilidade: comprovar a entrega persistente entre dois containers da imagem final. */
public final class PdeMarketStrategyImageSmoke {
  /** Impede instanciação do comando de homologação sem dependência de frameworks de teste. */
  private PdeMarketStrategyImageSmoke() {}

  /** Executa uma fase isolada com HTTP e modelo simulados, mantendo o estado no volume real. */
  public static void main(String[] args) throws Exception {
    String phase = args[0];
    boolean produce = "produce".equals(phase);
    if (!produce && !"deliver".equals(phase)) throw new IllegalArgumentException("Fase inválida");
    Path directory = Path.of("/var/lib/atena/image-smoke");
    Files.createDirectories(directory);
    var json = new ObjectMapper();
    var callbacks = new AtomicInteger();
    var claims = new AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String operation = Path.of(exchange.getRequestURI().getPath()).getFileName().toString();
          byte[] input = exchange.getRequestBody().readAllBytes();
          byte[] output = new byte[0];
          int status = 204;
          if ("automatic-execution".equals(operation)) {
            status = 200;
            output = json.writeValueAsBytes(Map.of("automaticExecutionEnabled", produce));
          } else if ("pending".equals(operation)) {
            claims.incrementAndGet();
            status = 200;
            output =
                json.writeValueAsBytes(
                    List.of(
                        Map.of(
                            "taskId",
                            358,
                            "sourceReference",
                            "experiment:92",
                            "taskTarget",
                            Map.of("productId", 4),
                            "processContextJson",
                            "{\"learningSalesCycle\":{\"productId\":4,\"experimentId\":92,\"productVersion\":\"v8-fixture\"}}")));
          } else if ("execution-audit".equals(operation)) {
            Files.write(directory.resolve("audit.json"), input);
          } else if ("result".equals(operation)) {
            callbacks.incrementAndGet();
            JsonNode body = json.readTree(input);
            if (produce) {
              Files.write(directory.resolve("expected-callback.json"), input);
              status = 503;
            } else if (!body.equals(
                json.readTree(Files.readAllBytes(directory.resolve("expected-callback.json"))))) {
              status = 409;
            }
          } else {
            status = 500;
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, output.length == 0 ? -1 : output.length);
          if (output.length > 0) exchange.getResponseBody().write(output);
          exchange.close();
        });
    server.start();
    try {
      var properties = new WorkerProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      properties.setBpmStateDirectory(directory.resolve("outbox").toString());
      properties.setRepositoryPath("/tests");
      properties.setCodexCommand("/tests/bpm/fake-codex.mjs");
      properties.setModel("fixture-atena-no-network");
      properties.setCodexTimeout(Duration.ofSeconds(10));
      var consumer =
          new PdeMarketStrategyBpmTaskConsumer(
              properties, json, new AutomaticExecutionControl(properties.getBackendUrl()));
      consumer.processOne();
      boolean pending = Files.exists(directory.resolve("outbox/pending.json"));
      long invocations = Files.readAllLines(directory.resolve("invocations.txt")).size();
      if (callbacks.get() != 1
          || claims.get() != (produce ? 1 : 0)
          || invocations != 1
          || pending != produce) {
        throw new IllegalStateException(
            "Entrega inesperada: phase="
                + phase
                + " callbacks="
                + callbacks
                + " claims="
                + claims
                + " invocations="
                + invocations
                + " pending="
                + pending);
      }
      System.out.println(
          "IMAGE_SMOKE_OK phase=" + phase + " modelInvocations=1 persisted=" + pending);
    } finally {
      server.stop(0);
    }
  }
}
