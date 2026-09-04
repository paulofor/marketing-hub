package com.marketinghub.harnesslibraryapi.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.harnesslibraryapi.config.HarnessLibraryProperties;
import com.marketinghub.harnesslibraryapi.config.SecretValueResolver;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Assina corpo, filtros, autoria e recência das chamadas destinadas ao backend. */
@Component
public class InternalRequestSigner {
  public static final String TIMESTAMP_HEADER = "X-Harness-Timestamp";
  public static final String REQUEST_ID_HEADER = "X-Harness-Request-Id";
  public static final String CONTENT_SHA256_HEADER = "X-Harness-Content-SHA256";
  public static final String SIGNATURE_HEADER = "X-Harness-Signature";
  private static final Logger log = LoggerFactory.getLogger(InternalRequestSigner.class);

  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final byte[] signingKey;

  /** Resolve a chave interna obrigatória sem transmiti-la ao backend. */
  @Autowired
  public InternalRequestSigner(ObjectMapper objectMapper, HarnessLibraryProperties properties) {
    this(
        objectMapper,
        Clock.systemUTC(),
        SecretValueResolver.resolve(
            "HARNESS_LIBRARY_INTERNAL_SIGNING_KEY",
            properties.internalSigningKey(),
            properties.internalSigningKeyFile()));
  }

  /** Permite controlar tempo e chave nos testes criptográficos. */
  InternalRequestSigner(ObjectMapper objectMapper, Clock clock, byte[] signingKey) {
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.signingKey = signingKey.clone();
  }

  /** Serializa o contrato uma única vez para que assinatura e transmissão usem os mesmos bytes. */
  public byte[] serialize(Object body) {
    if (body == null) {
      return new byte[0];
    }
    try {
      return objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar payload destinado ao backend da Biblioteca", ex);
      throw new IllegalStateException("Não foi possível serializar a requisição.", ex);
    }
  }

  /** Produz somente os cabeçalhos derivados, sem incluir a chave secreta. */
  public Map<String, String> sign(
      String method,
      String path,
      Map<String, List<String>> parameters,
      String actor,
      String idempotencyKey,
      String requestId,
      byte[] body) {
    String timestamp = Long.toString(Instant.now(clock).getEpochSecond());
    String contentHash = sha256(body);
    String canonical =
        String.join(
            "\n",
            method.toUpperCase(),
            path,
            canonicalQuery(parameters),
            timestamp,
            requestId,
            actor,
            idempotencyKey == null ? "" : idempotencyKey,
            contentHash);
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(TIMESTAMP_HEADER, timestamp);
    headers.put(REQUEST_ID_HEADER, requestId);
    headers.put(CONTENT_SHA256_HEADER, contentHash);
    headers.put(SIGNATURE_HEADER, hmac(canonical));
    return Map.copyOf(headers);
  }

  /** Ordena filtros e valores para reproduzir a canonicalização do backend. */
  private String canonicalQuery(Map<String, List<String>> parameters) {
    List<Map.Entry<String, String>> values = new ArrayList<>();
    parameters.forEach(
        (key, entries) ->
            entries.stream().sorted().forEach(value -> values.add(Map.entry(key, value))));
    values.sort(
        Comparator.comparing((Map.Entry<String, String> entry) -> entry.getKey())
            .thenComparing(Map.Entry::getValue));
    return values.stream()
        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
        .reduce((left, right) -> left + "&" + right)
        .orElse("");
  }

  /** Codifica espaços e caracteres reservados no mesmo padrão usado pelo backend. */
  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** Calcula a assinatura de autenticação sem registrar seu material de origem. */
  private String hmac(String canonical) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
      log.error("Falha criptográfica ao assinar chamada da Biblioteca", ex);
      throw new IllegalStateException("Assinatura interna indisponível.", ex);
    }
  }

  /** Calcula a impressão digital dos bytes que serão enviados. */
  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível ao assinar chamada da Biblioteca", ex);
      throw new IllegalStateException("SHA-256 indisponível.", ex);
    }
  }
}
