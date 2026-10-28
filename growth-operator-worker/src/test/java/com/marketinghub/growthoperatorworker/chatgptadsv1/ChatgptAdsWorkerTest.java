package com.marketinghub.growthoperatorworker.chatgptadsv1;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** Exercita transporte, isolamento de conta e recuperação real de callback por HTTP local. */
class ChatgptAdsWorkerTest {
  @TempDir Path directory;
  HttpServer server;
  ObjectMapper mapper = new ObjectMapper();
  ChatgptAdsWorker worker;
  AtomicInteger providerCalls = new AtomicInteger(), callbackCalls = new AtomicInteger();
  ObjectNode work, received;
  boolean failCallback, wrongAccount, tamperResponse;
  int providerStatus = 200;

  /** Inicia somente servidor local e credenciais sintéticas isoladas. */
  @BeforeEach
  void setup() throws Exception {
    Files.writeString(directory.resolve("worker-token"), "fake-internal");
    Files.writeString(directory.resolve("advertiser-key"), "fake-advertiser");
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    work =
        mapper
            .createObjectNode()
            .put("id", 71)
            .put("lease", "lease-a")
            .put("kind", "CAMPAIGN")
            .put("method", "POST")
            .put("path", "/campaigns")
            .put("idempotencyKey", "mkt-chatgpt-v1-71")
            .put("accountId", "adacct_test");
    work.set("body", mapper.createObjectNode().put("status", "paused"));
    server.createContext(
        "/api/internal/chatgpt-ads/v1/delivery/stage-executions",
        e -> {
          assertNull(e.getRequestHeaders().getFirst("Authorization"));
          String body = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          String time = e.getRequestHeaders().getFirst("X-Mkt-Ads-Time"),
              nonce = e.getRequestHeaders().getFirst("X-Mkt-Ads-Nonce");
          assertEquals(
              sign(
                  "POST\n"
                      + e.getRequestURI().getRawPath()
                      + "\n"
                      + time
                      + "\n"
                      + nonce
                      + "\n"
                      + hash(body)),
              e.getRequestHeaders().getFirst("X-Mkt-Ads-Signature"));
          if (e.getRequestURI().getPath().endsWith("/pending")) {
            respond(e, 200, work.toString());
          } else {
            callbackCalls.incrementAndGet();
            received = (ObjectNode) mapper.readTree(body);
            respond(e, failCallback ? 503 : 200, "{}");
          }
        });
    server.createContext(
        "/v1/ad_account",
        e ->
            respond(
                e, 200, "{\"id\":\"" + (wrongAccount ? "other_account" : "adacct_test") + "\"}"));
    server.createContext(
        "/v1/campaigns",
        e -> {
          providerCalls.incrementAndGet();
          assertEquals("Bearer fake-advertiser", e.getRequestHeaders().getFirst("Authorization"));
          assertEquals("mkt-chatgpt-v1-71", e.getRequestHeaders().getFirst("Idempotency-Key"));
          respond(
              e,
              providerStatus,
              "{\"id\":\"camp_test\",\"status\":\"paused\",\"token\":\"fake-advertiser\",\"echo\":\"received fake-advertiser\"}");
        });
    server.start();
    String root = "http://127.0.0.1:" + server.getAddress().getPort();
    worker = new ChatgptAdsWorker(root, root + "/v1", directory, mapper);
  }

  /** Libera o servidor isolado após cada cenário. */
  @AfterEach
  void stop() {
    server.stop(0);
  }

  /** Responde em UTF-8 sem acesso a serviços externos. */
  private void respond(HttpExchange e, int status, String body) throws java.io.IOException {
    byte[] data = body.getBytes(StandardCharsets.UTF_8);
    if (e.getRequestURI().getPath().startsWith("/api/internal/"))
      e.getResponseHeaders()
          .add(
              "X-Mkt-Ads-Response",
              sign(
                  "RESPONSE\n"
                      + status
                      + "\n"
                      + e.getRequestHeaders().getFirst("X-Mkt-Ads-Nonce")
                      + "\n"
                      + hash(tamperResponse ? "changed" : body)));
    e.sendResponseHeaders(status, data.length);
    e.getResponseBody().write(data);
    e.close();
  }

  /** Calcula o hash dos bytes reais recebidos e enviados na fixture. */
  private String hash(String body) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(body.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException ex) {
      throw new AssertionError(ex);
    }
  }

  /** Assina a fixture independentemente do cliente HTTP em teste. */
  private String sign(String message) {
    try {
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(
          new javax.crypto.spec.SecretKeySpec(
              "fake-internal".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return java.util.HexFormat.of()
          .formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException ex) {
      throw new AssertionError(ex);
    }
  }

  /** Uma resposta adulterada não chega ao provedor mesmo com request interno válido. */
  @Test
  void rejectsTamperedBackendCommand() {
    tamperResponse = true;
    worker.poll();
    assertEquals(0, providerCalls.get());
    assertEquals(0, callbackCalls.get());
  }

  /** Uma falha de callback não provoca outra criação no provedor. */
  @Test
  void replaysPersistedReceiptWithNewLease() throws Exception {
    failCallback = true;
    worker.poll();
    assertEquals(1, providerCalls.get());
    failCallback = false;
    work.put("lease", "lease-b");
    worker.poll();
    assertEquals(1, providerCalls.get());
    assertEquals(2, callbackCalls.get());
    assertEquals("lease-b", received.path("lease").asText());
    assertFalse(
        Files.readString(directory.resolve("acknowledged-receipt-71.json"))
            .contains("fake-advertiser"));
  }

  /** Mudança de credencial para outra conta nunca publica recursos na identidade errada. */
  @Test
  void refusesWrongAccount() {
    wrongAccount = true;
    worker.poll();
    assertEquals(0, providerCalls.get());
    assertEquals("ACCOUNT_CHANGED_OR_UNAVAILABLE", received.path("errorCode").asText());
  }

  /** A falta da chave de anúncios retorna bloqueio explícito sem consumir API. */
  @Test
  void missingKeyIsRecoverableAfterConfiguration() throws Exception {
    Files.delete(directory.resolve("advertiser-key"));
    worker.poll();
    assertEquals("CREDENTIAL_MISSING", received.path("errorCode").asText());
    Files.writeString(directory.resolve("advertiser-key"), "fake-advertiser");
    worker.poll();
    assertEquals(1, providerCalls.get());
  }

  /** Redirecionamentos não recebem credenciais nem são considerados sucesso. */
  @Test
  void refusesRedirectAndPreservesFailure() {
    providerStatus = 302;
    worker.poll();
    assertEquals("HTTP_302", received.path("errorCode").asText());
    assertEquals(1, providerCalls.get());
  }

  /** Sem segredo interno o módulo fica inerte, preservando os outros trabalhos de Hermes. */
  @Test
  void missingInternalConnectionDoesNotPoll() throws Exception {
    Files.delete(directory.resolve("worker-token"));
    worker.poll();
    assertEquals(0, callbackCalls.get());
    assertEquals(0, providerCalls.get());
  }

  /** Um contrato com caminho absoluto não pode exfiltrar a chave para outra origem. */
  @Test
  void rejectsForeignUrl() {
    work.put("path", "https://malicious.example/steal");
    worker.poll();
    assertEquals(0, providerCalls.get());
    assertEquals(0, callbackCalls.get());
  }
}
