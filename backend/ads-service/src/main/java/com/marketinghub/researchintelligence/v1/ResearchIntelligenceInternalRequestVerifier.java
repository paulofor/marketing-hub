package com.marketinghub.researchintelligence.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Verifica integridade, autoria e recência das chamadas internas do gateway do Harness. */
@Component
public class ResearchIntelligenceInternalRequestVerifier {
  public static final String TIMESTAMP_HEADER = "X-Harness-Timestamp";
  public static final String REQUEST_ID_HEADER = "X-Harness-Request-Id";
  public static final String CONTENT_SHA256_HEADER = "X-Harness-Content-SHA256";
  public static final String SIGNATURE_HEADER = "X-Harness-Signature";
  private static final Logger log =
      LoggerFactory.getLogger(ResearchIntelligenceInternalRequestVerifier.class);
  private static final Pattern REQUEST_ID_PATTERN =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
  private static final Pattern ACTOR_PATTERN = Pattern.compile("^[A-Za-z0-9._@-]{3,120}$");
  private static final Pattern IDEMPOTENCY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");
  private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
  private static final long MAX_CLOCK_SKEW_SECONDS = 300;

  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final byte[] signingKey;

  /** Resolve o secret uma única vez, preferindo o arquivo protegido montado no container. */
  @Autowired
  public ResearchIntelligenceInternalRequestVerifier(
      ObjectMapper objectMapper,
      @Value("${research-intelligence.internal.signing-key:}") String directKey,
      @Value("${research-intelligence.internal.signing-key-file:}") String keyFile) {
    this(objectMapper, Clock.systemUTC(), resolveKey(directKey, keyFile));
  }

  /** Permite controlar relógio e secret nos testes de contrato. */
  ResearchIntelligenceInternalRequestVerifier(
      ObjectMapper objectMapper, Clock clock, byte[] signingKey) {
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.signingKey = signingKey == null ? new byte[0] : signingKey.clone();
  }

