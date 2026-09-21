package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** Homologa navegador real, bytes, upload e contexto persistido usando apenas fontes sintéticas. */
@EnabledIfEnvironmentVariable(named = "PSIQUE_BROWSER_INTEGRATION_TEST", matches = "true")
class VisualCapturePipelineIntegrationTest {
  @TempDir Path directory;

  /** Encadeia a captura oficial ao callback auditável sem pagar IA ou enviar uma compra. */
  @Test
  void carriesTwoRealPagesThroughUploadPromptAndPersistentEvidence() throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var purchaseCalls = new AtomicInteger();
    var uploads = new AtomicInteger();
    var expectedArtifacts = new ArrayList<BpmVisualEvidenceRunner.VisualArtifact>();
    String landing =
        """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <meta name="mh-publication-source-sha256" content="SOURCE_HASH">
        <meta name="mh-served-html-sha256" content="SERVED_HASH"></head>
        <body><h1>Pacote sintético de receitas</h1><a href="/checkout">Comprar pacote</a>
        <p>Entrega após briefing, exemplo de homologação sem oferta real.</p></body></html>
        """
            .replace("SOURCE_HASH", "a".repeat(64))
            .replace("SERVED_HASH", "b".repeat(64));
    String checkout =
        """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body><h1>Pacote sintético de receitas</h1><p>R$ 39,00. Pagamento único.</p>
        <form method="post" action="/pay"><button>Pagar agora</button></form></body></html>
        """;
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          if (path.contains("visual-evidence")) {
            var artifact = expectedArtifacts.get(uploads.getAndIncrement());
            byte[] pixels = Files.readAllBytes(Path.of(artifact.localPath()));
            String body =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertThat(body).contains(artifact.evidenceKey(), artifact.captureSessionId());
            String hash;
            try {
              hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pixels));
            } catch (java.security.NoSuchAlgorithmException ex) {
              throw new java.io.IOException(ex);
            }
            var persisted =
                new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
                    700L + uploads.get(),
                    artifact.captureSessionId(),
                    artifact.evidenceKey(),
                    artifact.evidenceType(),
                    "Prova sintética",
                    artifact.deviceProfile(),
                    artifact.pageNumber(),
                    artifact.foldNumber(),
                    artifact.viewportWidth(),
                    artifact.viewportHeight(),
                    artifact.pageHeightPx(),
                    artifact.scrollY(),
                    artifact.sourceUrl(),
                    artifact.finalUrl(),
                    "/evidence/" + uploads.get(),
                    (long) pixels.length,
                    hash,
                    artifact.capturedAt(),
                    null);
            byte[] bytes = json.writeValueAsBytes(persisted);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
          } else if (path.equals("/version-diagnostics.json")) {
            exchange.sendResponseHeaders(404, -1);
          } else {
            if (!exchange.getRequestMethod().equals("GET") || path.equals("/pay"))
              purchaseCalls.incrementAndGet();
            byte[] bytes =
                (path.equals("/checkout") ? checkout : landing).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html;charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
          }
          exchange.close();
        });
    server.start();
    try {
      String base = "http://example.com:" + server.getAddress().getPort();
      Path browser = Path.of("src/main/resources/browser/bpm-visual-evidence.mjs").toAbsolutePath();
      Path playwright = Path.of("node_modules/playwright-core/index.mjs").toAbsolutePath();
      Path adapter = directory.resolve("local-browser.mjs");
      // Apenas o adaptador de teste resolve o domínio público para o servidor sintético local.
      Files.writeString(
          adapter,
          """
          import {chromium} from PLAYWRIGHT_MODULE;
          const launch=chromium.launch.bind(chromium);
          chromium.launch=options=>launch({...options,args:[...options.args,
            '--host-resolver-rules=MAP example.com 127.0.0.1','--proxy-server=direct://']});
          await import(BROWSER_MODULE);
          """
              .replace("PLAYWRIGHT_MODULE", json.writeValueAsString(playwright.toUri().toString()))
              .replace("BROWSER_MODULE", json.writeValueAsString(browser.toUri().toString())));
      var runner = new BpmVisualEvidenceRunner(json, "node", adapter.toString());
      var bundle =
          runner.capture(
              base + "/kit?mh_test=1",
              directory.resolve("capture"),
              PdeExperienceEvidenceLoader.LiveVisualContract.publishedPage(
                  "a".repeat(64), "Comprar pacote"),
              List.of(base + "/checkout"));
      assertThat(bundle.capture().pages()).hasSize(2);
      assertThat(bundle.capture().pages().getFirst().documentSha256())
          .isEqualTo(
              HexFormat.of()
                  .formatHex(
                      MessageDigest.getInstance("SHA-256")
                          .digest(landing.getBytes(StandardCharsets.UTF_8))));
      expectedArtifacts.addAll(bundle.capture().artifacts());
      var client =
          new BpmVisualEvidenceBackendClient("http://127.0.0.1:" + server.getAddress().getPort());
      var evidence = client.upload(1205L, bundle);
      assertThat(evidence).hasSize(4);
      assertThat(uploads.get()).isEqualTo(4);
      assertThat(purchaseCalls.get()).isZero();
      var task =
          Map.<String, Object>of(
              "taskId",
              1205L,
              "processCode",
              "quartzo-commercial-preparation-v1",
              "sourceReference",
              "experiment:504",
              "activityId",
              "humanExperienceReview",
              "processContextJson",
              "{\"quartzoCommercial\":{\"fingerprint\":\"synthetic-scope\"}}");
      var enriched = CustomerBpmTaskConsumer.withCaptureFacts(task, bundle, evidence);
      var outbox = new CustomerBpmTaskOutbox(directory.resolve("outbox"), json);
      outbox.save(
          new CustomerBpmTaskOutbox.Pending(enriched, Map.of(), evidence, false, null, null));
      var worker =
          new CustomerBpmTaskConsumer(
              "http://127.0.0.1:1",
              "/never-paid",
              "gpt-5.6-sol",
              "max",
              "/missing",
              "/missing",
              json);
      assertThat(worker.prompt(outbox.read().task(), outbox.read().visualEvidence()))
          .contains("R$ 39,00", "documentSha256", "READ_ONLY_INITIAL_PAGES", "synthetic-scope");
      exportCapture(json, bundle);
    } finally {
      server.stop(0);
    }
  }

  /** Exporta somente a fixture sintética quando a integração com o backend é solicitada. */
  private void exportCapture(ObjectMapper json, BpmVisualEvidenceRunner.VisualEvidenceBundle bundle)
      throws Exception {
    String configured = System.getenv("PSIQUE_CAPTURE_EVIDENCE_OUTPUT");
    if (configured == null || configured.isBlank()) return;
    Path output = Path.of(configured).toAbsolutePath();
    Files.createDirectories(output);
    var serialized =
        json.copy()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .<com.fasterxml.jackson.databind.node.ObjectNode>valueToTree(bundle.capture());
    for (var artifact : serialized.path("artifacts")) {
      Path source = Path.of(artifact.path("localPath").asText());
      Path destination = output.resolve(source.getFileName());
      Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      ((com.fasterxml.jackson.databind.node.ObjectNode) artifact)
          .put("localPath", destination.toString());
    }
    Files.writeString(output.resolve("capture.json"), json.writeValueAsString(serialized));
  }
}
