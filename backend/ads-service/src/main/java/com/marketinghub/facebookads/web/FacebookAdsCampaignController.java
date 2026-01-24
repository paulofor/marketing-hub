package com.marketinghub.facebookads.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.facebookads.AdCreativeKind;
import com.marketinghub.facebookads.BudgetMode;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.experiment.AdSet;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/facebook-campaigns")
public class FacebookAdsCampaignController {
    private final ExperimentService experimentService;
    private final FacebookAdsCampaignRepository campaignRepository;
    private final FacebookAccountRepository accountRepository;
    private final FacebookAdsAdSetRepository adSetRepository;
    private final FacebookAdsAdCreativeRepository adCreativeRepository;
    private final FacebookAdsAdRepository adRepository;
    private final com.marketinghub.experiment.repository.AdSetRepository experimentAdSetRepository;
    private final AudienceRepository audienceRepository;
    private final ObjectMapper objectMapper;

    public FacebookAdsCampaignController(ExperimentService experimentService,
                                         FacebookAdsCampaignRepository campaignRepository,
                                         FacebookAccountRepository accountRepository,
                                         FacebookAdsAdSetRepository adSetRepository,
                                         FacebookAdsAdCreativeRepository adCreativeRepository,
                                         FacebookAdsAdRepository adRepository,
                                         com.marketinghub.experiment.repository.AdSetRepository experimentAdSetRepository,
                                         AudienceRepository audienceRepository,
                                         ObjectMapper objectMapper) {
        this.experimentService = experimentService;
        this.campaignRepository = campaignRepository;
        this.accountRepository = accountRepository;
        this.adSetRepository = adSetRepository;
        this.adCreativeRepository = adCreativeRepository;
        this.adRepository = adRepository;
        this.experimentAdSetRepository = experimentAdSetRepository;
        this.audienceRepository = audienceRepository;
        this.objectMapper = objectMapper;
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
    @Transactional
    public FacebookAdsCampaign create(@RequestBody CreateCampaignRequest req) {
        Experiment experiment;
        try {
            experiment = experimentService.get(req.experimentId());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Experiment not found: " + req.experimentId(), ex);
        }
        FacebookAccount account = accountRepository.findById(req.facebookAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Facebook account not found: " + req.facebookAccountId()));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId(req.id());
        campaign.setAdAccountId(req.adAccountId());
        campaign.setName(req.name());
        campaign.setObjective(req.objective());
        campaign.setBudgetMode(req.budgetMode());
        campaign.setExperiment(experiment);
        campaign.setFacebookAccount(account);
        FacebookAdsCampaign savedCampaign = campaignRepository.save(campaign);

        FacebookAdsAdSet savedAdSet = null;
        if (req.adSet() != null) {
            AdSet experimentAdSet = resolveExperimentAdSet(req.adSet(), experiment);
            savedAdSet = adSetRepository.save(mapAdSet(req.adSet(), savedCampaign, experimentAdSet));
        }

        FacebookAdsAdCreative savedCreative = null;
        if (req.adCreative() != null) {
            savedCreative = adCreativeRepository.save(mapAdCreative(req.adCreative()));
        }

        if (req.ad() != null) {
            if (savedAdSet == null || savedCreative == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ad set and ad creative must be provided when reporting an ad"
                );
            }
            adRepository.save(mapAd(req.ad(), savedAdSet, savedCreative));
        }

        return savedCampaign;
    }

    private AdSet resolveExperimentAdSet(CreateCampaignRequest.AdSet adSetReq, Experiment experiment) {
        if (adSetReq.experimentAdSetId() == null) {
            return null;
        }
        AdSet experimentAdSet = experimentAdSetRepository.findById(adSetReq.experimentAdSetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Experiment ad set not found: " + adSetReq.experimentAdSetId()));
        if (!experimentAdSet.getExperiment().getId().equals(experiment.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Experiment ad set does not belong to experiment " + experiment.getId());
        }
        return experimentAdSet;
    }

    private FacebookAdsAdSet mapAdSet(CreateCampaignRequest.AdSet adSetReq,
                                      FacebookAdsCampaign campaign,
                                      AdSet experimentAdSet) {
        FacebookAdsAdSet adSet = new FacebookAdsAdSet();
        adSet.setId(adSetReq.id());
        adSet.setCampaign(campaign);
        adSet.setName(adSetReq.name());
        adSet.setExperimentAdSet(experimentAdSet);
        adSet.setBillingEvent(adSetReq.billingEvent());
        adSet.setOptimizationGoal(adSetReq.optimizationGoal());
        adSet.setBidStrategy(resolveBidStrategy(adSetReq.bidStrategy()));
        adSet.setDailyBudgetMinor(parseLong(adSetReq.dailyBudget()));
        adSet.setLifetimeBudgetMinor(parseLong(adSetReq.lifetimeBudget()));
        adSet.setBidAmountMinor(parseLong(adSetReq.bidAmount()));
        adSet.setPromotedObjectJson(buildPromotedObjectJson(adSetReq.pageId()));
        adSet.setTargetingJson(buildTargetingJson(adSetReq.targetCountry(), adSetReq.targetingJson(), adSetReq.savedAudienceId()));
        return adSet;
    }

    private FacebookAdsAdCreative mapAdCreative(CreateCampaignRequest.AdCreative creativeReq) {
        FacebookAdsAdCreative creative = new FacebookAdsAdCreative();
        creative.setId(creativeReq.id());
        creative.setPageId(creativeReq.pageId());
        creative.setInstagramUserId(creativeReq.instagramActorId());
        creative.setKind(AdCreativeKind.LINK);
        creative.setLinkDataJson(buildLinkDataJson(creativeReq));
        return creative;
    }

    private FacebookAdsAd mapAd(CreateCampaignRequest.Ad adReq,
                                 FacebookAdsAdSet adSet,
                                 FacebookAdsAdCreative creative) {
        FacebookAdsAd ad = new FacebookAdsAd();
        ad.setId(adReq.id());
        ad.setName(adReq.name());
        ad.setAdSet(adSet);
        ad.setCreative(creative);
        return ad;
    }

    private String resolveBidStrategy(String bidStrategy) {
        if (StringUtils.hasText(bidStrategy)) {
            return bidStrategy.trim();
        }
        return "LOWEST_COST_WITHOUT_CAP";
    }

    private Long parseLong(String numeric) {
        if (!StringUtils.hasText(numeric)) {
            return null;
        }
        try {
            return Long.parseLong(numeric.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value: " + numeric, ex);
        }
    }

    private String buildPromotedObjectJson(String pageId) {
        if (!StringUtils.hasText(pageId)) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("page_id", pageId);
        return node.toString();
    }

    private String buildTargetingJson(String targetCountry, String targetingJson, String savedAudienceId) {
        ObjectNode node;
        if (StringUtils.hasText(targetingJson)) {
            try {
                JsonNode parsed = objectMapper.readTree(targetingJson);
                if (parsed != null && parsed.isObject()) {
                    node = ((ObjectNode) parsed).deepCopy();
                } else {
                    node = objectMapper.createObjectNode();
                }
            } catch (JsonProcessingException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid targeting JSON", ex);
            }
        } else {
            node = objectMapper.createObjectNode();
        }

        if (StringUtils.hasText(savedAudienceId)) {
            node.put("saved_audience_id", savedAudienceId);
        }

        if (!node.has("geo_locations")) {
            ObjectNode geoLocations = node.putObject("geo_locations");
            ArrayNode countries = geoLocations.putArray("countries");
            if (StringUtils.hasText(targetCountry)) {
                countries.add(targetCountry);
            }
        }

        return node.toString();
    }

    private String buildLinkDataJson(CreateCampaignRequest.AdCreative creativeReq) {
        ObjectNode linkData = objectMapper.createObjectNode();
        if (StringUtils.hasText(creativeReq.websiteUrl())) {
            linkData.put("link", creativeReq.websiteUrl());
        }
        linkData.put("message", creativeReq.message());
        if (StringUtils.hasText(creativeReq.headline())) {
            linkData.put("name", creativeReq.headline());
        }
        if (StringUtils.hasText(creativeReq.description())) {
            linkData.put("description", creativeReq.description());
        }
        if (StringUtils.hasText(creativeReq.callToActionType())) {
            ObjectNode callToAction = linkData.putObject("call_to_action");
            callToAction.put("type", creativeReq.callToActionType());
            ObjectNode value = callToAction.putObject("value");
            if (StringUtils.hasText(creativeReq.websiteUrl())) {
                value.put("link", creativeReq.websiteUrl());
            }
            if (StringUtils.hasText(creativeReq.leadGenFormId())) {
                value.put("lead_gen_form_id", creativeReq.leadGenFormId());
            }
            if (value.isEmpty()) {
                callToAction.remove("value");
            }
        }
        return linkData.toString();
    }

    private ExperimentSummary toSummary(Experiment experiment) {
        String pageId = resolveExperimentPageId(experiment);
        return new ExperimentSummary(
                experiment.getId(),
                experiment.getName(),
                experiment.getHypothesis(),
                pageId,
                experiment.getKpiTargetCpl(),
                experiment.getDailyBudget(),
                experiment.getStartDate(),
                experiment.getEndDate(),
                experiment.getNiche() != null ? experiment.getNiche().getName() : null,
                experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getTitle() : null,
                computeMissingConfiguration(experiment),
                toFacebookPageSummary(experiment),
                toInstagramAccountSummary(experiment),
                toInstantFormSummary(experiment),
                isNextStepInstantForm(experiment));
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
        if (experiment.getJourneyTemplate() == null) {
            missing.add("journeyTemplate");
        }
        if (!StringUtils.hasText(resolveExperimentPageId(experiment))) {
            missing.add("pageId");
        }
        if (experiment.getInstagramAccount() == null) {
            missing.add("instagramAccount");
        }
        if (isNextStepInstantForm(experiment)) {
            if (experiment.getFacebookInstantForm() == null) {
                missing.add("facebookInstantForm");
            } else {
                if (!experiment.getFacebookInstantForm().isApproved()) {
                    missing.add("facebookInstantFormApproval");
                }
                if (!experiment.getFacebookInstantForm().isPublished()) {
                    missing.add("facebookInstantFormPublication");
                }
            }
        }
        if (!hasApprovedAudiences(experiment)) {
            missing.add("approvedAudiences");
        }
        return missing;
    }

    private boolean hasApprovedAudiences(Experiment experiment) {
        if (experiment.getNiche() == null) {
            return false;
        }
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;
        return audienceRepository.existsApprovedByNicheAndMatchingHypothesis(
                experiment.getNiche().getId(), hypothesisId);
    }

    private String resolveExperimentPageId(Experiment experiment) {
        if (experiment.getFacebookPage() == null) {
            return null;
        }
        return experiment.getFacebookPage().getPageId();
    }

    private FacebookPageSummary toFacebookPageSummary(Experiment experiment) {
        if (experiment.getFacebookPage() == null) {
            return null;
        }
        var page = experiment.getFacebookPage();
        Long accountId = page.getAccount() != null ? page.getAccount().getId() : null;
        return new FacebookPageSummary(page.getId(), accountId, page.getPageId(), page.getName());
    }

    private InstagramAccountSummary toInstagramAccountSummary(Experiment experiment) {
        if (experiment.getInstagramAccount() == null) {
            return null;
        }
        var account = experiment.getInstagramAccount();
        return new InstagramAccountSummary(account.getId(), account.getHandle(), account.getCode(), account.getName());
    }

    private InstantFormSummary toInstantFormSummary(Experiment experiment) {
        if (experiment.getFacebookInstantForm() == null) {
            return null;
        }
        var form = experiment.getFacebookInstantForm();
        return new InstantFormSummary(
                form.getId(),
                form.getFormId(),
                form.getName(),
                form.getStatus(),
                form.isApproved(),
                form.isPublished(),
                form.getShareLink()
        );
    }

    private boolean isNextStepInstantForm(Experiment experiment) {
        if (experiment.getJourneyTemplate() == null) {
            return false;
        }
        List<JourneyStep> steps = experiment.getJourneyTemplate().getSteps();
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        List<JourneyStep> ordered = steps.stream()
                .sorted(Comparator.comparingInt(step -> step.getPosition() != null ? step.getPosition() : Integer.MAX_VALUE))
                .toList();
        JourneyStep previous = null;
        for (JourneyStep step : ordered) {
            if (previous != null && previous.getStimulusType() == JourneyStimulusType.AD) {
                return step.getStimulusType() == JourneyStimulusType.INSTANT_FORM;
            }
            previous = step;
        }
        return false;
    }

    public record ExperimentSummary(
            Long id,
            String name,
            String hypothesis,
            String pageId,
            BigDecimal kpiTargetCpl,
            BigDecimal dailyBudget,
            LocalDate startDate,
            LocalDate endDate,
            String nicheName,
            String hypothesisTitle,
            List<String> missingConfiguration,
            FacebookPageSummary facebookPage,
            InstagramAccountSummary instagramAccount,
            InstantFormSummary facebookInstantForm,
            boolean nextStepInstantForm) {}

    public record FacebookPageSummary(Long id, Long accountId, String pageId, String name) {}

    public record InstagramAccountSummary(Long id, String handle, String code, String name) {}

    public record InstantFormSummary(
            Long id,
            String facebookFormId,
            String name,
            String status,
            boolean approved,
            boolean published,
            String shareLink) {}

    public record CreateCampaignRequest(
            String id,
            String adAccountId,
            String name,
            String objective,
            BudgetMode budgetMode,
            Long experimentId,
            Long facebookAccountId,
            AdSet adSet,
            AdCreative adCreative,
            Ad ad) {

        public record AdSet(
                String id,
                String name,
                String billingEvent,
                String optimizationGoal,
                String bidStrategy,
                String bidAmount,
                String dailyBudget,
                String lifetimeBudget,
                String targetCountry,
                String destinationType,
                String pageId,
                String targetingJson,
                String savedAudienceId,
                String savedAudienceName,
                Long experimentAdSetId) {}

        public record AdCreative(
                String id,
                String pageId,
                String instagramActorId,
                String websiteUrl,
                String leadGenFormId,
                String message,
                String callToActionType,
                String headline,
                String description) {}

        public record Ad(
                String id,
                String name,
                String adSetId,
                String creativeId) {}
    }
}
