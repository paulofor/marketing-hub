package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercita HTTP real local, bytes de áudio e recuperação de callback sem nova cobrança. */
class FinalVideoAudioReviewTest {
  @TempDir Path dir;
  private final ObjectMapper json = new ObjectMapper();

  /** Duas identidades reaproveitam o recibo anterior sem chamada paga ou novo callback. */
  @Test
  void acceptsBackendReuseAndReportsRefusalWithoutRetrying() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var calls = new AtomicInteger();
    var refusal = new AtomicBoolean();
    server.createContext(
        "/",
        exchange -> {
          try {
            JsonNode response;
            if (exchange.getRequestMethod().equals("GET")) response = json.createArrayNode();
            else if (exchange.getRequestURI().getPath().endsWith("/audio-reviews")) {
              var body = json.readTree(exchange.getRequestBody());
              response =
                  json.createObjectNode()
                      .put("id", 8888)
                      .put("assetId", body.path("assetId").asLong())
                      .put("videoSha256", body.path("videoSha256").asText())
                      .put("audioSha256", body.path("audioSha256").asText())
                      .put("status", refusal.get() ? "REFUSED" : "COMPLETED")
                      .put("mayInvoke", false)
                      .put("estimatedUsd", 0.03)
                      .put(
                          "error",
                          refusal.get() ? "Provedor recusou a escuta; custo conhecido." : "");
            } else {
              calls.incrementAndGet();
              response = json.createObjectNode();
            }
            byte[] bytes = json.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
          } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(FinalVideoAudioReviewTest.class)
                .error(
                    "Falha na recuperação local da escuta. endpoint={}",
                    exchange.getRequestURI(),
                    ex);
            throw new IllegalStateException(ex);
          } finally {
            exchange.close();
          }
        });
    server.start();
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    try {
      for (long id : new long[] {31001, 32001}) {
        Path work = Files.createDirectory(dir.resolve("reuse-" + id));
        byte[] bytes = new byte[128];
        Files.write(work.resolve("mix.wav"), bytes);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        Files.writeString(
            work.resolve("audio-manifest.json"),
            json.writeValueAsString(
                List.of(
                    Map.of(
                        "assetId",
                        id,
                        "videoSha256",
                        "a".repeat(64),
                        "audioSha256",
                        hash,
                        "localPath",
                        work.resolve("mix.wav").toString()))));
        var task =
            Map.<String, Object>of(
                "taskId",
                id + 1,
                "taskTarget",
                Map.of(
                    "productId",
                    id + 2,
                    "pdeContext",
                    Map.of(
                        "audioReviewRequired",
                        true,
                        "finalVideoReview",
                        List.of(
                            Map.of(
                                "assetId", id, "sha256", "a".repeat(64), "durationSeconds", 30)))));
        var worker =
            new FinalVideoAudioReview(
                json, base, base + "/model", "", "synthetic-key", dir.resolve("state").toString());
        refusal.set(false);
        var result = worker.enrich(task, work);
        assertThat(json.valueToTree(result.get("audioReviews")).path(0).path("id").asLong())
            .isEqualTo(8888);
        refusal.set(true);
        assertThatThrownBy(() -> worker.enrich(task, work))
            .hasMessageContaining("REFUSED")
            .hasMessageContaining("custo conhecido");
      }
      assertThat(calls.get()).isZero();
    } finally {
      server.stop(0);
    }
  }

  /** Não envia briefing persuasivo à escuta e conserva os limites físicos recebidos do backend. */
  @Test
  void sendsMeasuredEvidenceWithoutMarketingBrief() throws Exception {
    var worker =
        new FinalVideoAudioReview(
            json, "http://127.0.0.1:1", "http://127.0.0.1:1/model", "", "", dir.toString());
    var media =
        json.createObjectNode()
            .put("assetId", 91001)
            .put("durationSeconds", 35)
            .put("narration", "ROTEIRO_NAO_DEVE_INDUZIR")
            .put("rejectionReason", "FEEDBACK_NAO_E_AUDIO");
    media.putObject("deliveryExamples").put("headline", "ANUNCIO_NAO_E_AUDIO");
    media
        .putObject("audioProductionEvidence")
        .put("mix", "VOICE_ONLY")
        .put("timingSource", "SEGMENT_AUDIO_DURATION")
        .putArray("segments")
        .addObject()
        .put("startSeconds", 0)
        .put("endSeconds", 6.312)
        .put("expectedText", "Comparação segmentada");
    var request = worker.request(media, new byte[64]);
    String prompt = request.at("/messages/0/content/0/text").asText();
    assertThat(prompt)
        .contains("6.312", "VOICE_ONLY", "Comparação segmentada")
        .doesNotContain("ROTEIRO_NAO_DEVE_INDUZIR", "FEEDBACK_NAO_E_AUDIO", "ANUNCIO_NAO_E_AUDIO");
  }

  /** Outro produto recebe recibo próprio; falha de callback reutiliza exatamente a resposta. */
  @Test
  void recoversAcrossWorkerRestartAndKeepsProductsIsolated() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var receipt = new AtomicReference<ObjectNode>();
    var calls = new AtomicInteger();
    var callbackFails = new AtomicBoolean(true);
    server.createContext(
        "/",
        exchange -> {
          try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            JsonNode body =
                method.equals("GET")
                    ? json.createObjectNode()
                    : json.readTree(exchange.getRequestBody());
            int status = 200;
            JsonNode response;
            if (path.equals("/model")) {
              calls.incrementAndGet();
              assertThat(body.at("/messages/0/content/1/type").asText()).isEqualTo("input_audio");
              assertThat(
                      Base64.getDecoder()
                          .decode(body.at("/messages/0/content/1/input_audio/data").asText()))
                  .hasSize(128);
              assertThat(body.path("modalities").toString()).isEqualTo("[\"text\"]");
              response = json.createObjectNode().put("providerFixture", true);
            } else if (method.equals("GET")) {
              var array = json.createArrayNode();
              if (receipt.get() != null) array.add(receipt.get());
              response = array;
            } else if (path.endsWith("/result")) {
              assertThat(body.path("rawResponse").asText()).isEqualTo("{\"providerFixture\":true}");
              if (callbackFails.getAndSet(false)) {
                status = 503;
                response = json.createObjectNode();
              } else {
                receipt.get().put("status", "COMPLETED").put("mayInvoke", false);
                receipt.get().putObject("assessment").put("verdict", "PASS");
                response = receipt.get();
              }
            } else {
              receipt.set(
                  json.createObjectNode()
                      .put("id", 9101)
                      .put("assetId", body.path("assetId").asLong())
                      .put("videoSha256", body.path("videoSha256").asText())
                      .put("audioSha256", body.path("audioSha256").asText())
                      .put("status", "RESERVED")
                      .put("mayInvoke", true));
              response = receipt.get();
            }
            byte[] bytes = json.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
          } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(FinalVideoAudioReviewTest.class)
                .error(
                    "Falha no servidor local de escuta. endpoint={}", exchange.getRequestURI(), ex);
            throw new IllegalStateException("Falha no servidor simulado", ex);
          } finally {
            exchange.close();
          }
        });
    server.start();
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    try {
      for (long id : new long[] {91001, 92001}) {
        receipt.set(null);
        callbackFails.set(true);
        Path work = Files.createDirectory(dir.resolve("capture-" + id));
        byte[] wav = new byte[128];
        Arrays.fill(wav, (byte) 7);
        Files.write(work.resolve("mix.wav"), wav);
        String audioHash =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(wav));
        Files.writeString(
            work.resolve("audio-manifest.json"),
            json.writeValueAsString(
                List.of(
                    Map.of(
                        "assetId",
                        id,
                        "videoSha256",
                        "a".repeat(64),
                        "audioSha256",
                        audioHash,
                        "localPath",
                        work.resolve("mix.wav").toString()))));
        var task =
            Map.<String, Object>of(
                "taskId",
                id + 1,
                "taskTarget",
                Map.of(
                    "productId",
                    id + 2,
                    "pdeContext",
                    Map.of(
                        "audioReviewRequired",
                        true,
                        "finalVideoReview",
                        List.of(
                            Map.of(
                                "assetId", id, "sha256", "a".repeat(64), "durationSeconds", 30)))));
        var worker =
            new FinalVideoAudioReview(
                json, base, base + "/model", "", "synthetic-key", dir.resolve("state").toString());
        assertThatThrownBy(() -> worker.enrich(task, work))
            .isInstanceOf(FinalVideoAudioReview.CallbackPendingException.class);
        int first = calls.get();
        var restarted =
            new FinalVideoAudioReview(
                json, base, base + "/model", "", "synthetic-key", dir.resolve("state").toString());
        var enriched = restarted.enrich(task, work);
        assertThat(json.valueToTree(enriched.get("audioReviews")).path(0).path("assetId").asLong())
            .isEqualTo(id);
        assertThat(calls.get()).isEqualTo(first);
        restarted.enrich(task, work);
        assertThat(calls.get()).isEqualTo(first);
      }
      assertThat(calls.get()).isEqualTo(2);
    } finally {
      server.stop(0);
    }
  }

  /** Sem vídeo ou contrato de escuta, não faz chamadas nem exige chave externa. */
  @Test
  void preservesImageOnlyPath() throws Exception {
    var worker =
        new FinalVideoAudioReview(
            json, "http://127.0.0.1:1", "http://127.0.0.1:1/model", "", "", dir.toString());
    var task = Map.<String, Object>of("taskId", 1);
    assertThat(worker.enrich(task, dir)).isSameAs(task);
  }
}
