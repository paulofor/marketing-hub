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
 * Responsabilidade: entregar a Têmis os pixels verificados das peças e de suas provas para revisão
 * independente.
 */
final class CreativeReviewImages implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(CreativeReviewImages.class);
  private final Path directory;
  private final List<Path> paths = new ArrayList<>();
  private final List<JsonNode> metadata = new ArrayList<>();
  private final Map<String, Integer> attachedHashes = new LinkedHashMap<>();

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
      var checks = new ArrayList<Map<String, Object>>();
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
        result.attachedHashes.put(evidence.path("sha256").asText(), result.paths.size());
        var check = new LinkedHashMap<String, Object>();
        check.put("contractVersion", "CREATIVE_PROOF_VERIFICATION_V1");
        check.put("renderedArtifactId", evidence.path("id").asLong());
        check.put("renderedSha256", evidence.path("sha256").asText());
        check.put("renderedHashVerified", true);
        check.put("renderedAttachmentIndex", result.paths.size());
        check.put("checkedAt", java.time.Instant.now().toString());
        result.loadProof(backend, endpoint, id, input, check);
        checks.add(check);
        result.metadata.add(input);
        log.info(
            "Peça recebida por Têmis. taskId={} artifactId={} sha256={} bytes={}",
            id,
            evidence.path("id").asLong(),
            evidence.path("sha256").asText(),
            bytes.length);
      }
      task.put("creativeVisualInputs", List.copyOf(result.metadata));
      task.put("creativeEvidenceChecks", List.copyOf(checks));
      return result;
    } catch (Exception ex) {
      log.error(
          "Falha ao carregar imagens do gate comercial. taskId={} endpoint={}", id, endpoint, ex);
      result.close();
      throw new IOException(
          "Não foi possível verificar as provas de Têmis. taskId=" + id + ": " + ex.getMessage(),
          ex);
    }
  }

  /** Acrescenta a fonte exata, uma prévia do recorte e as capturas datadas do destino aprovado. */
  private void loadProof(
      RestClient backend, String endpoint, long taskId, JsonNode input, Map<String, Object> check)
      throws Exception {
    var proof = input.path("creativeProof");
    check.put("sourceVerificationAvailable", proof.isObject());
    if (!proof.isObject()) return;
    var source = proof.path("source");
    requireSameVersion(input, source);
    byte[] bytes = verifiedPixels(backend, endpoint, taskId, source);
    var pixels = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
    if (pixels == null) throw new IllegalStateException("A fonte aprovada não é uma imagem.");
    attachPixels("source-" + source.path("evidence").path("id").asLong(), bytes);
    var crop = proof.path("crop");
    for (String key : List.of("x", "y", "width", "height"))
      if (!crop.path(key).isIntegralNumber() || !crop.path(key).canConvertToInt())
        throw new IllegalStateException("O recorte da fonte não contém coordenadas inteiras.");
    int x = crop.path("x").asInt(), y = crop.path("y").asInt();
    int width = crop.path("width").asInt(), height = crop.path("height").asInt();
    if (x < 0
        || y < 0
        || width < 1
        || height < 1
        || (long) x + width > pixels.getWidth()
        || (long) y + height > pixels.getHeight())
      throw new IllegalStateException("O recorte excede os pixels autorizados da fonte.");
    var out = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(pixels.getSubimage(x, y, width, height), "png", out);
    int cropIndex =
        attachPixels(
            "source-crop-" + source.path("evidence").path("id").asLong(), out.toByteArray());
    check.put(
        "cropPreview",
        Map.of(
            "sourceArtifactId",
            source.path("evidence").path("id").asLong(),
            "sha256",
            sha(out.toByteArray()),
            "attachmentIndex",
            cropIndex,
            "width",
            width,
            "height",
            height,
            "purpose",
            "REVIEW_SOURCE_CROP_ONLY"));
    check.put("source", verifiedMetadata(source, bytes));
    check.put("crop", crop);
    var destination = proof.path("currentDestination");
    if (!destination.isObject()) return;
    var report = destination.path("technicalProof");
    if (!"APPROVED".equals(report.path("decision").asText())
        || !input
            .path("prototypeVersion")
            .asText()
            .equals(report.path("prototypeVersion").asText()))
      throw new IllegalStateException("O relatório do destino não aprova a mesma versão.");
    var captures = new ArrayList<Map<String, Object>>();
    for (JsonNode capture : destination.path("captures")) {
      requireSameVersion(input, capture);
      byte[] captureBytes = verifiedPixels(backend, endpoint, taskId, capture);
      if (javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(captureBytes)) == null)
        throw new IllegalStateException("A captura do destino não é uma imagem.");
      attachPixels("destination-" + capture.path("evidence").path("id").asLong(), captureBytes);
      captures.add(verifiedMetadata(capture, captureBytes));
    }
    check.put(
        "currentDestination",
        Map.of(
            "taskId",
            destination.path("taskId").asLong(),
            "captures",
            captures,
            "scope",
            "PERSISTED_DETERMINISTIC_CAPTURE_WITH_EXPLICIT_TIMESTAMPS"));
  }

  /** Recusa trocar a versão do produto entre peça, fonte e evidências do destino. */
  private static void requireSameVersion(JsonNode input, JsonNode source) {
    if (input.path("prototypeVersion").asText().isBlank()
        || !input
            .path("prototypeVersion")
            .asText()
            .equals(source.path("prototypeVersion").asText()))
      throw new IllegalStateException("A fonte ou o destino pertencem a outra versão.");
  }

  /** Recalcula o hash de bytes entregues pelo endpoint canônico reservado para esta revisão. */
  private byte[] verifiedPixels(RestClient backend, String endpoint, long taskId, JsonNode source)
      throws Exception {
    long sourceTask = source.path("sourceTaskId").asLong();
    long artifact = source.path("evidence").path("id").asLong();
    if (sourceTask < 1 || artifact < 1) throw new IllegalStateException("Fonte sem identidade.");
    String content = endpoint + "/" + sourceTask + "/" + artifact + "/content";
    log.info("Baixando prova de origem/destino para Têmis. taskId={} endpoint={}", taskId, content);
    byte[] bytes = backend.get().uri(content).retrieve().body(byte[].class);
    if (bytes == null || !sha(bytes).equals(source.path("evidence").path("sha256").asText()))
      throw new IllegalStateException(
          "O hash da fonte ou da captura recebida por Têmis está incorreto.");
    log.info(
        "Prova recebida por Têmis. taskId={} endpoint={} bytes={} sha256={}",
        taskId,
        content,
        bytes.length,
        sha(bytes));
    return bytes;
  }

  /** Registra somente verificações executadas, conservando o horário original de cada captura. */
  private Map<String, Object> verifiedMetadata(JsonNode source, byte[] bytes) throws Exception {
    var evidence = source.path("evidence");
    return Map.of(
        "attachmentIndex",
        attachedHashes.get(sha(bytes)),
        "sourceTaskId",
        source.path("sourceTaskId").asLong(),
        "artifactId",
        evidence.path("id").asLong(),
        "sha256",
        sha(bytes),
        "hashVerified",
        true,
        "prototypeVersion",
        source.path("prototypeVersion").asText(),
        "capturedAt",
        evidence.path("capturedAt").asText(),
        "deviceProfile",
        evidence.path("deviceProfile").asText(),
        "evidenceKey",
        evidence.path("evidenceKey").asText());
  }

  /** Anexa pixels únicos e devolve o índice que relaciona cada imagem à auditoria do prompt. */
  private int attachPixels(String name, byte[] bytes) throws Exception {
    String hash = sha(bytes);
    if (attachedHashes.containsKey(hash)) return attachedHashes.get(hash);
    Path file = directory.resolve(name + ".png");
    Files.write(file, bytes);
    paths.add(file);
    attachedHashes.put(hash, paths.size());
    return paths.size();
  }

  /** Calcula a identidade dos arquivos antes de entregá-los ao modelo revisor. */
  private static String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
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
