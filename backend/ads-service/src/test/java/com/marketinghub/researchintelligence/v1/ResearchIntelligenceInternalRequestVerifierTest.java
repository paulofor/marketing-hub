package com.marketinghub.researchintelligence.v1;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.researchintelligence.v1.service.managecard.ResearchIntelligenceCardTransitionRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

/** Comprova que alterações ou chamadas vencidas não alcançam a gestão persistente. */
class ResearchIntelligenceInternalRequestVerifierTest {
  private static final byte[] KEY =
      "internal-signing-key-with-40-characters-1234".getBytes(StandardCharsets.UTF_8);
  private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");
  private ObjectMapper objectMapper;
  private ResearchIntelligenceInternalRequestVerifier verifier;

  /** Prepara serialização de datas e relógio determinísticos. */
  @BeforeEach
  void setUp() {
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    verifier =
        new ResearchIntelligenceInternalRequestVerifier(
            objectMapper, Clock.fixed(NOW, ZoneOffset.UTC), KEY);
  }

  /** Aceita uma chamada recente quando corpo, ator e chave idempotente foram assinados. */
  @Test
  void shouldAcceptValidSignedBody() throws Exception {
    var body = new ResearchIntelligenceCardTransitionRequest("Fonte revisada.");
    MockHttpServletRequest request =
        signedRequest(body, NOW.getEpochSecond(), "actor-test", "idem-12345678");

    assertThatCode(() -> verifier.verify(request, "actor-test", "idem-12345678", body))
        .doesNotThrowAnyException();
  }

  /** Rejeita adulteração de conteúdo mesmo quando os demais cabeçalhos permanecem válidos. */
  @Test
  void shouldRejectChangedBody() throws Exception {
    var signedBody = new ResearchIntelligenceCardTransitionRequest("Fonte revisada.");
    var changedBody = new ResearchIntelligenceCardTransitionRequest("Fonte não revisada.");
    MockHttpServletRequest request =
        signedRequest(signedBody, NOW.getEpochSecond(), "actor-test", "idem-12345678");

    assertThatThrownBy(() -> verifier.verify(request, "actor-test", "idem-12345678", changedBody))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("401");
  }

  /** Rejeita replay fora da janela de cinco minutos. */
  @Test
  void shouldRejectExpiredTimestamp() throws Exception {
    var body = new ResearchIntelligenceCardTransitionRequest("Fonte revisada.");
    MockHttpServletRequest request =
        signedRequest(body, NOW.minusSeconds(301).getEpochSecond(), "actor-test", "idem-12345678");

    assertThatThrownBy(() -> verifier.verify(request, "actor-test", "idem-12345678", body))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("401");
  }

  /** Rejeita chave idempotente inválida mesmo quando ela foi incluída na assinatura recebida. */
  @Test
  void shouldRejectMalformedSignedIdempotencyKey() throws Exception {
    var body = new ResearchIntelligenceCardTransitionRequest("Fonte revisada.");
    MockHttpServletRequest request =
        signedRequest(body, NOW.getEpochSecond(), "actor-test", "curta");

    assertThatThrownBy(() -> verifier.verify(request, "actor-test", "curta", body))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("401");
  }

  /** Monta os mesmos bytes e representação canônica usados pelo gateway. */
  private MockHttpServletRequest signedRequest(
      Object body, long epochSecond, String actor, String idempotencyKey) throws Exception {
    String path = "/api/internal/research-intelligence/v1/cards/test/versions/1/activate";
    String requestId = "b39ea871-1f95-4d7e-b3d5-7e626caf2c44";
    byte[] bytes = objectMapper.writeValueAsBytes(body);
    String contentHash = sha256(bytes);
    String timestamp = Long.toString(epochSecond);
    String canonical =
        ResearchIntelligenceInternalRequestVerifier.canonicalRequest(
            "POST", path, Map.of(), timestamp, requestId, actor, idempotencyKey, contentHash);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.addHeader(ResearchIntelligenceInternalRequestVerifier.TIMESTAMP_HEADER, timestamp);
    request.addHeader(ResearchIntelligenceInternalRequestVerifier.REQUEST_ID_HEADER, requestId);
    request.addHeader(
        ResearchIntelligenceInternalRequestVerifier.CONTENT_SHA256_HEADER, contentHash);
    request.addHeader(
        ResearchIntelligenceInternalRequestVerifier.SIGNATURE_HEADER, hmac(canonical));
    return request;
  }

  /** Calcula o hash usado nos cabeçalhos de teste. */
  private String sha256(byte[] value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
  }

  /** Calcula a assinatura usada nos cabeçalhos de teste. */
  private String hmac(String canonical) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
  }
}
