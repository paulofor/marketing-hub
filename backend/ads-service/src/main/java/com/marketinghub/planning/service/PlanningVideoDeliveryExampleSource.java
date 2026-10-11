package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.salesvideo.dto.DeliveryExampleDto;
import com.marketinghub.salesvideo.service.VideoDeliveryExampleSource;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fornece amostras comerciais revisadas, recuperando hashes legados da auditoria original. */
@Component
public class PlanningVideoDeliveryExampleSource implements VideoDeliveryExampleSource {
  private static final Logger log =
      LoggerFactory.getLogger(PlanningVideoDeliveryExampleSource.class);
  private final CommercialPlanRepository plans;
  private final CommercialPlanVisualAssetRepository assets;
  private final CommercialPlanImageStudioJobRepository jobs;
  private final ObjectMapper mapper;

  /** Conecta os registros canônicos do produto, da peça e da geração original. */
  public PlanningVideoDeliveryExampleSource(
      CommercialPlanRepository plans,
      CommercialPlanVisualAssetRepository assets,
      CommercialPlanImageStudioJobRepository jobs,
      ObjectMapper mapper) {
    this.plans = plans;
    this.assets = assets;
    this.jobs = jobs;
    this.mapper = mapper;
  }

  /** Expõe apenas entregáveis aprovados; ausência de prova não vira autorização de reutilização. */
  @Override
  @Transactional(readOnly = true)
  public List<DeliveryExampleDto> approved(Long productId) {
    var result = new ArrayList<DeliveryExampleDto>();
    for (var planId : plans.findIdsByProductId(productId)) {
      for (var asset :
          assets.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
              planId, CommercialPlanVisualAssetStatus.APPROVED)) {
        if (!"IMAGE".equalsIgnoreCase(asset.getMediaType())
            || asset.getAgentReviewStatus() != CommercialPlanVisualAssetReviewStatus.APPROVED
            || blank(asset.getRightsStatement())
            || blank(asset.getOrigin())
            || blank(asset.getAssetUrl())
            || !asset.getAssetUrl().startsWith("https://")
            || !("DELIVERY".equals(asset.getPurpose())
                || "PRODUCT_PROOF".equals(asset.getPurpose())
                || containsDeliveryPurpose(asset))) continue;
        String hash = hash(asset);
        if (hash == null || !hash.matches("[0-9a-f]{64}")) continue;
        result.add(
            new DeliveryExampleDto(
                asset.getId(),
                planId,
                asset.getLabel(),
                asset.getAssetUrl(),
                hash,
                asset.getOrigin(),
                asset.getRightsStatement()));
      }
    }
    return List.copyOf(result);
  }

  /** Reconhece finalidades persistidas sem procurar palavras em campos livres. */
  private boolean containsDeliveryPurpose(CommercialPlanVisualAsset asset) {
    try {
      var purposes =
          mapper.readTree(blank(asset.getPurposesJson()) ? "[]" : asset.getPurposesJson());
      for (var purpose : purposes)
        if ("DELIVERY".equals(purpose.asText()) || "PRODUCT_PROOF".equals(purpose.asText()))
          return true;
      return false;
    } catch (JsonProcessingException ex) {
      log.error("Finalidades inválidas na biblioteca audiovisual; assetId={}", asset.getId(), ex);
      return false;
    }
  }

  /**
   * Usa o hash canônico ou o recibo da geração concluída da mesma peça, sem recalcular aprovação.
   */
  private String hash(CommercialPlanVisualAsset asset) {
    if (!blank(asset.getContentSha256())) return asset.getContentSha256();
    var job = jobs.findByResultVisualAssetId(asset.getId()).orElse(null);
    if (job == null
        || job.getStatus() != CommercialPlanImageStudioStatus.COMPLETED
        || blank(job.getResponseJson())) return null;
    try {
      return mapper.readTree(job.getResponseJson()).at("/data/0/image_sha256").asText(null);
    } catch (JsonProcessingException ex) {
      log.error("Recibo da imagem inválido; assetId={} jobId={}", asset.getId(), job.getId(), ex);
      return null;
    }
  }

  /** Distingue ausência de metadados obrigatórios de um texto informado. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
