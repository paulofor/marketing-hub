package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Acrescenta escuta auditada à tarefa de Psique sem repetir chamadas de estado incerto. */
@Component
public class FinalVideoAudioReview {
  private static final Logger log = LoggerFactory.getLogger(FinalVideoAudioReview.class);
  private final ObjectMapper json;
  private final RestClient backend;
  private final HttpClient provider;
  private final String endpoint;
  private final String keyFile;
  private final String apiKey;
  private final Path state;

  /** Configura transporte com timeout, segredo externo e recuperação no volume persistente. */
  public FinalVideoAudioReview(
      ObjectMapper json,
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${CUSTOMER_AGENT_AUDIO_ENDPOINT:https://api.openai.com/v1/chat/completions}")
          String endpoint,
      @Value("${OPENAI_API_KEY_FILE:}") String keyFile,
      @Value("${OPENAI_API_KEY:}") String apiKey,
      @Value("${customer-agent.bpm-state-directory}") String directory) {
    this.json = json;
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.endpoint = endpoint;
    this.keyFile = keyFile;
    this.apiKey = apiKey;
    this.state = Path.of(directory).resolve("audio-v1");
    this.provider =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  /**
   * Analisa somente novas tarefas com contrato explícito; imagens mantêm comportamento anterior.
   */
  Map<String, Object> enrich(Map<String, Object> task, Path directory) throws Exception {
    if (!json.valueToTree(task.get("taskTarget")).at("/pdeContext/audioReviewRequired").asBoolean())
      return task;
    long taskId = ((Number) task.get("taskId")).longValue();
    JsonNode media = FinalVideoReviewInputs.inputs(json, task);
    if (media.isEmpty()) throw new IllegalStateException("Escuta solicitada sem vídeos.");
    var existing = backend.get().uri(base(taskId)).retrieve().body(JsonNode.class);
    var receipts = json.createArrayNode();
    var manifest = json.readTree(directory.resolve("audio-manifest.json").toFile());
    for (var video : media) {
      long assetId = video.path("assetId").asLong();
      JsonNode audio = null;
      for (var item : manifest) if (item.path("assetId").asLong() == assetId) audio = item;
      if (audio == null
          || !audio.path("videoSha256").asText().equals(video.path("sha256").asText()))
        throw new IllegalStateException("Mix de áudio não corresponde ao vídeo final.");
      JsonNode receipt = null;
      if (existing != null)
        for (var item : existing) if (item.path("assetId").asLong() == assetId) receipt = item;
      Path folder = state.resolve(Long.toString(taskId)).resolve(Long.toString(assetId));
      Files.createDirectories(folder);
      Path response = folder.resolve("response.json");
      if (receipt == null) {
        String secret = secret();
        Path wav = Path.of(audio.path("localPath").asText()).toRealPath();
        if (!wav.startsWith(directory.toRealPath()))
          throw new IllegalStateException("Mix fora da captura autorizada.");
        byte[] bytes = Files.readAllBytes(wav);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!sha.equals(audio.path("audioSha256").asText()))
          throw new IllegalStateException("Mix alterado antes da escuta.");
        JsonNode request = request(video, bytes);
        persist(folder.resolve("request.json"), request.toString());
        receipt =
            backend
                .post()
                .uri(base(taskId))
                .body(
                    Map.of(
                        "assetId",
                        assetId,
                        "videoSha256",
                        video.path("sha256").asText(),
                        "audioSha256",
                        sha,
                        "request",
                        request))
                .retrieve()
                .body(JsonNode.class);
        if (receipt != null && receipt.path("mayInvoke").asBoolean()) {
          // A reserva remota persiste antes do envio. Qualquer envio incerto bloqueia repetição.
          log.info(
              "Enviando escuta. taskId={} assetId={} endpoint={} reviewId={}",
              taskId,
              assetId,
              endpoint,
              receipt.path("id").asLong());
          HttpRequest http =
              HttpRequest.newBuilder(URI.create(endpoint))
                  .timeout(Duration.ofMinutes(3))
                  .header("Authorization", "Bearer " + secret)
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                  .build();
          var reply = provider.send(http, HttpResponse.BodyHandlers.ofString());
          persist(response, reply.body());
          log.info(
              "Resposta auditiva preservada. taskId={} assetId={} httpStatus={}",
              taskId,
              assetId,
              reply.statusCode());
        }
      }
      if (Files.isRegularFile(response)
          && receipt != null
          && !"COMPLETED".equals(receipt.path("status").asText())) {
        try {
          receipt =
              backend
                  .post()
                  .uri(base(taskId) + "/" + assetId + "/result")
                  .body(Map.of("rawResponse", Files.readString(response)))
                  .retrieve()
                  .body(JsonNode.class);
        } catch (RuntimeException ex) {
          log.error(
              "Callback auditivo pendente; resultado preservado sem nova inferência. taskId={} assetId={}",
              taskId,
              assetId,
              ex);
          throw new CallbackPendingException(ex);
        }
      }
      if (receipt == null || !"COMPLETED".equals(receipt.path("status").asText()))
        throw new IllegalStateException(
            "Escuta pendente ou incerta; recuperar/conferir custos sem nova inferência. taskId="
                + taskId
                + " assetId="
                + assetId);
      if (!receipt.path("videoSha256").asText().equals(video.path("sha256").asText())
          || !receipt.path("audioSha256").asText().equals(audio.path("audioSha256").asText()))
        throw new IllegalStateException("Recibo auditivo pertence a outra mídia.");
      receipts.add(receipt);
    }
    var result = new LinkedHashMap<>(task);
    result.put("audioReviews", receipts);
    return result;
  }

  /** Sinaliza transporte pendente para conservar a tarefa e recuperar o mesmo recibo. */
  static final class CallbackPendingException extends RuntimeException {
    /** Preserva a causa de integração e impede converter callback em nova chamada paga. */
    CallbackPendingException(Exception cause) {
      super("Callback da escuta pendente; resposta preservada.", cause);
    }
  }

  /** Monta request sem imagens, lendo prompt e schema versionados do próprio executor. */
  JsonNode request(JsonNode media, byte[] wav) throws Exception {
    String prompt =
        read("prompts/audio-review/v1/review.md")
            .replace("{{MEDIA}}", media.toString())
            .replace("{{SCHEMA}}", read("prompts/audio-review/v1/review-schema.json"));
    var request = json.createObjectNode();
    request.put("model", "gpt-audio-1.5");
    request.put("service_tier", "default");
    request.put("store", false);
    request.put("max_completion_tokens", 2000);
    request.putArray("modalities").add("text");
    var message = request.putArray("messages").addObject().put("role", "user");
    var content = message.putArray("content");
    content.addObject().put("type", "text").put("text", prompt);
    content
        .addObject()
        .put("type", "input_audio")
        .putObject("input_audio")
        .put("format", "wav")
        .put("data", Base64.getEncoder().encodeToString(wav));
    return request;
  }

  /** Carrega o segredo sem imprimir nem persistir credenciais em auditoria. */
  private String secret() throws Exception {
    String key = keyFile.isBlank() ? apiKey : Files.readString(Path.of(keyFile)).trim();
    if (key.isBlank())
      throw new IllegalStateException("Credencial da escuta de Psique indisponível.");
    return key;
  }

  /** Lê recursos canônicos do classpath, preservando sua versão. */
  private String read(String path) throws Exception {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Usa somente os endpoints do módulo de tarefas de Psique. */
  private String base(long taskId) {
    return "/api/internal/agent-tasks/customer-agent/stage-executions/" + taskId + "/audio-reviews";
  }

  /** Sincroniza bytes e troca arquivo atomicamente antes de enviar qualquer callback. */
  private void persist(Path target, String content) throws Exception {
    Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
    try (var file =
        FileChannel.open(
            temporary,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      var bytes = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
      while (bytes.hasRemaining()) file.write(bytes);
      file.force(true);
    }
    Files.move(
        temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
}
