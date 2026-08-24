package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.dto.CommercialPlanVisualAssetDto;
import com.marketinghub.planning.dto.CreateCommercialPlanVisualAssetRequest;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
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
  private static final Set<String> PURPOSES =
      Set.of("ADS", "LANDING", "SOCIAL", "DELIVERY", "PRODUCT_PROOF");
  private final CommercialPlanService planService;
  private final CommercialPlanVisualAssetRepository repository;
  private final ObjectMapper objectMapper;

  /** Inicializa a governança com as fontes canônicas de plano e kit visual. */
  @Autowired
  public CommercialPlanVisualAssetService(
      CommercialPlanService planService,
      CommercialPlanVisualAssetRepository repository,
      ObjectMapper objectMapper) {
    this.planService = planService;
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  /** Mantém a construção direta de testes legados com serialização JSON padrão. */
  CommercialPlanVisualAssetService(
      CommercialPlanService planService, CommercialPlanVisualAssetRepository repository) {
    this(planService, repository, new ObjectMapper());
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
    asset.setPurpose(normalizePurpose(request.purpose()));
    asset.setPurposesJson(writePurposes(List.of(asset.getPurpose())));
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
    if (status == CommercialPlanVisualAssetStatus.APPROVED
        && asset.getAgentReviewStatus() != null
        && asset.getAgentReviewStatus()
            != com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus
                .APPROVED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A imagem produzida por Têmis exige revisão independente antes da aprovação.");
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
        readPurposes(asset),
        asset.getOrigin(),
        asset.getRightsStatement(),
        asset.getVersionNumber(),
        asset.getStatus(),
        asset.getSourceVisualAsset() != null ? asset.getSourceVisualAsset().getId() : null,
        asset.getAgentReviewStatus(),
        reviewSummary(asset),
        asset.getCreatedAt(),
        asset.getUpdatedAt());
  }

  /** Serializa finalidades reutilizáveis sem acoplar a entidade a uma coluna JSON nativa. */
  public String writePurposes(List<String> purposes) {
    try {
      return objectMapper.writeValueAsString(purposes);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Finalidades da mídia inválidas", ex);
    }
  }

  /** Recupera finalidades novas e mantém compatibilidade com itens de finalidade única. */
  public List<String> readPurposes(CommercialPlanVisualAsset asset) {
    if (!StringUtils.hasText(asset.getPurposesJson())) {
      return StringUtils.hasText(asset.getPurpose()) ? List.of(asset.getPurpose()) : List.of();
    }
    try {
      return objectMapper.readValue(
          asset.getPurposesJson(),
          objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Finalidades persistidas da mídia são inválidas", ex);
    }
  }

  /** Extrai o resumo funcional sem expor request e response brutos na tela. */
  private String reviewSummary(CommercialPlanVisualAsset asset) {
    if (!StringUtils.hasText(asset.getAgentReviewJson())) {
      return null;
    }
    try {
      return objectMapper.readTree(asset.getAgentReviewJson()).path("summary").asText(null);
    } catch (JsonProcessingException ex) {
      return "Parecer persistido indisponível para leitura";
    }
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

  /** Normaliza e restringe a finalidade aos papéis canônicos da biblioteca audiovisual. */
  private String normalizePurpose(String purpose) {
    String normalized = purpose.trim().toUpperCase();
    if (!PURPOSES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "purpose must be ADS, LANDING, SOCIAL, DELIVERY or PRODUCT_PROOF");
    }
    return normalized;
  }
}
