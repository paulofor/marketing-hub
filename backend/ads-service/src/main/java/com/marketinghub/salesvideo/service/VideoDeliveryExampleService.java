package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.DeliveryExampleDto;
import com.marketinghub.salesvideo.service.postproduction.DeliveryExampleShot;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Congela amostras de entrega do produto para recomposição audiovisual sem confundi-las com a UI.
 */
@Component
public class VideoDeliveryExampleService {
  private final VideoProjectRepository projects;
  private final VideoDeliveryExampleSource source;

  /** Conecta o projeto proprietário à biblioteca revisada por meio da porta canônica. */
  public VideoDeliveryExampleService(
      VideoProjectRepository projects, VideoDeliveryExampleSource source) {
    this.projects = projects;
    this.source = source;
  }

  /** Lista amostras utilizáveis pela tela do projeto, sem autorizar o novo vídeo. */
  @Transactional(readOnly = true)
  public List<DeliveryExampleDto> available(Long projectId) {
    return source.approved(ownedProject(projectId).getProductId());
  }

  /** Resolve fontes no backend e exige sequência variada, iniciada no primeiro trecho da fala. */
  @Transactional(readOnly = true)
  public Map<String, Object> sequence(
      Long projectId, List<DeliveryExampleShot> shots, String captionText) {
    VideoProject project = ownedProject(projectId);
    if (shots == null || shots.size() < 2 || shots.size() > 12)
      throw invalid("Selecione de duas a doze amostras para a montagem.");
    var approved = source.approved(project.getProductId());
    int cues = captionText.split("\\s*\\|\\s*", -1).length;
    int previous = -1;
    var resolved = new ArrayList<Map<String, Object>>();
    for (var shot : shots) {
      if (shot == null
          || shot.captionCueIndex() <= previous
          || shot.captionCueIndex() >= cues
          || (previous == -1 && shot.captionCueIndex() != 0))
        throw invalid("Organize as amostras pela fala, começando pelo primeiro trecho.");
      var example =
          approved.stream()
              .filter(value -> Objects.equals(value.visualAssetId(), shot.visualAssetId()))
              .findFirst()
              .orElseThrow(() -> invalid("A amostra não está aprovada para este produto."));
      if (shot.headline() != null && shot.headline().length() > 90)
        throw invalid(
            "Use título curto, com até 90 caracteres, para preservar leitura no celular.");
      resolved.add(
          Map.of(
              "visualAssetId",
              example.visualAssetId(),
              "commercialPlanId",
              example.commercialPlanId(),
              "url",
              example.url(),
              "sha256",
              example.sha256(),
              "origin",
              example.origin(),
              "rightsStatement",
              example.rightsStatement(),
              "captionCueIndex",
              shot.captionCueIndex(),
              "headline",
              shot.headline() == null ? "" : shot.headline().strip()));
      previous = shot.captionCueIndex();
    }
    if (resolved.stream().map(value -> value.get("sha256")).distinct().count() < 2)
      throw invalid("A montagem exige ao menos duas artes diferentes, não cópias da mesma imagem.");
    var result = new LinkedHashMap<String, Object>();
    result.put("contractVersion", "APPROVED_DELIVERY_EXAMPLES_V1");
    result.put("projectId", project.getId());
    result.put("productId", project.getProductId());
    result.put("experimentId", project.getExperimentId());
    result.put("tenantId", project.getTenantId());
    result.put("productVersion", project.getCampaignKey());
    result.put("shots", resolved);
    result.put("commercialEvidenceClaimed", false);
    result.put("runtimeCaptureClaimed", false);
    return result;
  }

  /** Impede cruzamento de tenant ou projeto sem produto e experimento definidos. */
  private VideoProject ownedProject(Long id) {
    return projects
        .findById(id)
        .filter(
            value ->
                Objects.equals(value.getTenantId(), TenantContextHolder.requireTenant())
                    && value.getProductId() != null
                    && value.getExperimentId() != null)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Interrompe a finalização sem disparar geração ou alterar decisões independentes. */
  private ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
