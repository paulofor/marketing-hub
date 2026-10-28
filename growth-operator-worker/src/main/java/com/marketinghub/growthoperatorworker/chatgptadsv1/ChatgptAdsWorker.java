package com.marketinghub.growthoperatorworker.chatgptadsv1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.growthoperatorworker.WorkerProperties;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Executa requests de anúncios decididos pelo backend e recupera recibos sem duplicar publicação.
 */
@Component
public class ChatgptAdsWorker {
  private static final Logger log = LoggerFactory.getLogger(ChatgptAdsWorker.class);
  private static final String QUEUE = "/api/internal/chatgpt-ads/v1/delivery/stage-executions";
  private final ObjectMapper mapper;
  private final HttpClient client;
  private final String backend;
  private final String provider;
  private final Path directory;

  /** Usa credenciais dedicadas protegidas fora do repositório e API oficial fixa. */
  @org.springframework.beans.factory.annotation.Autowired
  public ChatgptAdsWorker(
      WorkerProperties properties,
      ObjectMapper mapper,
      @Value("${chatgpt-ads.directory:/home/operator/.codex/chatgpt-ads}") String directory) {
    this(properties.getBackendUrl(), "https://api.ads.openai.com/v1", Path.of(directory), mapper);
  }

  /** Permite homologação determinística com provedor e backend locais isolados. */
  ChatgptAdsWorker(String backend, String provider, Path directory, ObjectMapper mapper) {
    this.backend = backend.replaceAll("/+$", "");
    this.provider = provider;
    this.directory = directory;
    this.mapper = mapper;
    this.client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  /** Consulta a fila em cadência própria; nenhuma chamada é feita sem credencial interna. */
  @Scheduled(cron = "*/30 * * * * *")
  public synchronized void poll() {
    if (!Files.isRegularFile(directory.resolve("worker-token"))) return;
    Long operationId = null;
    try {
      String internal = readSecret("worker-token");
      HttpResponse<String> pending =
          send(backend + QUEUE + "/pending", "POST", mapper.createObjectNode(), internal, null);
      if (pending.statusCode() == 204) return;
      if (pending.statusCode() != 200)
        throw safeFailure("Fila ChatGPT Ads indisponível HTTP " + pending.statusCode());
      JsonNode work = mapper.readTree(pending.body());
      operationId = work.path("id").longValue();
      if (operationId <= 0 || !work.path("lease").isTextual())
        throw safeFailure("Reserva inválida");
      Path receiptFile = directory.resolve("receipt-" + operationId + ".json");
      ObjectNode receipt;
      if (Files.isRegularFile(receiptFile))
        receipt = (ObjectNode) mapper.readTree(Files.readString(receiptFile));
      else {
        receipt = execute(work);
        Path temporary = Files.createTempFile(directory, "receipt-", ".tmp");
        Files.writeString(temporary, receipt.toString(), StandardCharsets.UTF_8);
        Files.move(
            temporary,
            receiptFile,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      }
      receipt.put("lease", work.path("lease").asText());
      HttpResponse<String> callback =
          send(backend + QUEUE + "/" + operationId + "/result", "POST", receipt, internal, null);
      if (callback.statusCode() / 100 != 2)
        throw safeFailure("Callback ChatGPT Ads recusado HTTP " + callback.statusCode());
      Files.move(
          receiptFile,
          directory.resolve("acknowledged-receipt-" + operationId + ".json"),
          StandardCopyOption.REPLACE_EXISTING);
      log.info(
          "ChatGPT Ads operação={} callback={} recibo preservado",
          operationId,
          callback.statusCode());
    } catch (Exception ex) {
      // Nunca propaga mensagens da biblioteca HTTP que possam incluir credenciais ou payloads.
      log.error(
          "ChatGPT Ads operação={} falha de entrega tipo={}",
          operationId,
          ex.getClass().getSimpleName(),
          sanitized(ex));
    }
  }

  /** Executa somente caminhos do contrato, usando a mesma chave em toda recuperação. */
  private ObjectNode execute(JsonNode work) throws Exception {
    ObjectNode receipt = mapper.createObjectNode();
    if (!Files.isRegularFile(directory.resolve("advertiser-key"))) {
      receipt.put("httpStatus", 0);
      receipt.put("errorCode", "CREDENTIAL_MISSING");
      receipt.set("response", mapper.createObjectNode());
      return receipt;
    }
    String key = readSecret("advertiser-key");
    String path = work.path("path").asText();
    if (!work.path("kind").asText().equals("ACCOUNT")) {
      var accountResponse =
          send(provider + "/ad_account", "GET", mapper.createObjectNode(), key, null);
      var account =
          accountResponse.statusCode() == 200
              ? mapper.readTree(accountResponse.body())
              : mapper.createObjectNode();
      if (!work.path("accountId").isTextual()
          || !work.path("accountId").asText().equals(account.path("id").asText())) {
        receipt.put("httpStatus", 409);
        receipt.put("errorCode", "ACCOUNT_CHANGED_OR_UNAVAILABLE");
        receipt.set("response", mapper.createObjectNode());
        return receipt;
      }
    }
    String method = work.path("method").asText();
    if (!path.matches(
            "/(ad_account|campaigns|ad_groups|upload|ads)([/?][A-Za-z0-9_./?=&%\\[\\]-]*)?")
        || !(method.equals("GET") || method.equals("POST")))
      throw safeFailure("Caminho não permitido no contrato");
    String url = provider + path.replace("[", "%5B").replace("]", "%5D");
    log.info(
        "ChatGPT Ads operação={} URL={} método={} request={}",
        work.path("id").asLong(),
        url,
        method,
        work.path("body"));
    HttpResponse<String> response =
        send(url, method, work.path("body"), key, work.path("idempotencyKey").asText());
    receipt.put("httpStatus", response.statusCode());
    JsonNode payload;
    try {
      payload = mapper.readTree(response.body());
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      log.warn(
          "ChatGPT Ads operação={} resposta não JSON", work.path("id").asLong(), sanitized(ex));
      payload = mapper.createObjectNode();
      receipt.put("errorCode", "INVALID_RESPONSE");
    }
    JsonNode safe = redact(payload == null ? mapper.createObjectNode() : payload, key);
    receipt.set("response", safe);
    if (response.statusCode() / 100 != 2) receipt.put("errorCode", "HTTP_" + response.statusCode());
    log.info(
        "ChatGPT Ads operação={} URL={} status={} response={}",
        work.path("id").asLong(),
        url,
        response.statusCode(),
        safe);
    return receipt;
  }

  /** Executa HTTP com limites, sem redirecionar credenciais para outros hosts. */
  private HttpResponse<String> send(
      String url, String method, JsonNode body, String token, String idempotency) throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("Accept", "application/json");
    String requestNonce = null;
    if (url.startsWith(backend + QUEUE)) {
      String time = Long.toString(java.time.Instant.now().getEpochSecond()),
          nonce = java.util.UUID.randomUUID().toString();
      requestNonce = nonce;
      String digest =
          java.util.HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(body.toString().getBytes(StandardCharsets.UTF_8)));
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(
          new javax.crypto.spec.SecretKeySpec(
              token.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String signature =
          java.util.HexFormat.of()
              .formatHex(
                  mac.doFinal(
                      (method
                              + "\n"
                              + URI.create(url).getRawPath()
                              + "\n"
                              + time
                              + "\n"
                              + nonce
                              + "\n"
                              + digest)
                          .getBytes(StandardCharsets.UTF_8)));
      builder
          .header("X-Mkt-Ads-Time", time)
          .header("X-Mkt-Ads-Nonce", nonce)
          .header("X-Mkt-Ads-Signature", signature);
    } else builder.header("Authorization", "Bearer " + token);
    if (idempotency != null && !idempotency.isBlank())
      builder.header("Idempotency-Key", idempotency);
    if (method.equals("GET")) builder.GET();
    else
      builder
          .header("Content-Type", "application/json; charset=UTF-8")
          .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
    var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.body().length() > 262144) throw safeFailure("Resposta excedeu o limite do recibo");
    if (requestNonce != null) {
      String signature = response.headers().firstValue("X-Mkt-Ads-Response").orElse("");
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(
          new javax.crypto.spec.SecretKeySpec(
              token.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String digest =
          java.util.HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(response.body().getBytes(StandardCharsets.UTF_8)));
      byte[] expected =
          mac.doFinal(
              ("RESPONSE\n" + response.statusCode() + "\n" + requestNonce + "\n" + digest)
                  .getBytes(StandardCharsets.UTF_8));
      if (!signature.matches("[a-f0-9]{64}")
          || !java.security.MessageDigest.isEqual(
              expected, java.util.HexFormat.of().parseHex(signature)))
        throw safeFailure("Resposta do backend sem assinatura válida");
    }
    return response;
  }

  /** Lê segredo local sem incluí-lo em request persistido ou mensagem de erro. */
  private String readSecret(String name) throws Exception {
    String value = Files.readString(directory.resolve(name)).trim();
    if (value.isEmpty() || value.contains("\n") || value.length() > 8192)
      throw safeFailure("Credencial ausente ou inválida");
    return value;
  }

  /** Mascara campos sensíveis e qualquer eco literal da credencial antes da auditoria. */
  private JsonNode redact(JsonNode source, String secret) {
    if (source.isObject()) {
      ObjectNode result = mapper.createObjectNode();
      source
          .fields()
          .forEachRemaining(
              e ->
                  result.set(
                      e.getKey(),
                      e.getKey()
                              .toLowerCase()
                              .matches(".*(token|secret|authorization|api.?key|password).*")
                          ? mapper.getNodeFactory().textNode("[REDACTED]")
                          : redact(e.getValue(), secret)));
      return result;
    }
    if (source.isArray()) {
      var a = mapper.createArrayNode();
      source.forEach(v -> a.add(redact(v, secret)));
      return a;
    }
    return source.isTextual()
        ? mapper.getNodeFactory().textNode(source.asText().replace(secret, "[REDACTED]"))
        : source;
  }

  /** Preserva o stack trace operacional sem mensagens ou causas contendo dados externos. */
  private Exception sanitized(Exception ex) {
    var safe = new Exception(ex.getClass().getSimpleName());
    safe.setStackTrace(ex.getStackTrace());
    return safe;
  }

  /** Cria falha operacional segura sem incluir corpo ou segredo. */
  private IllegalStateException safeFailure(String message) {
    return new IllegalStateException(message);
  }
}
