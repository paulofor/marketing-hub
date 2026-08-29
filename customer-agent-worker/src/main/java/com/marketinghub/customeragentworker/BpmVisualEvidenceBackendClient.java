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

/** Responsabilidade: persistir no backend as capturas Playwright produzidas para uma tarefa. */
@Component
public class BpmVisualEvidenceBackendClient {
  private static final String AGENT_KEY = "customer-agent";
  private final RestClient backend;

  /** Inicializa o cliente usando exclusivamente o backend principal do Marketing Hub. */
  public BpmVisualEvidenceBackendClient(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
  }

  /** Envia todos os snapshots e devolve ids persistidos na mesma ordem da captura. */
  List<UploadedVisualEvidence> upload(
      long taskId, BpmVisualEvidenceRunner.VisualEvidenceBundle bundle) {
    List<UploadedVisualEvidence> uploaded = new ArrayList<>();
    for (BpmVisualEvidenceRunner.VisualArtifact artifact : bundle.capture().artifacts()) {
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
