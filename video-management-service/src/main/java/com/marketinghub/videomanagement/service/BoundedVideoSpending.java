package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.nio.file.*;
import java.util.Base64;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: admitir uma chamada finita de vídeo e recuperar seu resultado sem repetir
 * gasto.
 */
@Component
public class BoundedVideoSpending {
  private static final Logger log = LoggerFactory.getLogger(BoundedVideoSpending.class);
  private final BackendVideoClient backend;
  private final ObjectMapper json;
  private final Path state;

  /**
   * Usa o volume persistente de Apolo; nenhum resultado depende de diretório temporário de
   * montagem.
   */
  @Autowired
  public BoundedVideoSpending(BackendVideoClient backend, ObjectMapper json) {
    this(
        backend,
        json,
        Path.of(
            System.getenv()
                .getOrDefault(
                    "VIDEO_BOUNDED_SPEND_STATE_DIR",
                    System.getenv().getOrDefault("CODEX_HOME", "/var/lib/apollo")
                        + "/marketing-hub-bounded-video")));
  }

  /** Permite testar recuperação no workspace com o mesmo código do transporte de produção. */
  BoundedVideoSpending(BackendVideoClient backend, ObjectMapper json, Path state) {
    this.backend = backend;
    this.json = json;
    this.state = state;
  }

  /** Consulta o recibo e o modelo explícito de voz sem inferência nem alteração do teto. */
  public JsonNode context(Long jobId, String purpose) {
    var context = backend.spending(jobId, purpose, "", null);
    if (context == null || !context.path("required").isBoolean())
      throw blocked("Autoridade de gasto sem resposta válida");
    return context;
  }

  /** Recupera somente resposta já produzida, sem admissão de envio ou dependência de credencial. */
  public JsonNode replayOnly(Long jobId, String purpose) {
    var context = context(jobId, purpose);
    var lease = context.path("reservation");
    if (!context.path("required").asBoolean() || !lease.isObject()) return null;
    Path file =
        state.resolve(context.path("receiptId").asLong() + "-" + jobId + "-" + purpose + ".json");
    try {
      String raw = lease.path("rawResponse").asText(null);
      if (raw == null
          && (Files.exists(file)
              || Files.exists(file.resolveSibling(file.getFileName() + ".pending")))) {
        var saved = json.readTree(Files.readString(recoverableFile(file)));
        if (saved.path("reservationId").asLong() != lease.path("id").asLong()
            || !saved.path("requestSha256").equals(lease.path("requestSha256")))
          throw blocked("Resultado local pertence a outra reserva");
        backend.spending(
            jobId, purpose, "/" + lease.path("id").asLong() + "/result", saved.path("result"));
        raw = saved.at("/result/rawResponse").asText(null);
      }
      if (raw == null) return null;
      var result = json.createObjectNode().set("request", lease.path("request"));
      ((com.fasterxml.jackson.databind.node.ObjectNode) result).set("response", json.readTree(raw));
      return result;
    } catch (java.io.IOException | RuntimeException ex) {
      log.error(
          "Resposta preservada não pôde ser reaplicada; jobId={} purpose={}", jobId, purpose, ex);
      throw blocked("Resposta preservada exige conciliação; não gerar outra task");
    }
  }

  /** Conta a entrada imutável somente quando ainda não existe uma reserva recuperável. */
  public JsonNode jsonCall(
      Long jobId,
      String purpose,
      JsonNode request,
      Supplier<JsonNode> tokenCount,
      Supplier<JsonNode> provider) {
    var context = context(jobId, purpose);
    if (!context.path("required").asBoolean()) return provider.get();
    JsonNode existing = context.path("reservation");
    boolean renew =
        "RESERVED".equals(existing.path("state").asText())
            && java.time.Instant.parse(existing.path("quoteExpiresAt").asText())
                .isBefore(java.time.Instant.now());
    JsonNode count =
        (!context.hasNonNull("reservation") || renew) && tokenCount != null
            ? tokenCount.get()
            : null;
    return guarded(jobId, purpose, context, request, count, provider);
  }

