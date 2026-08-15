package com.marketinghub.planning.service;

import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.dto.CommercialPlanVisualAssetDto;
import com.marketinghub.planning.dto.CreateCommercialPlanVisualAssetRequest;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar o cadastro, aprovação e consumo da biblioteca audiovisual comercial.
 */
@Service
public class CommercialPlanVisualAssetService {
  private final CommercialPlanService planService;
  private final CommercialPlanVisualAssetRepository repository;

  /** Inicializa a governança com as fontes canônicas de plano e kit visual. */
  public CommercialPlanVisualAssetService(
      CommercialPlanService planService, CommercialPlanVisualAssetRepository repository) {
    this.planService = planService;
    this.repository = repository;
  }

  /** Lista todo o kit visual, incluindo rascunhos e itens retirados. */
  @Transactional(readOnly = true)
  public List<CommercialPlanVisualAssetDto> list(Long planId) {
    planService.getPlan(planId);
    return repository.findByCommercialPlanIdOrderByCreatedAtAsc(planId).stream()
        .map(this::dto)
        .toList();
  }

  /** Cadastra uma nova versão como rascunho para revisão independente. */
  @Transactional
  public CommercialPlanVisualAssetDto create(
      Long planId, CreateCommercialPlanVisualAssetRequest request) {
    require(request.assetUrl(), "assetUrl");
    require(request.mediaType(), "mediaType");
    require(request.label(), "label");
    require(request.purpose(), "purpose");
    require(request.origin(), "origin");
    require(request.rightsStatement(), "rightsStatement");
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setCommercialPlan(planService.getPlan(planId));
    asset.setAssetUrl(request.assetUrl().trim());
    asset.setMediaType(normalizeMediaType(request.mediaType()));
    asset.setLabel(request.label().trim());
    asset.setPurpose(request.purpose().trim().toUpperCase());
    asset.setOrigin(request.origin().trim());
    asset.setRightsStatement(request.rightsStatement().trim());
    asset.setVersionNumber(
        (int) repository.countByCommercialPlanIdAndAssetUrl(planId, asset.getAssetUrl()) + 1);
    asset.setStatus(CommercialPlanVisualAssetStatus.DRAFT);
    return dto(repository.save(asset));
  }

  /** Registra aprovação ou retirada sem apagar o histórico visual. */
  @Transactional
  public CommercialPlanVisualAssetDto updateStatus(
      Long planId, Long assetId, CommercialPlanVisualAssetStatus status) {
    if (status == null || status == CommercialPlanVisualAssetStatus.DRAFT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "status must be APPROVED or RETIRED");
    }
    CommercialPlanVisualAsset asset = repository.findById(assetId).orElseThrow();
    if (!planId.equals(asset.getCommercialPlan().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "visual asset not found for plan");
    }
    asset.setStatus(status);
    return dto(asset);
  }

  /** Produz o manifesto de referências aprovadas entregue a Têmis e ao AI Worker. */
  @Transactional(readOnly = true)
  public List<CommercialPlanVisualAssetDto> approved(Long planId) {
    return repository
        .findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            planId, CommercialPlanVisualAssetStatus.APPROVED)
        .stream()
        .map(this::dto)
        .toList();
  }

  /** Converte a entidade persistida para o contrato público. */
  private CommercialPlanVisualAssetDto dto(CommercialPlanVisualAsset asset) {
    return new CommercialPlanVisualAssetDto(
        asset.getId(),
        asset.getAssetUrl(),
        asset.getMediaType(),
        asset.getLabel(),
        asset.getPurpose(),
        asset.getOrigin(),
        asset.getRightsStatement(),
        asset.getVersionNumber(),
        asset.getStatus(),
        asset.getCreatedAt(),
        asset.getUpdatedAt());
  }

  /** Exige metadado textual preenchido antes de persistir a referência. */
  private void require(String value, String field) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
    }
  }

  /** Normaliza e restringe o tipo às mídias que os executores conseguem consumir. */
  private String normalizeMediaType(String mediaType) {
    String normalized = mediaType.trim().toUpperCase();
    if (!normalized.equals("IMAGE") && !normalized.equals("VIDEO")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaType must be IMAGE or VIDEO");
    }
    return normalized;
  }
}
