package com.marketinghub.payments.service.kit.privateprototype.v1;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.service.AgendaCheiaKitProductionService;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: consumir reservas privadas do backend e devolver arquivos sem inferência ou
 * venda.
 */
@Component
public class PrivateKitCompositionWorker {
  private static final Logger log = LoggerFactory.getLogger(PrivateKitCompositionWorker.class);
  private static final String ROOT = "/api/pde/kit/private/v1/stage-executions";
  private final AgendaCheiaKitProductionService composer;
  private final ObjectMapper json;
  private final RestClient client;
  private final Path replay;
  private final boolean configured;

  /**
   * Reutiliza autenticação, backend e volume existentes; não conecta o worker ao banco principal.
   */
  public PrivateKitCompositionWorker(
      AgendaCheiaKitProductionService composer,
      ObjectMapper json,
      @Value("${product-ai.delivery.backend-base-url:http://191.252.181.168}") String backend,
      @Value("${payments.admin-auth-token:}") String token,
      @Value("${agenda-cheia.production.storage-root:/data/agenda-cheia}") String storage,
      @Value("${private-kit.enabled:true}") boolean enabled) {
    this.composer = composer;
    this.json = json;
    this.replay = Path.of(storage).toAbsolutePath().normalize().resolve("private-callbacks-v1");
    this.configured = enabled && !token.isBlank();
    var http =
        java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .build();
    var factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(java.time.Duration.ofSeconds(60));
    this.client =
        RestClient.builder()
            .baseUrl(backend.replaceAll("/+$", ""))
            .defaultHeader("X-Payments-Auth", token)
            .requestFactory(factory)
            .build();
  }

  /**
   * Reaplica callbacks preservados antes de buscar trabalho novo, evitando composição duplicada.
   */
  @Scheduled(cron = "*/10 * * * * *")
  public synchronized void poll() {
    if (!configured) return;
    try {
      Files.createDirectories(replay);
      try (var files = Files.list(replay)) {
        for (var file :
            files
                .filter(p -> p.getFileName().toString().matches("[a-f0-9-]{36}\\.json"))
                .sorted()
                .toList()) process(file);
      }
      JsonNode pending = client.get().uri(ROOT + "/pending").retrieve().body(JsonNode.class);
      if (pending == null || !pending.isArray())
        throw new IllegalStateException("Fila privada inválida");
      for (var task : pending) {
        String id = task.path("id").asText();
        if (!id.matches("[a-f0-9-]{36}"))
          throw new IllegalStateException("Identificador inválido na fila privada");
        Path file = replay.resolve(id + ".json");
        if (!Files.exists(file)) {
          var job =
              json.createObjectNode().put("id", id).put("claimKey", UUID.randomUUID().toString());
          atomic(file, json.writeValueAsBytes(job));
        }
        process(file);
      }
    } catch (Exception ex) {
      log.error(
          "Compositor privado: consulta ou recuperação da fila falhou endpoint={}",
          ROOT + "/pending",
          ex);
    }
  }

