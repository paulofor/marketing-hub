package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.service.clipplan.VideoProductionSpendingPolicy;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: registrar uma prova temporal local do produto no Estúdio, sem contratar geração
 * ou revisão.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProductRecordingService {
  private final VideoProductRecordingRepository recordings;
  private final VideoProjectRepository projects;
  private final VideoProductionSpendingPolicy spending;
  private final ObjectMapper json;

  /** Preserva até 8 MiB no banco já existente; não chama storage externo nem fila de análise. */
  @Transactional
  public Map<String, Object> upload(Long projectId, MultipartFile file, String manifestText) {
    var project = owned(projectId, true);
    try {
      require(
          file != null && !file.isEmpty() && file.getSize() <= 8 * 1024 * 1024,
          "Envie um MP4 privado de até 8 MiB.");
      require(
          manifestText != null && manifestText.length() <= 65536,
          "Manifesto da gravação ausente ou excessivo.");
      var manifest = json.readTree(manifestText);
      var bytes = file.getBytes();
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      require(
          bytes.length > 12
              && new String(bytes, 4, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp"),
          "Arquivo MP4 inválido.");
      validate(manifest, project, hash);
      var row = recordings.findByVideoProjectIdAndSha256(projectId, hash).orElse(null);
      if (row != null) {
        require(
            json.readTree(row.getManifestJson()).equals(manifest),
            "Os mesmos bytes já possuem outro manifesto; preservar a proveniência original.");
        if (("internal://video-recordings/" + row.getId())
            .equals(project.getReferencePerformanceUri())) return resolve(project);
      }
      spending.validateProductRecording(project, manifest);
      if (row == null) {
        row = new VideoProductRecording();
        row.setVideoProjectId(projectId);
        row.setTenantId(project.getTenantId());
        row.setSha256(hash);
        row.setSourceVersion(manifest.path("sourcePrototypeVersion").asText());
        row.setManifestJson(manifest.toString());
        row.setVideoBytes(bytes);
        row.setCreatedAt(Instant.now());
        row = recordings.saveAndFlush(row);
      } else
        require(
            json.readTree(row.getManifestJson()).equals(manifest),
            "Os mesmos bytes já possuem outro manifesto; preservar a proveniência original.");
      project.setReferencePerformanceUri("internal://video-recordings/" + row.getId());
      project.setReferencePerformanceDurationSeconds(
          (int) Math.ceil(manifest.path("durationSeconds").asDouble()));
      projects.saveAndFlush(project);
      return resolve(project);
    } catch (IOException | java.security.NoSuchAlgorithmException ex) {
      log.error("Falha ao preservar gravação funcional privada; projectId={}", projectId, ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Não foi possível ler a gravação e seu manifesto.");
    }
  }

  /**
   * Resolve somente a gravação vinculada ao mesmo projeto, sem tratá-la como homologação premium.
   */
  @Transactional(readOnly = true)
  public Map<String, Object> resolve(VideoProject project) {
    var row = linked(project);
    try {
      var manifest = json.readTree(row.getManifestJson());
      var result = new LinkedHashMap<String, Object>();
      result.put("contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1");
      result.put("mediaKind", "FUNCTIONAL_SCREEN_RECORDING");
      result.put("projectId", project.getId());
      result.put("tenantId", project.getTenantId());
      result.put("productId", project.getProductId());
      result.put("experimentId", project.getExperimentId());
      result.put("productVersion", project.getCampaignKey());
      result.put("sourcePrototypeVersion", row.getSourceVersion());
      result.put("recordingId", row.getId());
      result.put("sha256", row.getSha256());
      result.put(
          "contentPath", "/api/sales-videos/projects/" + project.getId() + "/product-recording");
      result.put("durationSeconds", manifest.path("durationSeconds").asDouble());
      result.put("actions", manifest.path("actions"));
      result.put("syntheticScenario", true);
      result.put("commercialEvidenceClaimed", false);
      result.put("qualityApproved", false);
      result.put("captureEnvironment", "LOCAL_SANDBOX");
      result.put("manifest", manifest);
      return result;
    } catch (IOException ex) {
      log.error(
          "Manifesto persistido inválido; projectId={} recordingId={}",
          project.getId(),
          row.getId(),
          ex);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Manifesto da gravação inválido.");
    }
  }

  /** Entrega os bytes privados sem chamada a provedor ou exposição de URL pública. */
  @Transactional(readOnly = true)
  public byte[] read(Long projectId) {
    return linked(owned(projectId, false)).getVideoBytes();
  }

  /** Reconhece apenas o identificador do módulo, sem aceitar URL arbitrária ou conteúdo remoto. */
  public static boolean isReference(String value) {
    return value != null && value.matches("internal://video-recordings/[1-9][0-9]*");
  }

  /** Confere propriedade e vínculo antes de ler qualquer conteúdo binário. */
  private VideoProductRecording linked(VideoProject project) {
    TenantContextHolder.assertTenant(project.getTenantId());
    require(isReference(project.getReferencePerformanceUri()), "Gravação funcional não vinculada.");
    long id =
        Long.parseLong(
            project
                .getReferencePerformanceUri()
                .substring("internal://video-recordings/".length()));
    return recordings
        .findById(id)
        .filter(
            row ->
                Objects.equals(row.getVideoProjectId(), project.getId())
                    && Objects.equals(row.getTenantId(), project.getTenantId()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Exige uma captura temporal de software real, com dados de teste e ações observadas. */
  private void validate(JsonNode manifest, VideoProject project, String hash) {
    require(
        "PRODUCT_FUNCTIONAL_RECORDING_V1".equals(manifest.path("contractVersion").asText())
            && "LOCAL_SANDBOX".equals(manifest.path("captureEnvironment").asText())
            && "AGENT_VALIDATION".equals(manifest.path("trafficClass").asText())
            && manifest.path("syntheticData").asBoolean(false)
            && !manifest.path("commercialEvidenceClaimed").asBoolean(true)
            && !manifest.path("qualityApproved").asBoolean(true)
            && project.getProductId() == manifest.path("productId").asLong()
            && hash.equals(manifest.path("sha256").asText())
            && manifest.path("sourceFingerprint").asText().matches("[0-9a-f]{64}"),
        "Manifesto sem identidade, integridade ou segregação de teste.");
    double duration = manifest.path("durationSeconds").asDouble();
    require(
        Double.isFinite(duration)
            && duration >= 8
            && duration <= 90
            && manifest.path("width").asInt() >= 360
            && manifest.path("height").asInt() >= 600
            && "h264".equals(manifest.path("videoCodec").asText()),
        "Dimensões, duração ou codec da gravação inválidos.");
    Set<String> actions = new HashSet<>();
    double previous = -1;
    for (var action : manifest.path("actions")) {
      double at = action.path("seconds").asDouble(-1);
      require(at >= previous && at >= 0 && at < duration, "Ações fora da sequência da gravação.");
      actions.add(action.path("action").asText());
      previous = at;
    }
    require(
        actions.containsAll(
            Set.of("INPUT_SUBMITTED", "RESULT_VISIBLE", "SAVE_AND_COMPLETE", "REOPEN_SAVED")),
        "A gravação precisa mostrar entrada, resultado, salvar e reabrir.");
  }

  /** Mantém leitura isolada por tenant e serializa uploads concorrentes no projeto. */
  private VideoProject owned(Long id, boolean lock) {
    return (lock ? projects.findByIdForUpdate(id) : projects.findById(id))
        .filter(p -> Objects.equals(p.getTenantId(), TenantContextHolder.requireTenant()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Interrompe entrada inválida sem criar artefato, tarefa ou consumo externo. */
  private static void require(boolean valid, String message) {
    if (!valid) throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
