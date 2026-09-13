package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Resolve capturas homologadas da versão do PDE para composição privada, sem criar provas. */
@Component
public class VideoProductProofService {
  private static final Logger log = LoggerFactory.getLogger(VideoProductProofService.class);
  private static final Pattern REFERENCE =
      Pattern.compile(
          "^internal://agent-tasks/([1-9][0-9]*)/visual-evidence/([1-9][0-9]*)(?:#crop=([0-9]+),([0-9]+),([1-9][0-9]*),([1-9][0-9]*))?$");
  private final VideoProductProofSource source;
  private final VideoProjectRepository projectRepository;
  private final ObjectMapper mapper;

  /** Conecta prova persistida, bytes privados e o projeto proprietário. */
  public VideoProductProofService(
      VideoProductProofSource source,
      VideoProjectRepository projectRepository,
      ObjectMapper mapper) {
    this.source = source;
    this.projectRepository = projectRepository;
    this.mapper = mapper;
  }

  /** Reconhece somente o identificador interno canônico e o enquadramento explícito opcional. */
  public static boolean isInternalReference(String value) {
    return value != null && REFERENCE.matcher(value.trim()).matches();
  }

  /**
   * Congela hash e identidade da captura homologada, com enquadramento sem alteração dos pixels.
   */
  @Transactional(readOnly = true)
  public Map<String, Object> resolve(VideoProject project) {
    if (!isInternalReference(project.getReferencePerformanceUri())) return Map.of();
    var match = REFERENCE.matcher(project.getReferencePerformanceUri().trim());
    match.matches();
    var proof =
        source
            .find(Long.valueOf(match.group(1)), Long.valueOf(match.group(2)))
            .orElseThrow(() -> invalid("A captura informada não pertence à tarefa."));
    validateOwnership(project, proof);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1");
    result.put("projectId", project.getId());
    result.put("tenantId", project.getTenantId());
    result.put("taskId", proof.taskId());
    result.put("evidenceId", proof.evidenceId());
    result.put("productId", project.getProductId());
    result.put("experimentId", project.getExperimentId());
    result.put("productVersion", project.getCampaignKey());
    result.put("sha256", proof.sha256());
    result.put("contentPath", "/api/sales-videos/projects/" + project.getId() + "/product-proof");
    result.put("syntheticScenario", true);
    result.put("commercialEvidenceClaimed", false);
    if (match.group(3) != null) {
      result.put(
          "crop",
          java.util.List.of(
              Integer.valueOf(match.group(3)),
              Integer.valueOf(match.group(4)),
              Integer.valueOf(match.group(5)),
              Integer.valueOf(match.group(6))));
    }
    return result;
  }

  /** Entrega a captura somente pelo módulo de vídeo e pelo tenant do projeto. */
  @Transactional(readOnly = true)
  public byte[] read(Long projectId) {
    VideoProject project =
        projectRepository
            .findById(projectId)
            .filter(
                value -> Objects.equals(value.getTenantId(), TenantContextHolder.requireTenant()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var proof = resolve(project);
    if (proof.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return source.read((Long) proof.get("taskId"), (Long) proof.get("evidenceId"));
  }

  /**
   * Exige resultado técnico aprovado da mesma versão, experimento e produto, sem prova comercial.
   */
  private void validateOwnership(VideoProject project, VideoProductProofSource.Proof evidence) {
    try {
      JsonNode result =
          mapper.readTree(evidence.resultJson() == null ? "{}" : evidence.resultJson());
      boolean listed = false;
      for (JsonNode artifact : result.path("artifacts")) {
        if (artifact.path("artifactId").asLong() == evidence.evidenceId()
            && evidence.sha256().equals(artifact.path("sha256").asText())) listed = true;
      }
      if (!"COMPLETED".equals(evidence.status())
          || !"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(result.path("contractVersion").asText())
          || !"APPROVED".equals(result.path("decision").asText())
          || project.getProductId() == null
          || result.path("productId").asLong() != project.getProductId()
          || project.getExperimentId() == null
          || !("experiment:" + project.getExperimentId()).equals(evidence.sourceReference())
          || project.getCampaignKey() == null
          || !project.getCampaignKey().equals(result.path("prototypeVersion").asText())
          || !"AGENT_VALIDATION".equals(result.path("trafficClass").asText())
          || result.path("commercialEvidenceClaimed").asBoolean(true)
          || !listed)
        throw invalid("A prova precisa estar homologada na mesma versão, produto e experimento.");
    } catch (JsonProcessingException ex) {
      log.error(
          "Contrato da prova visual inválido; projectId={} taskId={} evidenceId={}",
          project.getId(),
          evidence.taskId(),
          evidence.evidenceId(),
          ex);
      throw invalid("O resultado da homologação visual está inválido.");
    }
  }

  /** Interrompe a preparação sem aprovar material, financeiro ou publicação. */
  private ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
