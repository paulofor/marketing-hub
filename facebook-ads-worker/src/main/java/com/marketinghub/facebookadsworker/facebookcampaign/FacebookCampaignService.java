package com.marketinghub.facebookadsworker.facebookcampaign;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FacebookCampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignService.class);

    private final FacebookAdsService facebookAdsService;
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final String adAccountId;
    private final String adSetDailyBudget;
    private final String adSetBillingEvent;
    private final String adSetOptimizationGoal;
    private final String adSetDestinationType;
    private final String adSetTargetCountry;
    private final String pageId;
    private final String instagramActorId;
    private final String websiteUrl;
    private final String creativeMessageTemplate;
    private final String callToActionType;
    private final Set<Long> experimentsBlockedByPermissions;

    public FacebookCampaignService(FacebookAdsService facebookAdsService,
                                   WebClient.Builder builder,
                                   @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                   @Value("${backend.api-prefix:/api}") String apiPrefix,
                                   @Value("${facebook.ad-account-id}") String adAccountId,
                                   @Value("${facebook.ad-set.daily-budget:1000}") String adSetDailyBudget,
                                   @Value("${facebook.ad-set.billing-event:IMPRESSIONS}") String adSetBillingEvent,
                                   @Value("${facebook.ad-set.optimization-goal:LINK_CLICKS}") String adSetOptimizationGoal,
                                   @Value("${facebook.ad-set.destination-type:WEBSITE}") String adSetDestinationType,
                                   @Value("${facebook.ad-set.target-country:BR}") String adSetTargetCountry,
                                   @Value("${facebook.page-id}") String pageId,
                                   @Value("${facebook.instagram-actor-id:}") String instagramActorId,
                                   @Value("${facebook.website-url}") String websiteUrl,
                                   @Value("${facebook.creative.message-template:%s}") String creativeMessageTemplate,
                                   @Value("${facebook.creative.call-to-action-type:LEARN_MORE}") String callToActionType) {
        this.facebookAdsService = facebookAdsService;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.adAccountId = adAccountId;
        this.adSetDailyBudget = adSetDailyBudget;
        this.adSetBillingEvent = adSetBillingEvent;
        this.adSetOptimizationGoal = adSetOptimizationGoal;
        this.adSetDestinationType = adSetDestinationType;
        this.adSetTargetCountry = adSetTargetCountry;
        this.pageId = pageId;
        this.instagramActorId = instagramActorId;
        this.websiteUrl = websiteUrl;
        this.creativeMessageTemplate = creativeMessageTemplate;
        this.callToActionType = callToActionType;
        this.experimentsBlockedByPermissions = ConcurrentHashMap.newKeySet();
    }

    public void createCampaignsFromExperiments() {
        List<Experiment> experiments = Collections.emptyList();

        try {
            experiments = backendClient.get()
                .uri(UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/experiments-ready"))
                .exchangeToFlux(response -> {
                    if (response.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }

                    if (response.statusCode().isError()) {
                        return response.createException().flatMapMany(Mono::error);
                    }

                    return response.bodyToFlux(Experiment.class);
                })
                .collectList()
                .block();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch experiments from backend", ex);
        }

        if (experiments == null || experiments.isEmpty()) {
            return;
        }

        experiments.forEach(exp -> {
            if (experimentsBlockedByPermissions.contains(exp.id())) {
                LOGGER.warn(
                    "Skipping experiment {} due to unresolved Facebook permission error; retry requires manual intervention",
                    exp.id()
                );
                return;
            }
            processExperiment(exp);
        });
    }

    private void processExperiment(Experiment exp) {
        try {
            String campaignId = facebookAdsService.createCampaign(adAccountId, exp.name());
            String adSetId = facebookAdsService.createAdSet(adAccountId, new FacebookAdsService.AdSetRequest(
                exp.name() + " - Ad Set",
                campaignId,
                adSetDailyBudget,
                adSetBillingEvent,
                adSetOptimizationGoal,
                adSetDestinationType,
                pageId,
                adSetTargetCountry
            ));
            String creativeId = facebookAdsService.createAdCreative(adAccountId, new FacebookAdsService.AdCreativeRequest(
                exp.name() + " - Creative",
                pageId,
                instagramActorId,
                websiteUrl,
                formatCreativeMessage(exp.name()),
                callToActionType
            ));
            facebookAdsService.createAd(adAccountId, new FacebookAdsService.AdRequest(
                exp.name() + " - Ad",
                adSetId,
                creativeId
            ));
            CreateCampaignRequest req = new CreateCampaignRequest(campaignId, adAccountId, exp.name(), "OUTCOME_TRAFFIC", "CAMPAIGN");
            backendClient.post()
                .uri(UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns"))
                .bodyValue(req)
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (FacebookPermissionException ex) {
            experimentsBlockedByPermissions.add(exp.id());
            LOGGER.warn(
                "Experiment {} blocked after Facebook permission error; the worker will keep it skipped until restart",
                exp.id()
            );
            LOGGER.warn(
                "Skipping experiment due to Facebook permission error: id={}, name={}, message={}, details={}",
                exp.id(),
                exp.name(),
                ex.getMessage(),
                ex.getErrorDetails()
            );
            markExperimentAsFailed(exp.id());
        }
    }

    private String formatCreativeMessage(String experimentName) {
        if (creativeMessageTemplate == null || creativeMessageTemplate.isBlank()) {
            return experimentName;
        }
        if (creativeMessageTemplate.contains("%s")) {
            return String.format(creativeMessageTemplate, experimentName);
        }
        return creativeMessageTemplate;
    }

    public record Experiment(long id, String name) {}
    public record CreateCampaignRequest(String id, String adAccountId, String name, String objective, String budgetMode) {}

    private void markExperimentAsFailed(long experimentId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/status?status=FAILED");
        try {
            backendClient.patch()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info("Marked experiment {} as FAILED after Facebook permission error", experimentId);
        } catch (Exception ex) {
            LOGGER.warn("Could not mark experiment {} as FAILED after Facebook permission error: {}", experimentId, ex.getMessage(), ex);
        }
    }
}
