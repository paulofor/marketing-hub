package com.marketinghub.customeragentworker;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/** Responsabilidade: trocar provas visuais privadas com o backend dentro da tarefa reservada. */
@Component
public class BpmVisualEvidenceBackendClient {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(BpmVisualEvidenceBackendClient.class);
  private static final String AGENT_KEY = "customer-agent";
  private final RestClient backend;

  /** Inicializa o cliente usando exclusivamente o backend principal do Marketing Hub. */
  public BpmVisualEvidenceBackendClient(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
  }

  /**
   * Baixa peças renderizadas autorizadas para revisão sem criar uma nova captura nem mudar sua
   * origem.
   */
  List<UploadedVisualEvidence> creativeInputs(long taskId, Path directory)
      throws java.io.IOException {
    String endpoint =
        "/api/internal/agent-tasks/" + AGENT_KEY + "/stage-executions/" + taskId + "/visual-inputs";
    try {
      var inputs =
          backend
              .get()
              .uri(endpoint)
              .retrieve()
              .body(com.fasterxml.jackson.databind.JsonNode.class);
      if (inputs == null || !inputs.isArray() || inputs.isEmpty())
        throw new IllegalStateException("A imagem final do criativo não está disponível.");
      var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
      List<UploadedVisualEvidence> result = new ArrayList<>();
      for (var input : inputs) {
        var evidence = mapper.treeToValue(input.path("evidence"), UploadedVisualEvidence.class);
        if (!"CREATIVE_RENDER".equals(evidence.evidenceType()))
          throw new IllegalStateException(
              "A revisão exige a peça renderizada, não uma captura de origem.");
        String content =
            endpoint + "/" + input.path("sourceTaskId").asLong() + "/" + evidence.id() + "/content";
        log.info(
            "Baixando criativo para Psique. taskId={} endpoint={} artifactId={}",
            taskId,
            content,
            evidence.id());
        byte[] bytes = backend.get().uri(content).retrieve().body(byte[].class);
        if (bytes == null
            || !java.util.HexFormat.of()
                .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
                .equals(evidence.sha256()))
          throw new IllegalStateException("O hash da imagem de Psique diverge da peça persistida.");
        Path file = directory.resolve("creative-" + evidence.id() + ".png");
        java.nio.file.Files.write(file, bytes);
        var pixels = javax.imageio.ImageIO.read(file.toFile());
        if (pixels == null || pixels.getWidth() != 1080 || pixels.getHeight() != 1350)
          throw new IllegalStateException("Imagem criativa fora do formato contratado.");
        result.add(evidence.withLocalPath(file.toAbsolutePath().toString()));
        log.info(
            "Criativo recebido por Psique. taskId={} artifactId={} bytes={} sha256={}",
            taskId,
            evidence.id(),
            bytes.length,
            evidence.sha256());
      }
      return List.copyOf(result);
    } catch (Exception ex) {
      log.error(
          "Falha ao receber peça real para Psique. taskId={} endpoint={}", taskId, endpoint, ex);
      throw new java.io.IOException("Não foi possível carregar a imagem final do criativo.", ex);
    }
  }

  /** Envia todos os snapshots e devolve ids persistidos na mesma ordem da captura. */
  List<UploadedVisualEvidence> upload(
      long taskId, BpmVisualEvidenceRunner.VisualEvidenceBundle bundle) {
    return uploadArtifacts(taskId, bundle.capture().artifacts());
  }

  /** Persiste artefatos de um harness multidispositivo sem exigir uma captura mobile monolítica. */
  List<UploadedVisualEvidence> uploadArtifacts(
      long taskId, List<BpmVisualEvidenceRunner.VisualArtifact> artifacts) {
    List<UploadedVisualEvidence> uploaded = new ArrayList<>();
    for (BpmVisualEvidenceRunner.VisualArtifact artifact : artifacts) {
      var body = new LinkedMultiValueMap<String, Object>();
      body.add("captureSessionId", artifact.captureSessionId());
      body.add("evidenceKey", artifact.evidenceKey());
      body.add("evidenceType", artifact.evidenceType());
      body.add("deviceProfile", artifact.deviceProfile());
      body.add("pageNumber", artifact.pageNumber().toString());
      if (artifact.foldNumber() != null) {
        body.add("foldNumber", artifact.foldNumber().toString());
      }
      body.add("viewportWidth", artifact.viewportWidth().toString());
      body.add("viewportHeight", artifact.viewportHeight().toString());
      body.add("pageHeightPx", artifact.pageHeightPx().toString());
      body.add("scrollY", artifact.scrollY().toString());
      body.add("sourceUrl", artifact.sourceUrl());
      body.add("finalUrl", artifact.finalUrl());
      body.add("capturedAt", artifact.capturedAt().toString());
      body.add("file", new FileSystemResource(Path.of(artifact.localPath())));
      UploadedVisualEvidence persisted =
          backend
              .post()
              .uri(
                  "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/visual-evidence",
                  AGENT_KEY,
                  taskId)
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(body)
              .retrieve()
              .body(UploadedVisualEvidence.class);
      if (persisted == null) {
        throw new BpmVisualEvidenceRunner.VisualEvidenceException(
            "Backend não confirmou o snapshot visual de Psique.");
      }
      uploaded.add(
          persisted.withLocalPath(Path.of(artifact.localPath()).toAbsolutePath().toString()));
    }
    return List.copyOf(uploaded);
  }

  /** Representa a prova persistida e o arquivo local disponível à mesma execução do modelo. */
  record UploadedVisualEvidence(
      Long id,
      String captureSessionId,
      String evidenceKey,
      String evidenceType,
      String label,
      String deviceProfile,
      Integer pageNumber,
      Integer foldNumber,
      Integer viewportWidth,
      Integer viewportHeight,
      Integer pageHeightPx,
      Integer scrollY,
      String sourceUrl,
      String finalUrl,
      String contentUrl,
      Long sizeBytes,
      String sha256,
      Instant capturedAt,
      String localPath) {
    /** Acrescenta o caminho efêmero somente para inspeção visual pelo modelo desta tentativa. */
    UploadedVisualEvidence withLocalPath(String value) {
      return new UploadedVisualEvidence(
          id,
          captureSessionId,
          evidenceKey,
          evidenceType,
          label,
          deviceProfile,
          pageNumber,
          foldNumber,
          viewportWidth,
          viewportHeight,
          pageHeightPx,
          scrollY,
          sourceUrl,
          finalUrl,
          contentUrl,
          sizeBytes,
          sha256,
          capturedAt,
          value);
    }
  }
}