  /** Confere cabeçalhos, corpo canônico e assinatura antes de qualquer acesso persistente. */
  public void verify(HttpServletRequest request, String actor, String idempotencyKey, Object body) {
    if (signingKey.length < 32) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Assinatura interna da Biblioteca não configurada.");
    }
    String requestId = requiredHeader(request, REQUEST_ID_HEADER);
    String timestamp = requiredHeader(request, TIMESTAMP_HEADER);
    String declaredContentHash = requiredHeader(request, CONTENT_SHA256_HEADER);
    String declaredSignature = requiredHeader(request, SIGNATURE_HEADER);
    if (!REQUEST_ID_PATTERN.matcher(requestId).matches()
        || !ACTOR_PATTERN.matcher(actor == null ? "" : actor).matches()
        || (idempotencyKey != null && !IDEMPOTENCY_PATTERN.matcher(idempotencyKey).matches())
        || !SHA256_PATTERN.matcher(declaredContentHash).matches()
        || !SHA256_PATTERN.matcher(declaredSignature).matches()) {
      reject(request, requestId, "formato de autenticação inválido");
    }
    long epochSecond = parseTimestamp(request, requestId, timestamp);
    long skew = Math.abs(Instant.now(clock).getEpochSecond() - epochSecond);
    if (skew > MAX_CLOCK_SKEW_SECONDS) {
      reject(request, requestId, "timestamp expirado");
    }
    byte[] serializedBody = serializeBody(request, requestId, body);
    String actualContentHash = sha256(serializedBody);
    if (!constantTimeEquals(declaredContentHash, actualContentHash)) {
      reject(request, requestId, "hash do corpo divergente");
    }
    String canonical =
        canonicalRequest(
            request.getMethod(),
            request.getRequestURI(),
            request.getParameterMap(),
            timestamp,
            requestId,
            actor,
            idempotencyKey,
            actualContentHash);
    String actualSignature = hmac(canonical);
    if (!constantTimeEquals(declaredSignature, actualSignature)) {
      reject(request, requestId, "assinatura divergente");
    }
  }

  /** Monta a representação estável compartilhada com o gateway externo. */
  static String canonicalRequest(
      String method,
      String path,
      Map<String, String[]> parameters,
      String timestamp,
      String requestId,
      String actor,
      String idempotencyKey,
      String contentHash) {
    return String.join(
        "\n",
        method.toUpperCase(),
        path,
        canonicalQuery(parameters),
        timestamp,
        requestId,
        actor,
        idempotencyKey == null ? "" : idempotencyKey,
        contentHash);
  }

  /** Ordena e codifica os filtros para que sua assinatura não dependa da ordem da URL. */
  private static String canonicalQuery(Map<String, String[]> parameters) {
    TreeMap<String, String[]> sorted = new TreeMap<>(parameters);
    StringBuilder query = new StringBuilder();
    sorted.forEach(
        (key, values) -> {
          String[] orderedValues = values == null ? new String[0] : values.clone();
          Arrays.sort(orderedValues);
          for (String value : orderedValues) {
            if (!query.isEmpty()) {
              query.append('&');
            }
            query.append(encode(key)).append('=').append(encode(value));
          }
        });
    return query.toString();
  }

  /** Codifica os componentes de consulta com espaços no padrão percent-encoding. */
  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** Lê uma chave direta ou montada e falha cedo quando o arquivo declarado é inválido. */
  private static byte[] resolveKey(String directKey, String keyFile) {
    if (StringUtils.hasText(keyFile)) {
      try {
        return Files.readString(Path.of(keyFile), StandardCharsets.UTF_8)
            .trim()
            .getBytes(StandardCharsets.UTF_8);
      } catch (IOException ex) {
        log.error(
            "Falha ao ler secret interno da Biblioteca arquivo={} errorLine={} errorClass={} errorMessage={}",
            keyFile,
            errorLine(ex),
            ex.getClass().getName(),
            ex.getMessage(),
            ex);
        throw new IllegalStateException("Secret interno da Biblioteca não pode ser lido.", ex);
      }
    }
    return StringUtils.hasText(directKey)
        ? directKey.trim().getBytes(StandardCharsets.UTF_8)
        : new byte[0];
  }

  /** Exige um cabeçalho sem revelar seu conteúdo na resposta ou no log. */
  private String requiredHeader(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    if (!StringUtils.hasText(value)) {
      reject(request, request.getHeader(REQUEST_ID_HEADER), "cabeçalho obrigatório ausente");
    }
    return value.trim();
  }

  /** Converte o timestamp textual sem aceitar outros formatos. */
  private long parseTimestamp(HttpServletRequest request, String requestId, String value) {
    if (!value.matches("^[0-9]{10}$")) {
      reject(request, requestId, "timestamp inválido");
    }
    return Long.parseLong(value);
  }

  /** Serializa o contrato reconhecido e rejeita falha interna sem prosseguir com a mutação. */
  private byte[] serializeBody(HttpServletRequest request, String requestId, Object body) {
    if (body == null) {
      return new byte[0];
    }
    try {
      return objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao serializar corpo para autenticação da Biblioteca requestId={} path={} errorLine={} errorClass={} errorMessage={}",
          requestId,
          request.getRequestURI(),
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível autenticar o contrato interno.", ex);
    }
  }

  /** Calcula a assinatura HMAC sem registrar o secret ou o material assinado. */
  private String hmac(String canonical) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
      log.error(
          "Falha criptográfica ao autenticar chamada interna da Biblioteca errorLine={} errorClass={} errorMessage={}",
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Assinatura interna indisponível.", ex);
    }
  }

  /** Calcula a impressão digital do corpo canônico recebido. */
  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException ex) {
      log.error(
          "SHA-256 indisponível na autenticação da Biblioteca errorLine={} errorClass={} errorMessage={}",
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "SHA-256 indisponível.", ex);
    }
  }

  /**
   * Localiza a primeira linha da stack para tornar a falha pesquisável sem perder o stack trace.
   */
  private static int errorLine(Throwable error) {
    return error.getStackTrace().length == 0 ? -1 : error.getStackTrace()[0].getLineNumber();
  }

  /** Compara hashes em tempo constante para reduzir vazamento por temporização. */
  private boolean constantTimeEquals(String left, String right) {
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.US_ASCII), right.getBytes(StandardCharsets.US_ASCII));
  }

  /** Registra contexto operacional não sensível e interrompe a chamada com 401. */
  private void reject(HttpServletRequest request, String requestId, String reason) {
    log.warn(
        "Chamada interna da Biblioteca rejeitada requestId={} method={} path={} reason={}",
        requestId,
        request.getMethod(),
        request.getRequestURI(),
        reason);
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura interna inválida.");
  }
}
