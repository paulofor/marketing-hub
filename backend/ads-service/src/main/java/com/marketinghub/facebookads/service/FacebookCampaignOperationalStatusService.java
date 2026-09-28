package com.marketinghub.facebookads.service;

import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: explicar o estado operacional real da campanha sem inferi-lo de RUNNING. */
@Service
public class FacebookCampaignOperationalStatusService {
  private static final ZoneId COMMERCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Duration STATUS_FRESHNESS = Duration.ofMinutes(15);

  private final FacebookAdsCampaignRepository campaignRepository;
  private final Clock clock;

  /** Configura a fonte persistida das campanhas e o relógio operacional. */
  @Autowired
  public FacebookCampaignOperationalStatusService(
      FacebookAdsCampaignRepository campaignRepository) {
    this(campaignRepository, Clock.systemUTC());
  }

  /** Permite validar o estado operacional com instante determinístico. */
  FacebookCampaignOperationalStatusService(
      FacebookAdsCampaignRepository campaignRepository, Clock clock) {
    this.campaignRepository = campaignRepository;
    this.clock = clock;
  }

  /** Resume configuração, janela, saldo e capacidade de entrega da campanha vigente. */
  @Transactional(readOnly = true)
  public CampaignOperationSummary summarize(Long experimentId) {
    FacebookAdsCampaign campaign = currentCampaign(experimentId);
    if (campaign == null) {
      return new CampaignOperationSummary(
          null, null, null, null, DeliveryState.NOT_PUBLISHED, false, null, null, null, null, null);
    }
    Instant now = Instant.now(clock);
    Instant windowStart = resolveWindowStart(campaign);
    Instant windowEnd = resolveWindowEnd(campaign);
    DeliveryState deliveryState = resolveDeliveryState(campaign, windowStart, windowEnd, now);
    Boolean deliveringNow =
        deliveryState == DeliveryState.ELIGIBLE_NOT_CONFIRMED ? null : Boolean.FALSE;
    return new CampaignOperationSummary(
        campaign.getId(),
        firstText(
            campaign.getMetaConfiguredStatus(),
            campaign.getStatus() != null ? campaign.getStatus().name() : null),
        campaign.getMetaEffectiveStatus(),
        campaign.getStatus() != null ? campaign.getStatus().name() : null,
        deliveryState,
        deliveringNow,
        windowStart,
        windowEnd,
        campaign.getStatusLastSyncedAt(),
        remainingBudget(campaign.getAdSets()),
        campaign.getMetricsFinalSyncedAt());
  }

  /** Seleciona a campanha vigente, desconsiderando origens explicitamente substituídas. */
  private FacebookAdsCampaign currentCampaign(Long experimentId) {
    return campaignRepository.findDetailedByExperimentId(experimentId).stream()
        .filter(campaign -> campaign.getSupersededByCampaignId() == null)
        .max(
            Comparator.comparing(
                    FacebookAdsCampaign::getCreatedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(
                    FacebookAdsCampaign::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
  }

  /** Classifica a entrega sem promover status configurado a prova de veiculação. */
  private DeliveryState resolveDeliveryState(
      FacebookAdsCampaign campaign, Instant windowStart, Instant windowEnd, Instant now) {
    if (windowEnd != null && !windowEnd.isAfter(now)) {
      return DeliveryState.WINDOW_ENDED;
    }
    if (windowStart != null && windowStart.isAfter(now)) {
      return DeliveryState.SCHEDULED;
    }
    if (campaign.getStopRequestedAt() != null && campaign.getStopCompletedAt() == null) {
      return DeliveryState.PAUSE_PENDING;
    }
    if (campaign.getStatus() != FacebookAdStatus.ACTIVE
        || (campaign.getMetaEffectiveStatus() != null
            && !"ACTIVE".equalsIgnoreCase(campaign.getMetaEffectiveStatus()))) {
      return DeliveryState.NOT_DELIVERING;
    }
    if (campaign.getStatusLastSyncedAt() == null) {
      return DeliveryState.AWAITING_STATUS_SYNC;
    }
    if (campaign.getStatusLastSyncedAt().isBefore(now.minus(STATUS_FRESHNESS))) {
      return DeliveryState.STATUS_STALE;
    }
    return DeliveryState.ELIGIBLE_NOT_CONFIRMED;
  }

  /** Resolve o primeiro início reportado pela campanha ou pelos conjuntos. */
  private Instant resolveWindowStart(FacebookAdsCampaign campaign) {
    if (campaign.getMetaStartTime() != null) {
      return campaign.getMetaStartTime();
    }
    return campaign.getAdSets().stream()
        .map(FacebookAdsAdSet::getStartTime)
        .filter(Objects::nonNull)
        .min(LocalDateTime::compareTo)
        .map(value -> value.atZone(COMMERCIAL_ZONE).toInstant())
        .orElse(null);
  }

  /** Resolve o último término reportado pela campanha ou pelos conjuntos. */
  private Instant resolveWindowEnd(FacebookAdsCampaign campaign) {
    if (campaign.getMetaStopTime() != null) {
      return campaign.getMetaStopTime();
    }
    return campaign.getAdSets().stream()
        .map(FacebookAdsAdSet::getEndTime)
        .filter(Objects::nonNull)
        .max(LocalDateTime::compareTo)
        .map(value -> value.atZone(COMMERCIAL_ZONE).toInstant())
        .orElse(null);
  }

  /** Soma os saldos dos conjuntos quando a Meta os informou. */
  private Long remainingBudget(List<FacebookAdsAdSet> adSets) {
    if (adSets == null
        || adSets.stream().noneMatch(item -> item.getBudgetRemainingMinor() != null)) {
      return null;
    }
    return adSets.stream()
        .map(FacebookAdsAdSet::getBudgetRemainingMinor)
        .filter(Objects::nonNull)
        .reduce(0L, Long::sum);
  }

  /** Retorna o primeiro texto preenchido. */
  private String firstText(String first, String second) {
    return first != null && !first.isBlank() ? first : second;
  }

  /** Estados que descrevem a possibilidade real de entrega, separada da configuração Meta. */
  public enum DeliveryState {
    NOT_PUBLISHED,
    SCHEDULED,
    WINDOW_ENDED,
    PAUSE_PENDING,
    NOT_DELIVERING,
    AWAITING_STATUS_SYNC,
    STATUS_STALE,
    ELIGIBLE_NOT_CONFIRMED
  }

  /** Contrato de leitura operacional consumido pela tela de campanhas. */
  public record CampaignOperationSummary(
      String campaignId,
      String configuredStatus,
      String effectiveStatus,
      String persistedStatus,
      DeliveryState deliveryState,
      Boolean deliveringNow,
      Instant windowStart,
      Instant windowEnd,
      Instant statusLastSyncedAt,
      Long budgetRemainingMinor,
      Instant metricsFinalSyncedAt) {}
}
