package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: conferir upload, callbacks e custo zero da prova, sem navegador ou inferência
 * externos.
 */
class OpalaCommercialUiProofCaptureTest {
  /**
   * O backend recebe a captura e depois o resultado; hash divergente gera falha e preserva
   * auditoria sem modelo.
   */
  @Test
  void reportsOnlyVerifiedUploadAndKeepsFailureDeterministic() throws Exception {
    var json = new ObjectMapper();
    for (String mode : List.of("SUCCESS", "HASH_MISMATCH", "CALLBACK_FAILURE")) {
      boolean matchingHash = !"HASH_MISMATCH".equals(mode);
      var operations = new ArrayList<String>();
      var callback = new AtomicReference<JsonNode>();
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            try {
              String operation = exchange.getRequestURI().getPath().replaceAll(".*/", "");
              operations.add(operation);
              byte[] body = exchange.getRequestBody().readAllBytes();
              if (operation.equals("visual-evidence")) {
                assertThat(exchange.getRequestHeaders().getFirst("Content-Type"))
                    .contains("multipart/form-data");
                assertThat(new String(body, java.nio.charset.StandardCharsets.ISO_8859_1))
                    .contains("name=\"file\"", "name=\"deviceProfile\"", "PDE_UI_480");
                byte[] response =
                    json.writeValueAsBytes(
                        Map.of(
                            "id", 1200, "sha256", matchingHash ? "a".repeat(64) : "b".repeat(64)));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
              } else {
                if (!operation.equals("execution-audit")) callback.set(json.readTree(body));
                exchange.sendResponseHeaders(
                    "CALLBACK_FAILURE".equals(mode) && operation.equals("result") ? 503 : 204, -1);
              }
            } finally {
              exchange.close();
            }
          });
      server.start();
      try {
        var backend =
            RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        var capture =
            new OpalaCommercialUiProofCapture(
                backend,
                json,
                (script, input, png, receipt, log) -> {
                  Files.write(png, new byte[] {1, 2, 3});
                  var value = json.readTree(Files.readString(input)).deepCopy();
                  var result = (com.fasterxml.jackson.databind.node.ObjectNode) value;
                  result
                      .put("sha256", "a".repeat(64))
                      .put("captureSessionId", "opala-ui-801")
                      .put("evidenceKey", "COMMERCIAL-FIRST-VALUE")
                      .put("evidenceType", "FOLD")
                      .put("deviceProfile", "PDE_UI_480")
                      .put("pageNumber", 1)
                      .put("foldNumber", 1)
                      .put("viewportWidth", 480)
                      .put("viewportHeight", 900)
                      .put("pageHeightPx", 3000)
                      .put("scrollY", 900)
                      .put("sourceUrl", "https://candidate.sandbox.local")
                      .put("finalUrl", "https://candidate.sandbox.local")
                      .put("capturedAt", "2026-10-22T00:00:00Z");
                  Files.writeString(receipt, result.toString());
                });
        var input =
            json.createObjectNode()
                .put("contractVersion", OpalaCommercialUiProofCapture.CONTRACT)
                .put("experimentId", 106)
                .put("productId", 4)
                .put("cycleId", 13)
                .put("productVersion", "fixture-v14");
        input
            .putObject("copy")
            .put("headline", "Primeiro ajuste gratuito")
            .put("primaryText", "Veja uma orientação para experimentar.")
            .put("description", "Demonstração");
        var context = json.createObjectNode();
        context.putObject("opalaCommercial").set("staticProofPreparation", input);
        Map<String, Object> task =
            Map.of(
                "taskId",
                801L,
                "processCode",
                "opala-commercial-preparation-v1",
                "activityId",
                "creative",
                "processContextJson",
                context.toString());
        capture.complete(task, capture.input(task));
        assertThat(operations)
            .containsExactlyElementsOf(
                "CALLBACK_FAILURE".equals(mode)
                    ? List.of("execution-audit", "visual-evidence", "result", "failure")
                    : List.of(
                        "execution-audit", "visual-evidence", matchingHash ? "result" : "failure"));
        assertThat(callback.get().path("executionAudit").path("executionMode").asText())
            .isEqualTo("DETERMINISTIC");
        assertThat(callback.get().path("modelUsages").isEmpty()).isTrue();
        var evidence = json.readTree(callback.get().path("evidenceJson").asText());
        if (matchingHash) {
          assertThat(evidence.path("commercialUiProof").path("artifactId").asLong())
              .isEqualTo(1200L);
          assertThat(
                  json.readTree(callback.get().path("resultJson").asText())
                      .path("instruction")
                      .path("headline")
                      .asText())
              .isEqualTo("Primeiro ajuste gratuito");
        } else {
          assertThat(evidence.path("commercialUiProofFailure").isObject()).isTrue();
          assertThat(callback.get().path("blockerGuidance").path("category").asText())
              .isEqualTo("TECHNICAL_FAILURE");
        }
      } finally {
        server.stop(0);
      }
    }
  }
}
