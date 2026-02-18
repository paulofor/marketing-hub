package com.marketinghub.facebookads.service;

import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.facebookads.dto.ExperimentFacebookAdDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookAdSetDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ExperimentFacebookCampaignService {
    private final FacebookAdsCampaignRepository campaignRepository;

    public ExperimentFacebookCampaignService(FacebookAdsCampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public List<ExperimentFacebookCampaignDto> listByExperiment(Long experimentId) {
        if (experimentId == null) {
            return List.of();
        }
        List<FacebookAdsCampaign> campaigns = campaignRepository.findDetailedByExperimentId(experimentId);
        if (campaigns == null || campaigns.isEmpty()) {
            return List.of();
        }
        return campaigns.stream()
            .sorted(Comparator.comparing(FacebookAdsCampaign::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private ExperimentFacebookCampaignDto toDto(FacebookAdsCampaign campaign) {
        List<ExperimentFacebookAdSetDto> adSets = mapAdSets(campaign.getAdSets());
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

    private List<ExperimentFacebookAdSetDto> mapAdSets(List<FacebookAdsAdSet> adSets) {
        if (CollectionUtils.isEmpty(adSets)) {
            return List.of();
        }
        return adSets.stream()
            .sorted(Comparator.comparing(FacebookAdsAdSet::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::toAdSetDto)
            .collect(Collectors.toList());
    }

    private ExperimentFacebookAdSetDto toAdSetDto(FacebookAdsAdSet adSet) {
        List<String> issues = new ArrayList<>();
        if (adSet.getExperimentAdSet() == null) {
            issues.add("Conjunto não está vinculado à segmentação aprovada do experimento.");
        }
        List<ExperimentFacebookAdDto> ads = mapAds(adSet.getAds());
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

    private List<ExperimentFacebookAdDto> mapAds(List<FacebookAdsAd> ads) {
        if (CollectionUtils.isEmpty(ads)) {
            return List.of();
        }
        return ads.stream()
            .sorted(Comparator.comparing(FacebookAdsAd::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::toAdDto)
            .collect(Collectors.toList());
    }

    private ExperimentFacebookAdDto toAdDto(FacebookAdsAd ad) {
        return new ExperimentFacebookAdDto(
            ad.getId(),
            ad.getName(),
            ad.getStatus(),
            ad.getCreatedAt()
        );
    }
}
