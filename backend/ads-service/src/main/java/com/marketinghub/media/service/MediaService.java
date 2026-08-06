package com.marketinghub.media.service;

import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.media.*;
import com.marketinghub.media.client.*;
import com.marketinghub.media.dto.CreateAudioRequest;
import com.marketinghub.media.dto.CreateVideoRequest;
import com.marketinghub.repository.jpa.media.AssetRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: criar ativos legados de mídia sem permitir consumo financeiro invisível. */
@Service
public class MediaService {
  private static final Logger log = LoggerFactory.getLogger(MediaService.class);
  private final AssetRepository repository;
  private final SynthesiaClient synthesia;
  private final HeyGenClient heyGen;
  private final ElevenLabsClient elevenLabs;
  private final RunwayClient runway;
  private final StudioCostLedgerService costLedgerService;

  /** Inicializa o serviço com provedores de mídia e o ledger financeiro obrigatório. */
  public MediaService(
      AssetRepository repository,
      SynthesiaClient synthesia,
      HeyGenClient heyGen,
      ElevenLabsClient elevenLabs,
      RunwayClient runway,
      StudioCostLedgerService costLedgerService) {
    this.repository = repository;
    this.synthesia = synthesia;
    this.heyGen = heyGen;
    this.elevenLabs = elevenLabs;
    this.runway = runway;
    this.costLedgerService = costLedgerService;
  }

  /** Cria vídeo assíncrono e mantém contexto ausente visível para regularização financeira. */
  @Transactional
  public Asset createVideo(CreateVideoRequest request) {
    Asset asset =
        Asset.builder()
            .type(AssetType.VIDEO)
            .provider(request.getProvider())
            .status(AssetStatus.PENDING)
            .payload(request.getScript())
            .campaignId(request.getCampaignId())
            .build();
    repository.save(asset);
    recordAttempt(
        asset,
        request.getProductId(),
        request.getCommercialPlanId(),
        request.getExperimentId(),
        "PENDING");
    createVideoAsync(asset, request);
    return asset;
  }

  /** Envia o vídeo ao provedor e preserva falhas no ativo e no ledger. */
  @Async
  void createVideoAsync(Asset asset, CreateVideoRequest request) {
    try {
      asset.setStatus(AssetStatus.PROCESSING);
      repository.save(asset);
      recordAttempt(
          asset,
          request.getProductId(),
          request.getCommercialPlanId(),
          request.getExperimentId(),
          "PROCESSING");
      Map<String, Object> resp;
      if (request.getProvider() == MediaProvider.SYNTHESIA) {
        resp = synthesia.createVideo(Map.of("script", request.getScript()));
      } else {
        resp = heyGen.createVideo(Map.of("script", request.getScript()));
      }
      asset.setExternalId(resp.get("id").toString());
      repository.save(asset);
    } catch (RuntimeException ex) {
      failAttempt(
          asset,
          request.getProductId(),
          request.getCommercialPlanId(),
          request.getExperimentId(),
          ex);
    }
  }

  /** Cria áudio assíncrono e mantém contexto ausente visível para regularização financeira. */
  @Transactional
  public Asset createAudio(CreateAudioRequest request) {
    Asset asset =
        Asset.builder()
            .type(AssetType.AUDIO)
            .provider(request.getProvider())
            .status(AssetStatus.PENDING)
            .payload(request.getScript())
            .campaignId(request.getCampaignId())
            .build();
    repository.save(asset);
    recordAttempt(
        asset,
        request.getProductId(),
        request.getCommercialPlanId(),
        request.getExperimentId(),
        "PENDING");
    createAudioAsync(asset, request);
    return asset;
  }

  /** Envia o áudio ao provedor e preserva falhas no ativo e no ledger. */
  @Async
  void createAudioAsync(Asset asset, CreateAudioRequest request) {
    try {
      asset.setStatus(AssetStatus.PROCESSING);
      repository.save(asset);
      recordAttempt(
          asset,
          request.getProductId(),
          request.getCommercialPlanId(),
          request.getExperimentId(),
          "PROCESSING");
      Map<String, Object> resp =
          elevenLabs.createSpeech(request.getVoice(), Map.of("text", request.getScript()));
      asset.setExternalId(resp.get("id").toString());
      repository.save(asset);
    } catch (RuntimeException ex) {
      failAttempt(
          asset,
          request.getProductId(),
          request.getCommercialPlanId(),
          request.getExperimentId(),
          ex);
    }
  }

  /** Consulta um ativo persistido pelo identificador. */
  public Asset getAsset(Long id) {
    return repository.findById(id).orElseThrow();
  }

  /** Lista ativos pelos filtros operacionais informados. */
  public List<Asset> findAssets(AssetStatus status, Long campaignId) {
    if (status != null && campaignId != null) {
      return repository.findByStatusAndCampaignId(status, campaignId);
    } else if (status != null) {
      return repository.findByStatus(status);
    } else {
      return repository.findAll();
    }
  }

  /** Atualiza ativos em processamento e reflete o estado no ledger idempotente. */
  @Scheduled(fixedDelay = 300000)
  public void refreshStatus() {
    List<Asset> processing = repository.findByStatus(AssetStatus.PROCESSING);
    for (Asset asset : processing) {
      Map<String, Object> resp;
      switch (asset.getProvider()) {
        case SYNTHESIA -> resp = synthesia.getVideo(asset.getExternalId());
        case HEYGEN -> resp = heyGen.getVideo(asset.getExternalId());
        case RUNWAY -> resp = runway.getJob(asset.getExternalId());
        default -> {
          continue;
        }
      }
      // TODO: map provider response to status/url
      asset.setStatus(AssetStatus.READY);
      asset.setUrl("TODO");
      repository.save(asset);
      costLedgerService.updateMediaStatus(asset.getId(), "READY", java.time.Instant.now());
    }
  }

  /** Registra o estado atual de uma tentativa de mídia sem inventar custo do provedor. */
  private void recordAttempt(
      Asset asset, Long productId, Long commercialPlanId, Long experimentId, String status) {
    costLedgerService.recordMedia(
        asset.getId(),
        productId,
        commercialPlanId,
        experimentId,
        asset.getType().name(),
        asset.getProvider() == null ? null : asset.getProvider().name(),
        asset.getModel(),
        status,
        null,
        false,
        asset.getCreatedAt(),
        "READY".equals(status) || "FAILED".equals(status) ? java.time.Instant.now() : null);
  }

  /** Marca a tentativa como falha, registra stack trace e mantém sua lacuna de custo visível. */
  private void failAttempt(
      Asset asset, Long productId, Long commercialPlanId, Long experimentId, RuntimeException ex) {
    asset.setStatus(AssetStatus.FAILED);
    repository.save(asset);
    recordAttempt(asset, productId, commercialPlanId, experimentId, "FAILED");
    log.error(
        "Falha no consumo do provedor de mídia; assetId={}, provider={}",
        asset.getId(),
        asset.getProvider(),
        ex);
  }
}