  /** Mantém claim e ZIP duráveis até o backend confirmar o resultado ou a falha terminal. */
  private void process(Path file) {
    String id = file.getFileName().toString().replace(".json", "");
    try {
      var job =
          (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(Files.readAllBytes(file));
      String claim = job.path("claimKey").asText();
      if (job.has("failure")) {
        sendFailure(id, claim, job.path("failure").asText());
        Files.delete(file);
        return;
      }
      if (!job.has("zipPath")) {
        JsonNode task;
        try {
          task =
              client
                  .post()
                  .uri(ROOT + "/" + id + "/claim")
                  .body(Map.of("claimKey", claim))
                  .retrieve()
                  .body(JsonNode.class);
        } catch (HttpClientErrorException ex) {
          log.warn(
              "Compositor privado: reserva não disponível para este consumidor artifactId={}"
                  + " endpoint={}",
              id,
              ROOT + "/" + id + "/claim",
              ex);
          if (ex.getStatusCode().value() == 409 || ex.getStatusCode().value() == 404) {
            Files.delete(file);
            return;
          }
          throw ex;
        }
        try {
          var input = task.path("input");
          AgendaCheiaBriefing briefing = new AgendaCheiaBriefing();
          briefing.setBuyerEmail(input.path("email").asText());
          briefing.setProfessionalName(input.path("professionalName").asText());
          briefing.setCityRegion(input.path("cityRegion").asText());
          briefing.setWhatsapp(input.path("whatsapp").asText());
          briefing.setServices(input.path("services").asText());
          briefing.setVisualStyle(input.path("visualStyle").asText());
          briefing.setWeeklyGoal(input.path("weeklyGoal").asText());
          briefing.setPreferredColors(input.path("preferredColors").asText());
          briefing.setNotes(input.path("notes").asText());
          var prepared =
              composer.preparePrivateCandidate(briefing, task.path("profileCode").asText(), id);
          job.put("zipPath", prepared.zipPath().toString());
          atomic(file, json.writeValueAsBytes(job));
          log.info(
              "Compositor privado: arquivo preservado artifactId={} cycleId={} version={}"
                  + " providerCalls=0",
              id,
              task.path("cycleId"),
              task.path("prototypeVersion").asText());
        } catch (Exception ex) {
          log.error("Compositor privado: composição falhou artifactId={}", id, ex);
          job.put(
              "failure",
              "Não foi possível compor o arquivo privado. Consulte a execução do compositor.");
          atomic(file, json.writeValueAsBytes(job));
          sendFailure(id, claim, job.path("failure").asText());
          Files.delete(file);
          return;
        }
      }
      Path zip = Path.of(job.path("zipPath").asText()).toAbsolutePath().normalize();
      if (!zip.getParent().equals(replay.getParent())
          || !zip.getFileName().toString().equals("agenda-cheia-private-" + id + ".zip"))
        throw new IllegalStateException("Caminho de replay não pertence à composição");
      byte[] bytes = Files.readAllBytes(zip);
      String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      try {
        client
            .post()
            .uri(ROOT + "/" + id + "/result")
            .body(
                Map.of(
                    "claimKey",
                    claim,
                    "zipBase64",
                    Base64.getEncoder().encodeToString(bytes),
                    "zipSha256",
                    sha,
                    "providerCalls",
                    0))
            .retrieve()
            .toBodilessEntity();
      } catch (HttpClientErrorException ex) {
        log.error(
            "Compositor privado: resultado recusado funcionalmente artifactId={} endpoint={}",
            id,
            ROOT + "/" + id + "/result",
            ex);
        if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 409) {
          job.put(
              "failure",
              "O backend recusou o contrato do arquivo privado. O ZIP original foi preservado para"
                  + " diagnóstico.");
          atomic(file, json.writeValueAsBytes(job));
          sendFailure(id, claim, job.path("failure").asText());
          Files.delete(file);
          return;
        }
        throw ex;
      }
      Files.delete(file);
      Files.deleteIfExists(zip);
      log.info(
          "Compositor privado: callback aceito artifactId={} sha256={} bytes={} providerCalls=0",
          id,
          sha,
          bytes.length);
    } catch (Exception ex) {
      log.error(
          "Compositor privado: callback preservado para replay artifactId={} endpoint={}",
          id,
          ROOT + "/" + id,
          ex);
    }
  }

  /** Envia a falha da tentativa reservada sem pedir outra inferência ou renovar limite. */
  private void sendFailure(String id, String claim, String error) {
    client
        .post()
        .uri(ROOT + "/" + id + "/failure")
        .body(Map.of("claimKey", claim, "error", error))
        .retrieve()
        .toBodilessEntity();
  }

  /** Grava metadados de recuperação por renomeação atômica no volume do compositor. */
  private void atomic(Path destination, byte[] bytes) throws java.io.IOException {
    Path temp = destination.resolveSibling(destination.getFileName() + ".tmp");
    Files.write(temp, bytes);
    Files.move(
        temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
}
