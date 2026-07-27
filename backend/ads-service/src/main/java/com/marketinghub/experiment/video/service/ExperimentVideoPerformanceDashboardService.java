package com.marketinghub.experiment.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.dto.ExperimentVideoPerformanceDashboardDto;
import com.marketinghub.experiment.video.dto.ExperimentVideoPerformanceDashboardDto.ExperimentVideoPerformanceAssetDto;
import com.marketinghub.experiment.video.dto.ExperimentVideoPerformanceDashboardDto.ExperimentVideoPerformanceCampaignDto;
import com.marketinghub.experiment.video.dto.ExperimentVideoPerformanceDashboardDto.ExperimentVideoPerformanceCreativeDto;
import com.marketinghub.experiment.video.dto.ExperimentVideoPerformanceDashboardDto.ExperimentVideoPerformanceSummaryDto;
import com.marketinghub.facebookads.AdCreativeKind;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Consolida assets de vídeo, criativos Meta e marcos do funil em um painel comercial. */
@Service
public class ExperimentVideoPerformanceDashboardService {
  private static final ObjectMapper OBJECT_MAPPER =
      JsonMapper.builder().findAndAddModules().build();

  private final ExperimentRepository experimentRepository;
  private final ExperimentVideoAssetRepository videoAssetRepository;
  private final FacebookAdsCampaignRepository campaignRepository;
  private final FacebookAdsAdSetRepository adSetRepository;
  private final ExperimentCampaignMetricRepository campaignMetricRepository;
  private final ExperimentFunnelService funnelService;

  /** Inicializa o serviço com as fontes canônicas de vídeo, Meta Ads e funil. */
  public ExperimentVideoPerformanceDashboardService(
      ExperimentRepository experimentRepository,
      ExperimentVideoAssetRepository videoAssetRepository,
      FacebookAdsCampaignRepository campaignRepository,
      FacebookAdsAdSetRepository adSetRepository,
      ExperimentCampaignMetricRepository campaignMetricRepository,
      ExperimentFunnelService funnelService) {
    this.experimentRepository = experimentRepository;
    this.videoAssetRepository = videoAssetRepository;
    this.campaignRepository = campaignRepository;
    this.adSetRepository = adSetRepository;
    this.campaignMetricRepository = campaignMetricRepository;
    this.funnelService = funnelService;
  }

  /** Retorna o painel consolidado de vídeo para um experimento. */
  @Transactional(readOnly = true)
  public ExperimentVideoPerformanceDashboardDto summarize(Long experimentId) {
    experimentRepository.findById(experimentId).orElseThrow();
    List<ExperimentVideoAsset> assets =
        videoAssetRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    List<FacebookAdsCampaign> campaigns = fetchCampaigns(experimentId);
    Map<String, ExperimentCampaignMetric> metricsByCampaignId = fetchMetricsByCampaignId(campaigns);
    FunnelSnapshot funnel = fetchFunnelSnapshot(experimentId);
    List<MetaCreativeSnapshot> metaCreatives = collectMetaCreatives(campaigns);
    List<ExperimentVideoPerformanceAssetDto> assetDtos =
        buildAssetDtos(assets, metaCreatives, funnel);
    List<ExperimentVideoPerformanceCampaignDto> campaignDtos =
        buildCampaignDtos(campaigns, metricsByCampaignId);
    ExperimentVideoPerformanceSummaryDto summary =
        buildSummary(assets, metaCreatives, campaignDtos, funnel);
    return new ExperimentVideoPerformanceDashboardDto(summary, assetDtos, campaignDtos);
  }

  /** Busca campanhas com conjuntos e anúncios hidratados para o experimento. */
  private List<FacebookAdsCampaign> fetchCampaigns(Long experimentId) {
    List<FacebookAdsCampaign> campaigns =
        campaignRepository.findDetailedByExperimentId(experimentId);
    List<String> campaignIds =
        campaigns.stream().map(FacebookAdsCampaign::getId).filter(Objects::nonNull).toList();
    if (campaignIds.isEmpty()) {
      return campaigns;
    }
    List<FacebookAdsAdSet> adSets = adSetRepository.findDetailedByCampaignIds(campaignIds);
    Map<String, List<FacebookAdsAdSet>> adSetsByCampaign = new HashMap<>();
    for (FacebookAdsAdSet adSet : adSets) {
      if (adSet.getCampaign() == null || adSet.getCampaign().getId() == null) {
        continue;
      }
      adSetsByCampaign
          .computeIfAbsent(adSet.getCampaign().getId(), ignored -> new ArrayList<>())
          .add(adSet);
    }
    campaigns.forEach(
        campaign ->
            campaign.setAdSets(
                new ArrayList<>(adSetsByCampaign.getOrDefault(campaign.getId(), List.of()))));
    return campaigns;
  }

