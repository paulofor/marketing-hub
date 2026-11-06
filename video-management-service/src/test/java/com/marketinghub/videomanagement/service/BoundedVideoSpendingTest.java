package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.*;

/** Responsabilidade: provar recuperação de planner, clipe e bytes de voz sem chamada duplicada. */
class BoundedVideoSpendingTest {
  @TempDir Path directory;
  final ObjectMapper json = new ObjectMapper();
  final BackendVideoClient backend = mock(BackendVideoClient.class);
  final AtomicInteger calls = new AtomicInteger();
  ObjectNode context, lease, request;
  BoundedVideoSpending spending;

  /** Configura a autoridade simulada e o mesmo transporte usado pelo worker real. */
  @BeforeEach
  void setup() {
    spending = new BoundedVideoSpending(backend, json, directory);
    request = json.createObjectNode().put("model", "local-test");
    lease =
        json.createObjectNode()
            .put("id", 701L)
            .put("state", "RESERVED")
            .put("requestSha256", "fixed-local-hash")
            .put("quoteExpiresAt", java.time.Instant.now().plusSeconds(300).toString());
    lease.set("request", request);
    context = json.createObjectNode().put("required", true).put("receiptId", 128L);
    when(backend.spending(eq(31L), eq("CLIP_1"), eq(""), isNull())).thenReturn(context);
    when(backend.spending(eq(31L), eq("CLIP_1"), eq("/reserve"), any())).thenReturn(lease);
    when(backend.spending(eq(31L), eq("CLIP_1"), eq("/701/begin"), any()))
        .thenAnswer(
            a -> {
              var value = lease.deepCopy();
              value.put("mayInvoke", "RESERVED".equals(lease.path("state").asText()));
              lease.put("state", "SUBMITTED");
              return value;
            });
    when(backend.spending(eq(31L), eq("CLIP_1"), eq("/701/result"), any())).thenReturn(lease);
  }

  /** Reinício após callback perdido reaproveita request, resposta e ID do mesmo clipe. */
  @Test
  void callbackFailureRestoresWithoutSecondProviderCall() throws Exception {
    when(backend.spending(eq(31L), eq("CLIP_1"), eq("/701/result"), any()))
        .thenThrow(new IllegalStateException("local callback unavailable"))
        .thenReturn(lease);
    assertThatThrownBy(() -> call()).hasMessageContaining("conciliação");
    Path cached = directory.resolve("128-31-CLIP_1.json");
    java.nio.file.Files.move(cached, cached.resolveSibling(cached.getFileName() + ".pending"));
    context.set("reservation", lease);
    spending = new BoundedVideoSpending(backend, json, directory);
    assertThat(call().path("id").asText()).isEqualTo("provider-original");
    assertThat(calls.get()).isEqualTo(1);
  }

  /** Timeout após envio retém reserva e recusa reenvio mesmo após reiniciar a montagem. */
  @Test
  void uncertainSubmissionNeverRepeats() {
    assertThatThrownBy(
            () ->
                spending.jsonCall(
                    31L,
                    "CLIP_1",
                    request,
                    null,
                    () -> {
                      calls.incrementAndGet();
                      throw new IllegalStateException("local network reset");
                    }))
        .hasMessageContaining("conciliação");
    context.set("reservation", lease);
    spending = new BoundedVideoSpending(backend, json, directory);
    assertThatThrownBy(this::call)
        .hasRootCauseMessage("Envio anterior incerto; não repetir chamada paga");
    assertThat(calls.get()).isEqualTo(1);
  }

  /** Falta de cobertura e request alterado não chegam ao provedor. */
  @Test
  void budgetDenialAndDifferentRequestHaveNoProviderSideEffects() {
    when(backend.spending(eq(31L), eq("CLIP_1"), eq("/reserve"), any()))
        .thenThrow(new IllegalArgumentException("limit"));
    assertThatThrownBy(this::call).hasMessageContaining("limit");
    assertThat(calls.get()).isZero();
    context.set("reservation", lease);
    request.put("duration", 10);
    // Congela outra entrada na reserva; a mutação da fixture não pode conceder autoridade.
    lease.set("request", json.createObjectNode().put("model", "other"));
    assertThatThrownBy(this::call).hasMessageContaining("diverge");
    assertThat(calls.get()).isZero();
  }

  /** Bytes binários e cabeçalhos são recuperados exatamente, sem nova síntese de voz. */
  @Test
  void speechBytesSurviveRestartAndProviderKeyLoss() {
    byte[] audio = {0, 1, 2, 3, 4, 5};
    var first =
        spending.binaryCall(
            31L,
            "CLIP_1",
            request,
            () -> {
              calls.incrementAndGet();
              return ResponseEntity.ok()
                  .contentType(MediaType.valueOf("audio/wav"))
                  .header("x-request-id", "voice-original")
                  .body(audio);
            });
    context.set("reservation", lease);
    var restart = new BoundedVideoSpending(backend, json, directory);
    var replay =
        restart.binaryCall(
            31L,
            "CLIP_1",
            request,
            () -> {
              throw new AssertionError("No paid replay");
            });
    assertThat(first.getBody()).containsExactly(audio);
    assertThat(replay.getBody()).containsExactly(audio);
    assertThat(replay.getHeaders().getFirst("x-request-id")).isEqualTo("voice-original");
    assertThat(calls.get()).isEqualTo(1);
  }

  /** Caminho legado permanece funcional, mas autoridade ilegível nunca autoriza fallback. */
  @Test
  void legacyAndMalformedAuthority() {
    context.put("required", false);
    assertThat(call().path("id").asText()).isEqualTo("provider-original");
    context.put("required", "false");
    assertThatThrownBy(this::call).hasMessageContaining("Autoridade");
    assertThat(calls.get()).isEqualTo(1);
  }

  /** Simula apenas o resultado externo; nenhuma credencial ou API paga participa. */
  JsonNode call() {
    return spending.jsonCall(
        31L,
        "CLIP_1",
        request,
        null,
        () -> {
          calls.incrementAndGet();
          return json.createObjectNode().put("id", "provider-original");
        });
  }
}
