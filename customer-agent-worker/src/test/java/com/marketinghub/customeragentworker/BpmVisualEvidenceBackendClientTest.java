package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar o upload multipart da prova visual ao backend canônico. */
class BpmVisualEvidenceBackendClientTest {
  @TempDir Path temporaryDirectory;

  /** Envia arquivo e metadados correlacionados e preserva o id devolvido pelo backend. */
  @Test
  void uploadsVisualEvidenceThroughOfficialBackendContract() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    AtomicReference<String> contentType = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agent-tasks/customer-agent/stage-executions/258/visual-evidence",
        exchange -> {
          contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
          byte[] response =
              """
              {
                "id":901,
                "captureSessionId":"capture-rigel-258",
                "evidenceKey":"page-1-fold-1",
                "evidenceType":"FOLD",
                "label":"Página 1 · dobra 1",
                "deviceProfile":"IPHONE_15_PRO",
                "pageNumber":1,
                "foldNumber":1,
                "viewportWidth":393,
                "viewportHeight":852,
                "pageHeightPx":1704,
                "scrollY":0,
                "sourceUrl":"https://rigel.example/jornada",
                "finalUrl":"https://rigel.example/jornada",
                "contentUrl":"/api/agent-tasks/258/visual-evidence/901/content",
                "sizeBytes":8,
                "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "capturedAt":"2026-08-29T10:00:00Z"
              }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    Path pixels = temporaryDirectory.resolve("fold.png");
    Files.write(pixels, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    var artifact =
        new BpmVisualEvidenceRunner.VisualArtifact(
            "capture-rigel-258",
            "page-1-fold-1",
            "FOLD",
            "IPHONE_15_PRO",
            1,
            1,
            393,
            852,
            1704,
            0,
            "https://rigel.example/jornada",
            "https://rigel.example/jornada",
            Instant.parse("2026-08-29T10:00:00Z"),
            pixels.toString());
    var capture =
        new BpmVisualEvidenceRunner.CaptureOutput(
            "capture-rigel-258",
            "IPHONE_15_PRO",
            List.of(
                new BpmVisualEvidenceRunner.PageFacts(
                    1,
                    "https://rigel.example/jornada",
                    "https://rigel.example/jornada",
                    200,
                    "Rigel",
                    Map.of("width", 393, "height", 852),
                    List.of("Agenda Cheia"),
                    List.of("Quero começar"))),
            List.of(artifact));

    try {
      var client =
          new BpmVisualEvidenceBackendClient("http://127.0.0.1:" + server.getAddress().getPort());
      var uploaded =
          client.upload(
              258L, new BpmVisualEvidenceRunner.VisualEvidenceBundle(capture, temporaryDirectory));

      assertThat(uploaded).singleElement().extracting(value -> value.id()).isEqualTo(901L);
      assertThat(uploaded.getFirst().localPath()).isEqualTo(pixels.toAbsolutePath().toString());
      assertThat(contentType.get()).startsWith("multipart/form-data;boundary=");
      assertThat(requestBody.get())
          .contains(
              "name=\"captureSessionId\"",
              "capture-rigel-258",
              "name=\"evidenceType\"",
              "FOLD",
              "name=\"foldNumber\"",
              "name=\"file\"; filename=\"fold.png\"");
    } finally {
      server.stop(0);
    }
  }
}