  /** Preserva bytes de voz no volume e no backend antes do callback funcional da pós-produção. */
  public ResponseEntity<byte[]> binaryCall(
      Long jobId, String purpose, JsonNode request, Supplier<ResponseEntity<byte[]>> provider) {
    JsonNode result =
        jsonCall(
            jobId,
            purpose,
            request,
            null,
            () -> {
              var reply = provider.get();
              if (reply == null
                  || !reply.getStatusCode().is2xxSuccessful()
                  || reply.getBody() == null)
                throw blocked("Provedor não confirmou a resposta de voz; reserva preservada");
              return json.createObjectNode()
                  .put("status", reply.getStatusCode().value())
                  .put("contentType", String.valueOf(reply.getHeaders().getContentType()))
                  .put("requestId", reply.getHeaders().getFirst("x-request-id"))
                  .put("base64", Base64.getEncoder().encodeToString(reply.getBody()));
            });
    var headers = new HttpHeaders();
    if (!result.path("contentType").asText().equals("null"))
      headers.setContentType(MediaType.parseMediaType(result.path("contentType").asText()));
    if (result.hasNonNull("requestId"))
      headers.set("x-request-id", result.path("requestId").asText());
    return new ResponseEntity<>(
        Base64.getDecoder().decode(result.path("base64").asText()),
        headers,
        HttpStatusCode.valueOf(result.path("status").asInt()));
  }

  /**
   * Recupera também gravação íntegra interrompida antes do rename; arquivo parcial continua
   * bloqueado.
   */
  private Path recoverableFile(Path file) throws java.io.IOException {
    if (Files.exists(file)) return file;
    Path pending = file.resolveSibling(file.getFileName() + ".pending");
    json.readTree(Files.readString(pending));
    Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    return file;
  }

  /**
   * Reserva, grava intenção e envia no máximo uma vez; callback perdido é recuperado pelo mesmo ID.
   */
  private JsonNode guarded(
      Long jobId,
      String purpose,
      JsonNode context,
      JsonNode request,
      JsonNode count,
      Supplier<JsonNode> provider) {
    if (context.hasNonNull("blocker") && !context.hasNonNull("reservation"))
      throw blocked(context.path("blocker").asText());
    var lease = context.path("reservation");
    boolean renew =
        "RESERVED".equals(lease.path("state").asText())
            && java.time.Instant.parse(lease.path("quoteExpiresAt").asText())
                .isBefore(java.time.Instant.now());
    if (lease.isMissingNode() || lease.isNull() || renew) {
      var input = json.createObjectNode().set("request", request);
      ((com.fasterxml.jackson.databind.node.ObjectNode) input).set("inputTokenCount", count);
      lease = backend.spending(jobId, purpose, "/reserve", input);
    }
    if (lease == null || !lease.path("request").equals(request))
      throw blocked("Request diverge da reserva persistida");
    String suffix = "/" + lease.path("id").asLong();
    Path file =
        state.resolve(context.path("receiptId").asLong() + "-" + jobId + "-" + purpose + ".json");
    try {
      Files.createDirectories(state);
      if (Files.exists(file)
          || Files.exists(file.resolveSibling(file.getFileName() + ".pending"))) {
        var saved = json.readTree(Files.readString(recoverableFile(file)));
        if (saved.path("reservationId").asLong() != lease.path("id").asLong()
            || !saved.path("requestSha256").equals(lease.path("requestSha256")))
          throw blocked("Resultado preservado pertence a outra reserva");
        backend.spending(jobId, purpose, suffix + "/result", saved.path("result"));
        return json.readTree(saved.at("/result/rawResponse").asText());
      }
      if (lease.hasNonNull("rawResponse")) return json.readTree(lease.path("rawResponse").asText());
      var admission = backend.spending(jobId, purpose, suffix + "/begin", json.createObjectNode());
      if (admission.hasNonNull("rawResponse"))
        return json.readTree(admission.path("rawResponse").asText());
      if (!admission.path("mayInvoke").asBoolean())
        throw blocked("Envio anterior incerto; não repetir chamada paga");
      if (!admission.path("request").equals(request))
        throw blocked("Request alterado depois da reserva");
      JsonNode result = provider.get();
      var receipt =
          json.createObjectNode().put("httpStatus", 200).put("rawResponse", result.toString());
      var saved =
          json.createObjectNode()
              .put("reservationId", lease.path("id").asLong())
              .put("requestSha256", lease.path("requestSha256").asText())
              .set("result", receipt);
      Path temporary = file.resolveSibling(file.getFileName() + ".pending");
      Files.writeString(temporary, saved.toString());
      Files.move(
          temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      backend.spending(jobId, purpose, suffix + "/result", receipt);
      return result;
    } catch (java.io.IOException | RuntimeException ex) {
      log.error(
          "Consumo limitado interrompido; jobId={} purpose={} reservationId={}; não repetir envio",
          jobId,
          purpose,
          lease.path("id").asLong(),
          ex);
      throw new VideoProviderException(
          "SINGLE_VIDEO_RECONCILIATION_REQUIRED",
          "Resposta ou envio exige conciliação; reserva mantida, sem nova geração",
          ex);
    }
  }

  /** Mantém a recusa financeira explícita e preserva todo resultado já registrado. */
  private VideoProviderException blocked(String reason) {
    return new VideoProviderException("SINGLE_VIDEO_SPEND_BLOCKED", reason);
  }
}
