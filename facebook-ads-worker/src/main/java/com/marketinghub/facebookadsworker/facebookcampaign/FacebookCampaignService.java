package com.marketinghub.facebookadsworker.facebookcampaign;

import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final String adSetBidStrategy;
    private final String adSetBidAmount;
    private final String adSetTargetCountry;
    private final String defaultPageId;
    private final String defaultInstagramActorId;
    private final String defaultWebsiteUrl;
    private final String defaultCreativeMessageTemplate;
    private final String defaultCallToActionType;
    private final Set<Long> experimentsBlockedByPermissions;
    private final AtomicBoolean accessTokenExpired;
    private final AtomicBoolean accessTokenExpiryWarningLogged;
    private final FacebookAccessTokenManager accessTokenManager;

    public FacebookCampaignService(FacebookAdsService facebookAdsService,
                                   FacebookAccessTokenManager accessTokenManager,
                                   WebClient.Builder builder,
                                   @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                   @Value("${backend.api-prefix:/api}") String apiPrefix,
                                   @Value("${facebook.ad-account-id}") String adAccountId,
                                   @Value("${facebook.ad-set.daily-budget:1000}") String adSetDailyBudget,
                                   @Value("${facebook.ad-set.billing-event:IMPRESSIONS}") String adSetBillingEvent,
                                   @Value("${facebook.ad-set.optimization-goal:LINK_CLICKS}") String adSetOptimizationGoal,
                                   @Value("${facebook.ad-set.destination-type:WEBSITE}") String adSetDestinationType,
                                   @Value("${facebook.ad-set.bid-strategy:LOWEST_COST_WITHOUT_CAP}") String adSetBidStrategy,
                                   @Value("${facebook.ad-set.bid-amount:}") String adSetBidAmount,
                                   @Value("${facebook.ad-set.target-country:BR}") String adSetTargetCountry,
                                   @Value("${facebook.page-id}") String pageId,
                                   @Value("${facebook.instagram-actor-id:}") String instagramActorId,
                                   @Value("${facebook.website-url}") String websiteUrl,
                                   @Value("${facebook.creative.message-template:%s}") String creativeMessageTemplate,
                                   @Value("${facebook.creative.call-to-action-type:LEARN_MORE}") String callToActionType) {
        this.facebookAdsService = facebookAdsService;
        this.accessTokenManager = accessTokenManager;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.adAccountId = adAccountId;
        this.adSetDailyBudget = adSetDailyBudget;
        this.adSetBillingEvent = adSetBillingEvent;
        this.adSetOptimizationGoal = adSetOptimizationGoal;
        this.adSetDestinationType = adSetDestinationType;
        this.adSetBidStrategy = adSetBidStrategy;
        this.adSetBidAmount = adSetBidAmount;
        this.adSetTargetCountry = adSetTargetCountry;
        this.defaultPageId = pageId;
        this.defaultInstagramActorId = instagramActorId;
        this.defaultWebsiteUrl = websiteUrl;
        this.defaultCreativeMessageTemplate = creativeMessageTemplate;
        this.defaultCallToActionType = callToActionType;
        this.experimentsBlockedByPermissions = ConcurrentHashMap.newKeySet();
        this.accessTokenExpired = new AtomicBoolean(false);
        this.accessTokenExpiryWarningLogged = new AtomicBoolean(false);
    }

    public void createCampaignsFromExperiments() {
        if (accessTokenExpired.get()) {
            FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
            if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
                LOGGER.info(
                    "Facebook access token renewed automatically after a previous expiration; resuming campaign processing."
                );
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
            } else {
                if (accessTokenExpiryWarningLogged.compareAndSet(false, true)) {
                    LOGGER.warn(
                        "Skipping Facebook campaign processing because the configured access token has expired; renew the token and restart the worker."
                    );
                    logAutomaticRenewalOutcome(renewalResult);
                }
                return;
            }
        } else {
            accessTokenExpiryWarningLogged.set(false);
        }

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
            Creative creative = resolveCreative(exp.id());
            if (creative == null) {
                LOGGER.warn("Skipping experiment {} because no creative is available or could be fetched", exp.id());
                return;
            }

            String resolvedPageId = coalesce(exp.pageId(), defaultPageId);
            if (!StringUtils.hasText(resolvedPageId)) {
                LOGGER.warn("Skipping experiment {} because no Facebook page ID is configured", exp.id());
                return;
            }

            String resolvedWebsiteUrl = coalesce(creative.destinationUrl(), defaultWebsiteUrl);
            if (!StringUtils.hasText(resolvedWebsiteUrl)) {
                LOGGER.warn("Skipping experiment {} because no destination URL is configured", exp.id());
                return;
            }

            String resolvedMessage = StringUtils.hasText(creative.primaryText())
                ? creative.primaryText()
                : formatCreativeMessage(exp.name());
            String resolvedCallToAction = coalesce(creative.cta(), defaultCallToActionType);
            String resolvedInstagramActorId = coalesce(creative.instagramUserId(), defaultInstagramActorId);

            String campaignId = facebookAdsService.createCampaign(adAccountId, exp.name());
            String adSetId = facebookAdsService.createAdSet(adAccountId, new FacebookAdsService.AdSetRequest(
                exp.name() + " - Ad Set",
                campaignId,
                adSetDailyBudget,
                adSetBillingEvent,
                adSetOptimizationGoal,
                adSetDestinationType,
                adSetBidStrategy,
                adSetBidAmount,
                resolvedPageId,
                adSetTargetCountry
            ));
            String creativeId = facebookAdsService.createAdCreative(adAccountId, new FacebookAdsService.AdCreativeRequest(
                exp.name() + " - Creative",
                resolvedPageId,
                resolvedInstagramActorId,
                resolvedWebsiteUrl,
                resolvedMessage,
                resolvedCallToAction,
                creative.headline(),
                creative.description()
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
        } catch (FacebookAccessTokenExpiredException ex) {
            FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
            if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
                LOGGER.info(
                    "Facebook access token renewed automatically after detecting expiration while processing experiment {}; the worker will retry on the next cycle.",
                    exp.id()
                );
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
                return;
            }

            boolean firstDetection = accessTokenExpired.compareAndSet(false, true);
            accessTokenExpiryWarningLogged.set(false);
            if (firstDetection) {
                LOGGER.error(
                    "Facebook access token expired; the worker will pause campaign creation until the token is renewed. message={}, details={}",
                    ex.getMessage(),
                    ex.getErrorDetails()
                );
                if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED) {
                    logAutomaticRenewalOutcome(renewalResult);
                } else if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.FAILED) {
                    logAutomaticRenewalOutcome(renewalResult);
                }
            }
            LOGGER.warn(
                "Skipping experiment {} because the Facebook access token has expired; it will be retried after updating the token.",
                exp.id()
            );
        }
    }

    private String formatCreativeMessage(String experimentName) {
        if (defaultCreativeMessageTemplate == null || defaultCreativeMessageTemplate.isBlank()) {
            return experimentName;
        }
        if (defaultCreativeMessageTemplate.contains("%s")) {
            return String.format(defaultCreativeMessageTemplate, experimentName);
        }
        return defaultCreativeMessageTemplate;
    }

    public record Experiment(long id, String name, String pageId) {}
    public record CreateCampaignRequest(String id, String adAccountId, String name, String objective, String budgetMode) {}

    private Creative resolveCreative(long experimentId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/creatives");
        try {
            List<Creative> creatives = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Creative.class)
                .collectList()
                .block();
            if (creatives == null || creatives.isEmpty()) {
                return null;
            }
            return creatives.stream()
                .filter(c -> c.status() != null && "READY".equalsIgnoreCase(c.status()))
                .findFirst()
                .orElse(creatives.get(0));
        } catch (Exception ex) {
            LOGGER.warn("Failed to fetch creatives for experiment {}: {}", experimentId, ex.getMessage());
            LOGGER.debug("Stacktrace while fetching creatives for experiment {}", experimentId, ex);
            return null;
        }
    }

    private static String coalesce(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

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

    private void logAutomaticRenewalOutcome(FacebookAccessTokenManager.RenewalAttemptResult renewalResult) {
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED) {
            LOGGER.error(
                "Automatic token renewal is not configured. Provide facebook.app-id and facebook.app-secret so the worker can revalidate the access token without manual intervention."
            );
            return;
        }

        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.FAILED) {
            LOGGER.error(
                "Automatic token renewal attempt failed: {}",
                StringUtils.hasText(renewalResult.errorMessage()) ? renewalResult.errorMessage() : "unknown error"
            );
        }
    }

    public record Creative(
        Long id,
        Long experimentId,
        String format,
        String headline,
        String primaryText,
        String imageUrl,
        String description,
        String cta,
        String destinationUrl,
        String instagramUserId,
        String status
    ) {}
}
