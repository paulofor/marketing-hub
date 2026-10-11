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
    return resolveReference(project, project.getReferencePerformanceUri(), false);
  }

  /** Congela cada captura da sequência, exigindo o mesmo produto, versão, experimento e tenant. */
  @Transactional(readOnly = true)
  public java.util.List<Map<String, Object>> sequence(
      Long projectId,
      java.util.List<com.marketinghub.salesvideo.service.postproduction.ProductProofShot> shots,
      String captionText) {
    VideoProject project = ownedProject(projectId);
    if (shots == null || shots.isEmpty() || shots.size() > 5)
      throw invalid("Use de um a cinco trechos de demonstração.");
    int segments = captionText.split("\\s*\\|\\s*", -1).length;
    int previous = -1;
    var result = new java.util.ArrayList<Map<String, Object>>();
    for (var shot : shots) {
      if (shot == null
          || shot.captionCueIndex() <= previous
          || shot.captionCueIndex() >= segments
          || !Double.isFinite(shot.minimumSeconds())
          || shot.minimumSeconds() < 2
          || shot.minimumSeconds() > 12
          || !isInternalReference(shot.reference())
          || !shot.reference().contains("#crop="))
        throw invalid(
            "Cada trecho exige legenda em ordem, captura homologada recortada e leitura entre 2 e 12 segundos.");
      var proof = resolveReference(project, shot.reference(), true);
      proof.put("captionCueIndex", shot.captionCueIndex());
      proof.put("minimumSeconds", shot.minimumSeconds());
      result.add(proof);
      previous = shot.captionCueIndex();
    }
    return java.util.List.copyOf(result);
  }

  /** Resolve apenas referências canônicas, sem confiar em hash ou URL enviados pelo editor. */
  private Map<String, Object> resolveReference(
      VideoProject project, String reference, boolean explicit) {
    if (!isInternalReference(reference)) throw invalid("Referência de prova inválida.");
    var match = REFERENCE.matcher(reference.trim());
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
    result.put(
        "contentPath",
        "/api/sales-videos/projects/"
            + project.getId()
            + "/product-proof"
            + (explicit ? "/" + proof.taskId() + "/" + proof.evidenceId() : ""));
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
    VideoProject project = ownedProject(projectId);
    var proof = resolve(project);
    if (proof.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return source.read((Long) proof.get("taskId"), (Long) proof.get("evidenceId"));
  }

  /** Entrega outra captura homologada do mesmo projeto para demonstrar entrada e saída reais. */
  @Transactional(readOnly = true)
  public byte[] read(Long projectId, Long taskId, Long evidenceId) {
    var proof =
        resolveReference(
            ownedProject(projectId),
            "internal://agent-tasks/" + taskId + "/visual-evidence/" + evidenceId,
            true);
    return source.read((Long) proof.get("taskId"), (Long) proof.get("evidenceId"));
  }

  /** Recusa projeto de outro tenant antes de resolver sua prova. */
  private VideoProject ownedProject(Long projectId) {
    return projectRepository
        .findById(projectId)
        .filter(value -> Objects.equals(value.getTenantId(), TenantContextHolder.requireTenant()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /**
   * Exige resultado técnico aprovado da mesma versão e produto, com origem ou reautorização exata.
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
      boolean sameSource =
          project.getExperimentId() != null
              && ("experiment:" + project.getExperimentId()).equals(evidence.sourceReference());
      boolean reauthorized =
          project.getExperimentId() != null
              && source.isAuthorizedFor(
                  evidence,
                  "experiment:" + project.getExperimentId(),
                  project.getProductId(),
                  project.getCampaignKey());
      if (!"COMPLETED".equals(evidence.status())
          || !"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(result.path("contractVersion").asText())
          || !"APPROVED".equals(result.path("decision").asText())
          || project.getProductId() == null
          || result.path("productId").asLong() != project.getProductId()
          || (!sameSource && !reauthorized)
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