  /** Indexa métricas de campanha pelo id interno da campanha. */
  private Map<String, ExperimentCampaignMetric> fetchMetricsByCampaignId(
      List<FacebookAdsCampaign> campaigns) {
    List<String> campaignIds =
        campaigns.stream().map(FacebookAdsCampaign::getId).filter(Objects::nonNull).toList();
    if (campaignIds.isEmpty()) {
      return Map.of();
    }
    Map<String, ExperimentCampaignMetric> metrics = new HashMap<>();
    for (ExperimentCampaignMetric metric :
        campaignMetricRepository.findByCampaignIdIn(campaignIds)) {
      if (metric.getCampaign() != null && metric.getCampaign().getId() != null) {
        metrics.put(metric.getCampaign().getId(), metric);
      }
    }
    return metrics;
  }

  /** Lê os marcos comerciais do funil já consolidados pelo backend. */
  private FunnelSnapshot fetchFunnelSnapshot(Long experimentId) {
    List<ExperimentFunnelStageDto> stages = funnelService.summarize(experimentId);
    return new FunnelSnapshot(
        totalForStage(stages, ExperimentFunnelStage.ENVIO_FORM),
        totalForStage(stages, ExperimentFunnelStage.ACESSO_CHECKOUT),
        totalForStage(stages, ExperimentFunnelStage.COMPRA));
  }

  /** Extrai o total de uma etapa específica do funil. */
  private long totalForStage(List<ExperimentFunnelStageDto> stages, ExperimentFunnelStage stage) {
    return stages.stream()
        .filter(item -> item.getStage() == stage)
        .mapToLong(ExperimentFunnelStageDto::getTotalCount)
        .sum();
  }

  /** Coleta criativos Meta publicados e seus anúncios vinculados. */
  private List<MetaCreativeSnapshot> collectMetaCreatives(List<FacebookAdsCampaign> campaigns) {
    List<MetaCreativeSnapshot> snapshots = new ArrayList<>();
    for (FacebookAdsCampaign campaign : campaigns) {
      for (FacebookAdsAdSet adSet : safeList(campaign.getAdSets())) {
        for (FacebookAdsAd ad : safeList(adSet.getAds())) {
          FacebookAdsAdCreative creative = ad.getCreative();
          if (creative == null) {
            continue;
          }
          snapshots.add(
              new MetaCreativeSnapshot(
                  campaign.getId(),
                  creative.getId(),
                  creative.getKind(),
                  extractMetaVideoId(creative),
                  ad.getId(),
                  ad.getName(),
                  ad.getStatus() != null ? ad.getStatus().name() : null,
                  collectCreativeTokens(creative)));
        }
      }
    }
    return snapshots;
  }

