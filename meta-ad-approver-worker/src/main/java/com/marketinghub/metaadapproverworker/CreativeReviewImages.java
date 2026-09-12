package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: entregar a Têmis os pixels exatos das peças persistidas para revisão
 * independente.
 */
final class CreativeReviewImages implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(CreativeReviewImages.class);
  private final Path directory;
  private final List<Path> paths = new ArrayList<>();
  private final List<JsonNode> metadata = new ArrayList<>();

  /** Reserva o diretório efêmero somente para a tarefa em revisão. */
  private CreativeReviewImages(Path directory) {
    this.directory = directory;
  }

  /**
   * Baixa imagens exclusivamente pelo contrato reservado do backend e confere a identidade dos
   * pixels.
   */
  static CreativeReviewImages load(RestClient backend, Map<String, Object> task)
      throws IOException {
    if (!"creative-production-approval".equals(task.get("processCode")))
      return new CreativeReviewImages(null);
    long id = ((Number) task.get("taskId")).longValue();
    var result = new CreativeReviewImages(Files.createTempDirectory("temis-creative-" + id + "-"));
    String endpoint =
        "/api/internal/agent-tasks/meta-ad-approver/stage-executions/" + id + "/visual-inputs";
    try {
      var inputs = backend.get().uri(endpoint).retrieve().body(JsonNode.class);
      if (inputs == null || !inputs.isArray() || inputs.isEmpty())
        throw new IllegalStateException("Peça final indisponível para Têmis.");
      for (JsonNode input : inputs) {
        var evidence = input.path("evidence");
        if (!"CREATIVE_RENDER".equals(evidence.path("evidenceType").asText()))
          throw new IllegalStateException("O gate comercial exige a peça final.");
        String content =
            endpoint
                + "/"
                + input.path("sourceTaskId").asLong()
                + "/"
                + evidence.path("id").asLong()
                + "/content";
        log.info("Baixando peça para Têmis. taskId={} endpoint={}", id, content);
        byte[] bytes = backend.get().uri(content).retrieve().body(byte[].class);
        if (bytes == null
            || !HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                .equals(evidence.path("sha256").asText()))
          throw new IllegalStateException("O hash da peça recebida por Têmis está incorreto.");
        Path file = result.directory.resolve("creative-" + evidence.path("id").asLong() + ".png");
        Files.write(file, bytes);
        var pixels = javax.imageio.ImageIO.read(file.toFile());
        if (pixels == null || pixels.getWidth() != 1080 || pixels.getHeight() != 1350)
          throw new IllegalStateException("Peça final inválida.");
        result.paths.add(file);
        result.metadata.add(input);
        log.info(
            "Peça recebida por Têmis. taskId={} artifactId={} sha256={} bytes={}",
            id,
            evidence.path("id").asLong(),
            evidence.path("sha256").asText(),
            bytes.length);
      }
      task.put("creativeVisualInputs", List.copyOf(result.metadata));
      return result;
    } catch (Exception ex) {
      log.error(
          "Falha ao carregar imagens do gate comercial. taskId={} endpoint={}", id, endpoint, ex);
      result.close();
      throw new IOException("Não foi possível carregar a peça real para Têmis.", ex);
    }
  }

  /** Acrescenta arquivos governados ao comando multimodal sem permitir alterações de ativos. */
  List<String> attach(List<String> command) {
    List<String> result = new ArrayList<>(command);
    for (Path path : paths) {
      result.add("--image");
      result.add(path.toAbsolutePath().toString());
    }
    return result;
  }

  /** Exige que o parecer cite o hash e a avaliação de cada peça recebida. */
  static void validate(JsonNode result, JsonNode inputs) {
    var audit = result.path("renderedAssetAudit");
    if (!inputs.isArray() || inputs.isEmpty() || !audit.isArray() || audit.size() != inputs.size())
      throw new IllegalArgumentException("Têmis não auditou todas as peças finais recebidas.");
    Set<Long> seen = new HashSet<>();
    for (JsonNode item : audit) {
      boolean matched = false;
      for (JsonNode input : inputs) {
        var evidence = input.path("evidence");
        if (item.path("artifactId").asLong() == evidence.path("id").asLong()
            && item.path("sha256").asText().equals(evidence.path("sha256").asText()))
          matched = true;
      }
      if (!matched
          || !seen.add(item.path("artifactId").asLong())
          || item.path("assessment").asText().isBlank())
        throw new IllegalArgumentException(
            "O parecer de Têmis não corresponde às peças exatas recebidas.");
    }
  }

  /** Limpa arquivos efêmeros depois da execução, mantendo a auditoria e o storage privados. */
  @Override
  public void close() throws IOException {
    if (directory == null) return;
    try (var files = Files.walk(directory)) {
      for (var file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
    }
  }
}
