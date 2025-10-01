package com.marketinghub.facebookads.web;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.facebookads.AdCreativeKind;
import com.marketinghub.facebookads.BudgetMode;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/facebook-campaigns")
public class FacebookAdsCampaignController {
    private final ExperimentService experimentService;
    private final FacebookAdsCampaignRepository campaignRepository;
    private final FacebookAdsAdSetRepository adSetRepository;
    private final FacebookAdsAdCreativeRepository adCreativeRepository;
    private final FacebookAdsAdRepository adRepository;

    public FacebookAdsCampaignController(ExperimentService experimentService,
                                         FacebookAdsCampaignRepository campaignRepository,
                                         FacebookAdsAdSetRepository adSetRepository,
                                         FacebookAdsAdCreativeRepository adCreativeRepository,
                                         FacebookAdsAdRepository adRepository) {
        this.experimentService = experimentService;
        this.campaignRepository = campaignRepository;
        this.adSetRepository = adSetRepository;
        this.adCreativeRepository = adCreativeRepository;
        this.adRepository = adRepository;
    }

    @GetMapping("/experiments-ready")
    public List<ExperimentSummary> experimentsReady() {
        return experimentService.listReadyForCampaign().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/experiments")
    public List<ExperimentSummary> experiments(@RequestParam("status")
            com.marketinghub.experiment.ExperimentStatus status) {
        return experimentService
                .listByStatusAndPlatform(status, com.marketinghub.experiment.ExperimentPlatform.FACEBOOK)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @PostMapping
    public FacebookAdsCampaign create(@RequestBody CreateCampaignRequest req) {
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId(req.id());
        campaign.setAdAccountId(req.adAccountId());
        campaign.setName(req.name());
        campaign.setObjective(req.objective());
        campaign.setBudgetMode(req.budgetMode());
        FacebookAdsCampaign savedCampaign = campaignRepository.save(campaign);

        FacebookAdsAdSet savedAdSet = saveAdSet(req.adSet(), savedCampaign);
        FacebookAdsAdCreative savedCreative = saveAdCreative(req.adCreative());
        saveAd(req.ad(), savedAdSet, savedCreative);

        return savedCampaign;
    }

    private FacebookAdsAdSet saveAdSet(CreateCampaignRequest.AdSetPayload payload, FacebookAdsCampaign campaign) {
        if (payload == null) {
            return null;
        }
        FacebookAdsAdSet adSet = new FacebookAdsAdSet();
        adSet.setId(payload.id());
        adSet.setCampaign(campaign);
        adSet.setName(payload.name());
        adSet.setStatus(Optional.ofNullable(payload.status()).orElse(FacebookAdStatus.PAUSED));
        adSet.setDailyBudgetMinor(payload.dailyBudgetMinor());
        adSet.setLifetimeBudgetMinor(payload.lifetimeBudgetMinor());
        adSet.setBillingEvent(payload.billingEvent());
        adSet.setOptimizationGoal(payload.optimizationGoal());
        adSet.setBidStrategy(Optional.ofNullable(payload.bidStrategy()).orElse("LOWEST_COST_WITHOUT_CAP"));
        adSet.setBidAmountMinor(payload.bidAmountMinor());
        adSet.setPromotedObjectJson(payload.promotedObjectJson());
        adSet.setTargetingJson(Optional.ofNullable(payload.targetingJson()).orElse("{}"));
        return adSetRepository.save(adSet);
    }

    private FacebookAdsAdCreative saveAdCreative(CreateCampaignRequest.AdCreativePayload payload) {
        if (payload == null) {
            return null;
        }
        FacebookAdsAdCreative creative = new FacebookAdsAdCreative();
        creative.setId(payload.id());
        creative.setPageId(payload.pageId());
        creative.setInstagramUserId(payload.instagramUserId());
        creative.setKind(Optional.ofNullable(payload.kind()).orElse(AdCreativeKind.LINK));
        creative.setLinkDataJson(payload.linkDataJson());
        creative.setVideoDataJson(payload.videoDataJson());
        creative.setCarouselDataJson(payload.carouselDataJson());
        creative.setLastPreviewUrl(payload.lastPreviewUrl());
        return adCreativeRepository.save(creative);
    }

    private void saveAd(CreateCampaignRequest.AdPayload payload,
                         FacebookAdsAdSet adSet,
                         FacebookAdsAdCreative creative) {
        if (payload == null) {
            return;
        }
        FacebookAdsAdSet targetAdSet = adSet;
        if (targetAdSet == null && payload.adSetId() != null) {
            targetAdSet = adSetRepository.findById(payload.adSetId()).orElse(null);
        }
        FacebookAdsAdCreative targetCreative = creative;
        if (targetCreative == null && payload.creativeId() != null) {
            targetCreative = adCreativeRepository.findById(payload.creativeId()).orElse(null);
        }
        if (targetAdSet == null || targetCreative == null) {
            return;
        }
        FacebookAdsAd ad = new FacebookAdsAd();
        ad.setId(payload.id());
        ad.setName(payload.name());
        ad.setStatus(Optional.ofNullable(payload.status()).orElse(FacebookAdStatus.PAUSED));
        ad.setAdSet(targetAdSet);
        ad.setCreative(targetCreative);
        adRepository.save(ad);
    }

    private ExperimentSummary toSummary(Experiment experiment) {
        return new ExperimentSummary(
                experiment.getId(),
                experiment.getName(),
                experiment.getHypothesis(),
                experiment.getPageId(),
                experiment.getKpiTargetCpl(),
                experiment.getStartDate(),
                experiment.getEndDate(),
                experiment.getNiche() != null ? experiment.getNiche().getName() : null,
                experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getTitle() : null,
                computeMissingConfiguration(experiment));
    }

    private List<String> computeMissingConfiguration(Experiment experiment) {
        List<String> missing = new ArrayList<>();
        if (!experiment.isCreativeApproved()) {
            missing.add("creativeApproval");
        }
        if (experiment.getKpiTargetCpl() == null) {
            missing.add("kpiTargetCpl");
        }
        if (experiment.getStopLossCpl() == null) {
            missing.add("stopLossCpl");
        }
        if (experiment.getSampleSize() == null) {
            missing.add("sampleSize");
        }
        if (experiment.getStartDate() == null) {
            missing.add("startDate");
        }
        if (experiment.getEndDate() == null) {
            missing.add("endDate");
        }
        if (experiment.getSalesFunnel() == null) {
            missing.add("salesFunnel");
        }
        if (!StringUtils.hasText(experiment.getPageId())) {
            missing.add("pageId");
        }
        return missing;
    }

    public record ExperimentSummary(
            Long id,
            String name,
            String hypothesis,
            String pageId,
            BigDecimal kpiTargetCpl,
            LocalDate startDate,
            LocalDate endDate,
            String nicheName,
            String hypothesisTitle,
            List<String> missingConfiguration) {}

    public record CreateCampaignRequest(
            String id,
            String adAccountId,
            String name,
            String objective,
            BudgetMode budgetMode,
            AdSetPayload adSet,
            AdCreativePayload adCreative,
            AdPayload ad) {

        public record AdSetPayload(
                String id,
                String name,
                FacebookAdStatus status,
                Long dailyBudgetMinor,
                Long lifetimeBudgetMinor,
                String billingEvent,
                String optimizationGoal,
                String bidStrategy,
                Long bidAmountMinor,
                String promotedObjectJson,
                String targetingJson) {}

        public record AdCreativePayload(
                String id,
                String pageId,
                String instagramUserId,
                AdCreativeKind kind,
                String linkDataJson,
                String videoDataJson,
                String carouselDataJson,
                String lastPreviewUrl) {}

        public record AdPayload(
                String id,
                String name,
                FacebookAdStatus status,
                String adSetId,
                String creativeId) {}
    }
}