  /** Monta linhas de asset com vínculo direto quando houver identificador compatível. */
  private List<ExperimentVideoPerformanceAssetDto> buildAssetDtos(
      List<ExperimentVideoAsset> assets,
      List<MetaCreativeSnapshot> metaCreatives,
      FunnelSnapshot funnel) {
    return assets.stream()
        .filter(this::shouldExposeAsset)
        .sorted(
            Comparator.comparing(
                ExperimentVideoAsset::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
        .map(asset -> toAssetDto(asset, metaCreatives, funnel))
        .toList();
  }

  /** Decide se o asset é relevante para leitura comercial do painel. */
  private boolean shouldExposeAsset(ExperimentVideoAsset asset) {
    return asset.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED
        || StringUtils.hasText(asset.getAssetUrl())
        || asset.isRequiredForRelease();
  }

  /** Converte asset de vídeo para linha do painel com atribuição de funil. */
  private ExperimentVideoPerformanceAssetDto toAssetDto(
      ExperimentVideoAsset asset, List<MetaCreativeSnapshot> metaCreatives, FunnelSnapshot funnel) {
    List<MetaCreativeSnapshot> matches =
        metaCreatives.stream().filter(creative -> matchesAsset(asset, creative)).toList();
    String attributionLevel = matches.isEmpty() ? "EXPERIMENT" : "AD";
    List<ExperimentVideoPerformanceCreativeDto> creativeDtos =
        matches.stream().map(creative -> toCreativeDto(creative, funnel)).toList();
    return new ExperimentVideoPerformanceAssetDto(
        asset.getId(),
        asset.getSlot() != null ? asset.getSlot().name() : null,
        asset.getReviewStatus() != null ? asset.getReviewStatus().name() : null,
        asset.getStatus() != null ? asset.getStatus().name() : null,
        asset.getProvider(),
        asset.getModel(),
        asset.getAssetUrl(),
        attributionLevel,
        creativeDtos,
        funnel.diagnosticStarts(),
        funnel.checkoutAccesses(),
        funnel.purchases());
  }

  /** Converte criativo Meta para linha aninhada do painel. */
  private ExperimentVideoPerformanceCreativeDto toCreativeDto(
      MetaCreativeSnapshot creative, FunnelSnapshot funnel) {
    return new ExperimentVideoPerformanceCreativeDto(
        creative.creativeId(),
        creative.kind() != null ? creative.kind().name() : null,
        creative.metaVideoId(),
        creative.adId(),
        creative.adName(),
        creative.adStatus(),
        funnel.diagnosticStarts(),
        funnel.checkoutAccesses(),
        funnel.purchases());
  }

  /** Monta as linhas de campanha com impressões, cliques e gasto. */
  private List<ExperimentVideoPerformanceCampaignDto> buildCampaignDtos(
      List<FacebookAdsCampaign> campaigns,
      Map<String, ExperimentCampaignMetric> metricsByCampaignId) {
    return campaigns.stream()
        .map(
            campaign -> {
              ExperimentCampaignMetric metric = metricsByCampaignId.get(campaign.getId());
              return new ExperimentVideoPerformanceCampaignDto(
                  campaign.getId(),
                  campaign.getName(),
                  campaign.getStatus() != null ? campaign.getStatus().name() : null,
                  metric != null && metric.getImpressions() != null ? metric.getImpressions() : 0,
                  metric != null && metric.getClicks() != null ? metric.getClicks() : 0,
                  metric != null && metric.getSpend() != null ? metric.getSpend() : BigDecimal.ZERO,
                  campaign.getMetricsLastSyncedAt());
            })
        .toList();
  }

  /** Gera o resumo executivo e recomendação comercial do painel. */
  private ExperimentVideoPerformanceSummaryDto buildSummary(
      List<ExperimentVideoAsset> assets,
      List<MetaCreativeSnapshot> metaCreatives,
      List<ExperimentVideoPerformanceCampaignDto> campaigns,
      FunnelSnapshot funnel) {
    long impressions =
        campaigns.stream().mapToLong(ExperimentVideoPerformanceCampaignDto::impressions).sum();
    long clicks = campaigns.stream().mapToLong(ExperimentVideoPerformanceCampaignDto::clicks).sum();
    BigDecimal spend =
        campaigns.stream()
            .map(ExperimentVideoPerformanceCampaignDto::spend)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Instant lastMetricAt =
        campaigns.stream()
            .map(ExperimentVideoPerformanceCampaignDto::metricsLastSyncedAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    long approvedAssets =
        assets.stream()
            .filter(asset -> asset.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED)
            .count();
    long metaVideoCreatives =
        metaCreatives.stream().filter(creative -> creative.kind() == AdCreativeKind.VIDEO).count();
    return new ExperimentVideoPerformanceSummaryDto(
        approvedAssets,
        metaVideoCreatives,
        impressions,
        clicks,
        funnel.diagnosticStarts(),
        funnel.checkoutAccesses(),
        funnel.purchases(),
        spend,
        lastMetricAt,
        recommend(impressions, clicks, funnel));
  }

  /** Escolhe uma recomendação objetiva conforme a etapa onde o funil está travando. */
  private String recommend(long impressions, long clicks, FunnelSnapshot funnel) {
    if (impressions == 0) {
      return "Sem amostra de entrega: aguarde impressões antes de julgar o vídeo.";
    }
    if (clicks == 0) {
      return "Atenção insuficiente: teste novo hook/primeiros 3 segundos antes de mexer na oferta.";
    }
    if (funnel.diagnosticStarts() == 0) {
      return "Clique sem início de diagnóstico: revisar primeira dobra, promessa e CTA de baixo esforço.";
    }
    if (funnel.checkoutAccesses() == 0) {
      return "Diagnóstico sem intenção de compra: reforçar mecanismo, prova e redução de risco antes do preço.";
    }
    if (funnel.purchases() == 0) {
      return "Checkout sem compra: revisar oferta, garantia, preço percebido e fricção de pagamento.";
    }
    return "Há compra registrada: comparar vídeo vencedor e preparar variações de escala.";
  }

  /** Verifica se um criativo possui tokens que correspondem ao asset do experimento. */
  private boolean matchesAsset(ExperimentVideoAsset asset, MetaCreativeSnapshot creative) {
    Set<String> assetTokens = new HashSet<>();
    addToken(assetTokens, asset.getId());
    addToken(assetTokens, asset.getAssetUrl());
    addToken(assetTokens, asset.getAsset() != null ? asset.getAsset().getId() : null);
    addToken(
        assetTokens, asset.getSalesVideoJob() != null ? asset.getSalesVideoJob().getId() : null);
    addToken(
        assetTokens,
        asset.getSalesVideoProfile() != null ? asset.getSalesVideoProfile().getId() : null);
    return assetTokens.stream().anyMatch(creative.tokens()::contains);
  }

  /** Extrai o id de vídeo Meta salvo no payload do criativo. */
  private String extractMetaVideoId(FacebookAdsAdCreative creative) {
    if (creative.getKind() != AdCreativeKind.VIDEO
        || !StringUtils.hasText(creative.getVideoDataJson())) {
      return null;
    }
    try {
      JsonNode root = OBJECT_MAPPER.readTree(creative.getVideoDataJson());
      JsonNode videoId = root.get("video_id");
      return videoId != null && !videoId.isNull() ? videoId.asText() : null;
    } catch (Exception ex) {
      return null;
    }
  }

  /** Coleta tokens textuais seguros para tentativa de vínculo entre asset e criativo. */
  private Set<String> collectCreativeTokens(FacebookAdsAdCreative creative) {
    Set<String> tokens = new HashSet<>();
    addToken(tokens, creative.getId());
    addToken(tokens, creative.getExternalId());
    addJsonTokens(tokens, creative.getVideoDataJson());
    addJsonTokens(tokens, creative.getLinkDataJson());
    return tokens;
  }

  /** Adiciona tokens de um JSON de criativo sem falhar em payload legado inválido. */
  private void addJsonTokens(Set<String> tokens, String json) {
    if (!StringUtils.hasText(json)) {
      return;
    }
    addToken(tokens, json);
    try {
      JsonNode root = OBJECT_MAPPER.readTree(json);
      collectJsonText(tokens, root);
    } catch (Exception ex) {
      // Payload legado pode não ser JSON válido; o painel apenas ignora a correspondência direta.
    }
  }

  /** Varre valores textuais do JSON para encontrar identificadores persistidos pelo worker. */
  private void collectJsonText(Set<String> tokens, JsonNode node) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isValueNode()) {
      addToken(tokens, node.asText());
      return;
    }
    node.elements().forEachRemaining(child -> collectJsonText(tokens, child));
  }

  /** Adiciona token normalizado a partir de qualquer valor não vazio. */
  private void addToken(Set<String> tokens, Object value) {
    Optional.ofNullable(value)
        .map(String::valueOf)
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(item -> item.toLowerCase(Locale.ROOT))
        .ifPresent(tokens::add);
  }

  /** Retorna lista vazia quando a coleção JPA ainda não foi inicializada. */
  private <T> List<T> safeList(Collection<T> values) {
    return values == null ? List.of() : List.copyOf(values);
  }

  /** Snapshot dos marcos comerciais usados pelo painel. */
  private record FunnelSnapshot(long diagnosticStarts, long checkoutAccesses, long purchases) {}

  /** Snapshot interno de criativo Meta usado na associação com vídeos aprovados. */
  private record MetaCreativeSnapshot(
      String campaignId,
      String creativeId,
      AdCreativeKind kind,
      String metaVideoId,
      String adId,
      String adName,
      String adStatus,
      Set<String> tokens) {}
}
