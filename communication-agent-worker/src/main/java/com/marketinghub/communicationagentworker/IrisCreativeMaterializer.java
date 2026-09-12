package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: materializar e persistir peças privadas rastreáveis dentro da atividade de
 * Íris.
 */
@Component
public class IrisCreativeMaterializer {
  private static final Logger log = LoggerFactory.getLogger(IrisCreativeMaterializer.class);
  private static final String PREFIX =
      "/api/internal/agent-tasks/communication-director/stage-executions/";
  private final RestClient backend;
  private final ProofCardRenderer renderer;
  private final ObjectMapper json;

  /** Configura o acesso exclusivo ao backend e o renderizador determinístico. */
  public IrisCreativeMaterializer(
      CommunicationAgentProperties properties, ProofCardRenderer renderer, ObjectMapper json) {
    this.backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
    this.renderer = renderer;
    this.json = json;
  }

  /** Baixa uma prova aprovada para o modelo escolher um recorte dos pixels reais. */
  public Prepared prepare(Map<String, Object> task) throws IOException {
    if (!"creative-production-approval".equals(task.get("processCode"))
        || !"nonAudiovisual".equals(task.get("activityId"))) return new Prepared(null, List.of());
    long taskId = ((Number) task.get("taskId")).longValue();
    Path directory = Files.createTempDirectory("iris-creative-" + taskId + "-");
    Prepared prepared = new Prepared(directory, new ArrayList<>());
    try {
      String endpoint = PREFIX + taskId + "/visual-inputs";
      JsonNode inputs = backend.get().uri(endpoint).retrieve().body(JsonNode.class);
      if (inputs == null || !inputs.isArray() || inputs.isEmpty())
        throw new IllegalStateException("Nenhuma prova visual aprovada disponível para Íris.");
      JsonNode selected = inputs.get(0);
      for (JsonNode input : inputs)
        if ("DESKTOP_1440".equals(input.path("evidence").path("deviceProfile").asText())) {
          selected = input;
          break;
        }
      JsonNode evidence = selected.path("evidence");
      String content =
          endpoint
              + "/"
              + selected.path("sourceTaskId").asLong()
              + "/"
              + evidence.path("id").asLong()
              + "/content";
      log.info(
          "Baixando prova aprovada para Íris. taskId={} endpoint={} sha256={}",
          taskId,
          content,
          evidence.path("sha256").asText());
      byte[] bytes = backend.get().uri(content).retrieve().body(byte[].class);
      if (bytes == null || !sha(bytes).equals(evidence.path("sha256").asText()))
        throw new IllegalStateException("Hash da prova de Íris não corresponde ao backend.");
      Path file = directory.resolve("source-" + evidence.path("id").asLong() + ".png");
      Files.write(file, bytes);
      var pixels = ImageIO.read(file.toFile());
      if (pixels == null) throw new IllegalStateException("Origem criativa não é PNG válido.");
      ObjectNode metadata = selected.deepCopy();
      metadata.put("pixelWidth", pixels.getWidth());
      metadata.put("pixelHeight", pixels.getHeight());
      prepared.inputs().add(new Input(metadata, file));
      task.put("approvedVisualInputs", prepared.inputs().stream().map(Input::metadata).toList());
      log.info(
          "Prova visual recebida por Íris. taskId={} artifactId={} bytes={}",
          taskId,
          evidence.path("id").asLong(),
          bytes.length);
      return prepared;
    } catch (Exception ex) {
      log.error("Falha ao preparar fonte criativa. taskId={}", taskId, ex);
      prepared.close();
      throw ex;
    }
  }

