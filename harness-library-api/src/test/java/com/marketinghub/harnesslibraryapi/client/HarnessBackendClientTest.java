package com.marketinghub.harnesslibraryapi.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.harnesslibraryapi.api.RegisterCardRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Comprova encaminhamento exato, assinatura e tradução de falhas do backend. */
class HarnessBackendClientTest {
  private MockWebServer server;
  private HarnessBackendClient client;

  /** Inicia um backend falso com relógio e chave conhecidos. */
  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    ObjectMapper objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    InternalRequestSigner signer =
        new InternalRequestSigner(
            objectMapper,
            Clock.fixed(Instant.parse("2026-09-04T12:00:00Z"), ZoneOffset.UTC),
            "internal-signing-key-with-40-characters-1234".getBytes(StandardCharsets.UTF_8));
    client =
        new HarnessBackendClient(
            RestClient.builder().baseUrl(server.url("/").toString()).build(), signer);
  }

  /** Encerra o servidor falso sem deixar socket ativo entre testes. */
  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  /** Envia exatamente o JSON assinado com ator, request ID e idempotência. */
  @Test
  void shouldSignAndForwardRegistration() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(
                """
                {"cardKey":"homologacao-card","version":1,"cardId":"RI1-AAAAAAAAAAAA","status":"DRAFT","effectiveStatus":"DRAFT","collection":"video","routableAgents":["videomaker"],"sourceKind":"TEXT","sourceUri":"urn:test:card","sourceTitle":"Fonte sintética","sourceSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                """));

    var response =
        client.register(
            request(),
            "codex-homologacao",
            "idem-register-001",
            "b39ea871-1f95-4d7e-b3d5-7e626caf2c44");

    RecordedRequest received = server.takeRequest();
    assertThat(received.getMethod()).isEqualTo("POST");
    assertThat(received.getPath()).isEqualTo("/api/internal/research-intelligence/v1/cards");
    assertThat(received.getHeader("X-Actor")).isEqualTo("codex-homologacao");
    assertThat(received.getHeader("Idempotency-Key")).isEqualTo("idem-register-001");
    assertThat(received.getHeader("X-Harness-Timestamp")).isEqualTo("1788523200");
    assertThat(received.getHeader("X-Harness-Content-SHA256")).matches("[0-9a-f]{64}");
    assertThat(received.getHeader("X-Harness-Signature")).matches("[0-9a-f]{64}");
    assertThat(received.getBody().readUtf8()).contains("\"cardKey\":\"homologacao-card\"");
    assertThat(response.status()).isEqualTo("DRAFT");
  }

  /** Converte conflito editorial do backend em conflito público sem revelar seu corpo. */
  @Test
  void shouldTranslateBackendConflict() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(409)
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"detail\":\"internal database detail\"}"));

    assertThatThrownBy(
            () ->
                client.register(
                    request(),
                    "codex-homologacao",
                    "idem-register-001",
                    "b39ea871-1f95-4d7e-b3d5-7e626caf2c44"))
        .isInstanceOf(BackendApiException.class)
        .hasMessage("Operação conflita com o estado editorial atual.")
        .hasMessageNotContaining("database detail");
  }

  /** Monta um cartão completo para os contratos de integração. */
  private RegisterCardRequest request() {
    return new RegisterCardRequest(
        "homologacao-card",
        "video",
        "Demonstração clara",
        "Mostrar reduz ambiguidade.",
        "Concretização visual.",
        "Comparar retenção e CTA.",
        "Hipótese externa.",
        LocalDate.of(2026, 9, 4),
        LocalDate.of(2026, 10, 19),
        "Aumentará CTA.",
        "Generalização.",
        "Pagamento comprova venda.",
        "TEXT",
        "urn:test:card",
        "Fonte sintética",
        "a".repeat(64));
  }
}
