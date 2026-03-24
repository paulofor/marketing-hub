package com.marketinghub.facebookads.service;

import com.marketinghub.experiment.repository.ExperimentCampaignMetricRepository;
import com.marketinghub.experiment.funnel.ExperimentFunnelAttributionService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.dto.ExperimentFacebookAdFunnelStageDto;
import com.marketinghub.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.facebookads.dto.ExperimentFacebookAdDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookAdSetDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignResetSummary;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ExperimentFacebookCampaignService {
    private final FacebookAdsCampaignRepository campaignRepository;
    private final FacebookAdsAdSetRepository adSetRepository;
    private final FacebookAdsAdRepository adRepository;
    private final FacebookAdsAdCreativeRepository adCreativeRepository;
    private final ExperimentCampaignMetricRepository campaignMetricRepository;
    private final ExperimentFunnelAttributionService funnelAttributionService;

    public ExperimentFacebookCampaignService(FacebookAdsCampaignRepository campaignRepository,
                                             FacebookAdsAdSetRepository adSetRepository,
                                             FacebookAdsAdRepository adRepository,
                                             FacebookAdsAdCreativeRepository adCreativeRepository,
                                             ExperimentCampaignMetricRepository campaignMetricRepository,
                                             ExperimentFunnelAttributionService funnelAttributionService) {
        this.campaignRepository = campaignRepository;
        this.adSetRepository = adSetRepository;
        this.adRepository = adRepository;
        this.adCreativeRepository = adCreativeRepository;
        this.campaignMetricRepository = campaignMetricRepository;
        this.funnelAttributionService = funnelAttributionService;
    }

    public List<ExperimentFacebookCampaignDto> listByExperiment(Long experimentId) {
        if (experimentId == null) {
            return List.of();
        }
        List<FacebookAdsCampaign> campaigns = campaignRepository.findDetailedByExperimentId(experimentId);
        if (campaigns == null || campaigns.isEmpty()) {
            return List.of();
        }
        hydrateAdSets(campaigns);
        Map<String, EnumMap<ExperimentFunnelStage, Long>> attribution = funnelAttributionService.aggregateByCampaignCode(experimentId);
        return campaigns.stream()
            .sorted(Comparator.comparing(FacebookAdsCampaign::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(campaign -> toDto(campaign, attribution))
            .collect(Collectors.toList());
    }

    private void hydrateAdSets(List<FacebookAdsCampaign> campaigns) {
        List<String> campaignIds = campaigns.stream()
                .map(FacebookAdsCampaign::getId)
                .filter(Objects::nonNull)
                .toList();
        if (campaignIds.isEmpty()) {
            return;
        }
        List<FacebookAdsAdSet> adSets = adSetRepository.findDetailedByCampaignIds(campaignIds);
        Map<String, List<FacebookAdsAdSet>> adSetsByCampaignId = groupAdSetsByCampaignId(adSets);
        campaigns.forEach(campaign -> campaign.setAdSets(new ArrayList<>(
                adSetsByCampaignId.getOrDefault(campaign.getId(), List.of()))));
    }

    private Map<String, List<FacebookAdsAdSet>> groupAdSetsByCampaignId(Collection<FacebookAdsAdSet> adSets) {
        Map<String, List<FacebookAdsAdSet>> grouped = new HashMap<>();
        for (FacebookAdsAdSet adSet : adSets) {
            if (adSet.getCampaign() == null || adSet.getCampaign().getId() == null) {
                continue;
            }
            grouped.computeIfAbsent(adSet.getCampaign().getId(), ignored -> new ArrayList<>()).add(adSet);
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public ExperimentFacebookCampaignResetSummary previewReset(Long experimentId) {
        return collectPendingArtifacts(experimentId).toSummary();
    }

    @Transactional
    public ExperimentFacebookCampaignResetSummary reset(Long experimentId) {
        PendingArtifacts artifacts = collectPendingArtifacts(experimentId);
        if (artifacts.campaigns().isEmpty()) {
            return artifacts.toSummary();
        }
        List<String> campaignIds = artifacts.campaignIds();
        if (!campaignIds.isEmpty()) {
            campaignMetricRepository.deleteByCampaignIds(campaignIds);
        }
        campaignRepository.deleteAllInBatch(artifacts.campaigns());
        if (!artifacts.creativeIds().isEmpty()) {
            adCreativeRepository.deleteAllByIdInBatch(artifacts.creativeIds());
        }
        return artifacts.toSummary();
    }

    private PendingArtifacts collectPendingArtifacts(Long experimentId) {
        if (experimentId == null) {
            return PendingArtifacts.empty();
        }
        List<FacebookAdsCampaign> allCampaigns = campaignRepository.findByExperimentId(experimentId);
        if (allCampaigns == null || allCampaigns.isEmpty()) {
            return PendingArtifacts.empty();
        }
        List<FacebookAdsCampaign> pendingCampaigns = allCampaigns.stream()
                .filter(c -> !StringUtils.hasText(c.getExternalId()))
                .collect(Collectors.toList());
        if (pendingCampaigns.isEmpty()) {
            return PendingArtifacts.empty();
        }
        List<String> campaignIds = pendingCampaigns.stream()
                .map(FacebookAdsCampaign::getId)
                .collect(Collectors.toList());
        List<FacebookAdsAdSet> adSets = campaignIds.isEmpty()
                ? List.of()
                : adSetRepository.findByCampaignIdIn(campaignIds);
        List<String> adSetIds = adSets.stream()
                .map(FacebookAdsAdSet::getId)
                .collect(Collectors.toList());
        List<FacebookAdsAd> ads = adSetIds.isEmpty()
                ? List.of()
                : adRepository.findByAdSetIdIn(adSetIds);
        List<String> creativeIds = ads.stream()
                .map(FacebookAdsAd::getCreative)
                .filter(Objects::nonNull)
                .map(creative -> creative.getId())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return new PendingArtifacts(pendingCampaigns, adSets, ads, creativeIds);
    }

    private ExperimentFacebookCampaignDto toDto(FacebookAdsCampaign campaign,
                                                      Map<String, EnumMap<ExperimentFunnelStage, Long>> attribution) {
        List<ExperimentFacebookAdSetDto> adSets = mapAdSets(campaign.getAdSets(), attribution);
        List<String> issues = new ArrayList<>();
        if (adSets.isEmpty()) {
            issues.add("Esta campanha foi criada, mas nenhum conjunto de anúncios foi registrado.");
        }
        return new ExperimentFacebookCampaignDto(
            campaign.getId(),
            campaign.getName(),
            campaign.getObjective(),
            campaign.getStatus(),
            campaign.getCreatedAt(),
            campaign.getUpdatedAt(),
            campaign.getMetricsLastSyncedAt(),
            campaign.getMetricsLastError(),
            adSets,
            issues
        );
    }

    private List<ExperimentFacebookAdSetDto> mapAdSets(List<FacebookAdsAdSet> adSets,
                                                            Map<String, EnumMap<ExperimentFunnelStage, Long>> attribution) {
        if (CollectionUtils.isEmpty(adSets)) {
            return List.of();
        }
        return adSets.stream()
            .sorted(Comparator.comparing(FacebookAdsAdSet::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(adSet -> toAdSetDto(adSet, attribution))
            .collect(Collectors.toList());
    }

    private ExperimentFacebookAdSetDto toAdSetDto(FacebookAdsAdSet adSet,
                                                       Map<String, EnumMap<ExperimentFunnelStage, Long>> attribution) {
        List<String> issues = new ArrayList<>();
        if (adSet.getExperimentAdSet() == null) {
            issues.add("Conjunto não está vinculado à segmentação aprovada do experimento.");
        }
        List<ExperimentFacebookAdDto> ads = mapAds(adSet.getAds(), attribution);
        if (ads.isEmpty()) {
            issues.add("Nenhum anúncio foi publicado neste conjunto de anúncios.");
        }
        Long experimentAdSetId = adSet.getExperimentAdSet() != null ? adSet.getExperimentAdSet().getId() : null;
        return new ExperimentFacebookAdSetDto(
            adSet.getId(),
            adSet.getName(),
            adSet.getStatus(),
            adSet.getCreatedAt(),
            experimentAdSetId,
            ads,
            issues
        );
    }

    private List<ExperimentFacebookAdDto> mapAds(List<FacebookAdsAd> ads,
                                                      Map<String, EnumMap<ExperimentFunnelStage, Long>> attribution) {
        if (CollectionUtils.isEmpty(ads)) {
            return List.of();
        }
        return ads.stream()
            .sorted(Comparator.comparing(FacebookAdsAd::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(ad -> toAdDto(ad, attribution))
            .collect(Collectors.toList());
    }

    private ExperimentFacebookAdDto toAdDto(FacebookAdsAd ad,
                                               Map<String, EnumMap<ExperimentFunnelStage, Long>> attribution) {
        String trackingCode = resolveTrackingCode(ad);
        String normalizedCode = normalizeTrackingCode(trackingCode);
        List<ExperimentFacebookAdFunnelStageDto> funnelStages = buildFunnelStages(normalizedCode, attribution);
        return new ExperimentFacebookAdDto(
            ad.getId(),
            ad.getName(),
            ad.getStatus(),
            ad.getCreatedAt(),
            trackingCode,
            funnelStages
        );
    }

    private List<ExperimentFacebookAdFunnelStageDto> buildFunnelStages(String normalizedCode,
                                                                         Map<String, EnumMap<ExperimentFunnelStage, Long>> attribution) {
        if (normalizedCode == null || attribution == null) {
            return List.of();
        }
        EnumMap<ExperimentFunnelStage, Long> totals = attribution.get(normalizedCode);
        if (totals == null || totals.isEmpty()) {
            return List.of();
        }
        return totals.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getOrder()))
                .map(entry -> new ExperimentFacebookAdFunnelStageDto(
                        entry.getKey(),
                        entry.getKey().getLabel(),
                        entry.getKey().getOrder(),
                        entry.getValue()))
                .toList();
    }

    private String resolveTrackingCode(FacebookAdsAd ad) {
        if (ad == null) {
            return null;
        }
        if (ad.getExternalId() != null && !ad.getExternalId().trim().isEmpty()) {
            return ad.getExternalId().trim();
        }
        return ad.getId();
    }

    private String normalizeTrackingCode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private record PendingArtifacts(
            List<FacebookAdsCampaign> campaigns,
            List<FacebookAdsAdSet> adSets,
            List<FacebookAdsAd> ads,
            List<String> creativeIds
    ) {
        static PendingArtifacts empty() {
            return new PendingArtifacts(List.of(), List.of(), List.of(), List.of());
        }

        ExperimentFacebookCampaignResetSummary toSummary() {
            return new ExperimentFacebookCampaignResetSummary(
                    campaigns.size(),
                    adSets.size(),
                    ads.size(),
                    creativeIds.size()
            );
        }

        List<String> campaignIds() {
            return campaigns.stream()
                    .map(FacebookAdsCampaign::getId)
                    .collect(Collectors.toList());
        }
    }
}