  /**
   * Converte o briefing em PNGs antes do callback e preserva a resposta bruta fora do artefato
   * funcional.
   */
  public JsonNode materialize(Map<String, Object> task, JsonNode result, Prepared prepared)
      throws IOException {
    if (prepared.directory() == null) return result;
    ObjectNode output = result.deepCopy();
    ObjectNode functional = (ObjectNode) output.path("functionalOutput");
    var renders = functional.putArray("renderedAssets");
    JsonNode context = json.readTree(String.valueOf(task.getOrDefault("processContextJson", "{}")));
    boolean privateValidation =
        "LEARNING_CYCLE_PRIVATE"
            .equals(context.path("communicationMaterializationContext").path("mode").asText());
    // O contrato privado também é reconhecido pelo alvo para não depender do rótulo de UI.
    privateValidation =
        privateValidation
            || !context
                .path("communicationMaterializationContext")
                .path("privatePrototypeAcceptance")
                .isMissingNode();
    long taskId = ((Number) task.get("taskId")).longValue();
    String session = UUID.randomUUID().toString();
    int index = 0;
    for (JsonNode asset : functional.path("staticAssets")) {
      JsonNode spec = asset.path("renderSpec");
      Input source =
          prepared.inputs().stream()
              .filter(
                  i ->
                      i.metadata().path("evidence").path("id").asLong()
                              == spec.path("sourceArtifactId").asLong()
                          && i.metadata()
                              .path("evidence")
                              .path("sha256")
                              .asText()
                              .equals(spec.path("sourceSha256").asText()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "O briefing não identifica a prova visual aprovada."));
      byte[] bytes = renderer.render(spec, Files.readAllBytes(source.path()), privateValidation);
      Path file = prepared.directory().resolve("creative-" + (++index) + ".png");
      Files.write(file, bytes);
      var body = new LinkedMultiValueMap<String, Object>();
      body.add("captureSessionId", session);
      body.add("evidenceKey", "creative-" + index);
      body.add("evidenceType", "CREATIVE_RENDER");
      body.add("deviceProfile", "CREATIVE_1080X1350");
      body.add("pageNumber", String.valueOf(index));
      body.add("viewportWidth", "1080");
      body.add("viewportHeight", "1350");
      body.add("pageHeightPx", "1350");
      body.add("scrollY", "0");
      body.add("sourceUrl", source.metadata().path("evidence").path("sourceUrl").asText());
      body.add("finalUrl", source.metadata().path("evidence").path("finalUrl").asText());
      body.add("capturedAt", Instant.now().toString());
      body.add("file", new FileSystemResource(file));
      String endpoint = PREFIX + taskId + "/visual-evidence";
      log.info(
          "Enviando criativo privado. taskId={} endpoint={} sha256={} sourceArtifactId={}",
          taskId,
          endpoint,
          sha(bytes),
          spec.path("sourceArtifactId").asLong());
      JsonNode saved =
          backend
              .post()
              .uri(endpoint)
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      if (saved == null
          || saved.path("id").asLong() < 1
          || !sha(bytes).equals(saved.path("sha256").asText()))
        throw new IllegalStateException("O backend não confirmou a imagem renderizada.");
      ObjectNode rendered = renders.addObject();
      rendered.put("artifactId", saved.path("id").asLong());
      rendered.put("contentUrl", saved.path("contentUrl").asText());
      rendered.put("sha256", saved.path("sha256").asText());
      rendered.put("width", 1080);
      rendered.put("height", 1350);
      rendered.put("templateVersion", "PROOF_CARD_V1");
      rendered.put("sourceTaskId", source.metadata().path("sourceTaskId").asLong());
      rendered.put("sourceArtifactId", spec.path("sourceArtifactId").asLong());
      rendered.put("sourceSha256", spec.path("sourceSha256").asText());
      rendered.put("prototypeVersion", source.metadata().path("prototypeVersion").asText());
      rendered.set("crop", spec.path("crop"));
      rendered.put("privateValidation", privateValidation);
      log.info(
          "Criativo privado persistido. taskId={} artifactId={} sha256={}",
          taskId,
          saved.path("id").asLong(),
          saved.path("sha256").asText());
    }
    if (renders.isEmpty())
      throw new IllegalArgumentException("Íris não produziu nenhuma peça estática.");
    return output;
  }

  /** Calcula a identidade dos bytes recebidos e publicados no storage privado. */
  static String sha(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível no renderizador de Íris.", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Responsabilidade: associar um arquivo efêmero aos metadados autorizados pelo backend. */
  record Input(JsonNode metadata, Path path) {}

  /** Responsabilidade: manter as fontes locais durante a tarefa e removê-las após o callback. */
  public record Prepared(Path directory, List<Input> inputs) implements AutoCloseable {
    /** Entrega somente os arquivos de origem ao modelo como imagens anexadas. */
    List<Path> paths() {
      return inputs.stream().map(Input::path).toList();
    }

    /** Remove os arquivos efêmeros sem afetar os artefatos auditáveis do backend. */
    @Override
    public void close() throws IOException {
      if (directory == null) return;
      try (var files = Files.walk(directory)) {
        for (Path file : files.sorted(Comparator.reverseOrder()).toList())
          Files.deleteIfExists(file);
      }
    }
  }
}
