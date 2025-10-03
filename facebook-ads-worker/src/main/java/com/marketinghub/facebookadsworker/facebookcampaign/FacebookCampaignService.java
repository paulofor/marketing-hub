package com.marketinghub.facebookadsworker.facebookcampaign;

import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FacebookCampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignService.class);

    private final FacebookAdsService facebookAdsService;
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final Set<Long> experimentsBlockedByPermissions;
    private final AtomicBoolean accessTokenExpired;
    private final AtomicBoolean accessTokenExpiryWarningLogged;
    private final AtomicReference<String> lastExpiredAccessToken;
    private final FacebookAccessTokenManager accessTokenManager;
    private final AtomicBoolean configurationUnavailableWarningLogged;

    public FacebookCampaignService(FacebookAdsService facebookAdsService,
                                   FacebookAccessTokenManager accessTokenManager,
                                   WebClient.Builder builder,
                                   FacebookWorkerConfigurationClient configurationClient,
                                   @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                   @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.facebookAdsService = facebookAdsService;
        this.accessTokenManager = accessTokenManager;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.configurationClient = configurationClient;
        this.experimentsBlockedByPermissions = ConcurrentHashMap.newKeySet();
        this.accessTokenExpired = new AtomicBoolean(false);
        this.accessTokenExpiryWarningLogged = new AtomicBoolean(false);
        this.lastExpiredAccessToken = new AtomicReference<>(null);
        this.configurationUnavailableWarningLogged = new AtomicBoolean(false);
    }

    public void createCampaignsFromExperiments() {
        if (accessTokenExpired.get()) {
            if (hasTokenChangedSinceExpiration()) {
                LOGGER.info(
                    "Detected refreshed Facebook access token after a previous expiration; resuming campaign processing."
                );
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
                lastExpiredAccessToken.set(null);
            } else {
            FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
            if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
                LOGGER.info(
                    "Facebook access token renewed automatically after a previous expiration; resuming campaign processing."
                );
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
                lastExpiredAccessToken.set(null);
            } else {
                if (accessTokenExpiryWarningLogged.compareAndSet(false, true)) {
                    LOGGER.warn(
                        "Skipping Facebook campaign processing because the configured access token has expired; renew the token and restart the worker."
                    );
                    logAutomaticRenewalOutcome(renewalResult);
                }
                return;
            }
            }
        } else {
            accessTokenExpiryWarningLogged.set(false);
        }

        var configuration = configurationClient.fetchConfiguration();
        if (configuration.isEmpty()) {
            if (configurationUnavailableWarningLogged.compareAndSet(false, true)) {
                LOGGER.warn("Facebook worker configuration is unavailable; skipping campaign creation");
            }
            return;
        }
        configurationUnavailableWarningLogged.set(false);

        FacebookWorkerConfiguration config = configuration.get();
        String configuredToken = config.accessToken();
        if (!StringUtils.hasText(configuredToken)) {
            LOGGER.error("Facebook worker configuration is missing an access token; skipping campaign processing");
            return;
        }
        String currentToken = facebookAdsService.getCurrentAccessToken();
        if (!Objects.equals(configuredToken, currentToken)) {
            try {
                facebookAdsService.updateAccessToken(configuredToken);
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Facebook worker configuration returned an invalid access token: {}", ex.getMessage());
                return;
            }
        }

        List<Experiment> experiments = Collections.emptyList();
        String experimentsUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/experiments-ready");
        LOGGER.info(
            "Requesting experiments ready for Facebook campaigns from backend: url==>{}, params={}",
            experimentsUrl,
            Collections.emptyMap()
        );
        try {
            experiments = backendClient.get()
                .uri(experimentsUrl)
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
            LOGGER.info(
                "Received experiments response from backend: url<=={}, response={}",
                experimentsUrl,
                experiments
            );
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch experiments from backend: url==>{}", experimentsUrl, ex);
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
            processExperiment(exp, config);
        });
    }

    private void processExperiment(Experiment exp, FacebookWorkerConfiguration config) {
        try {
            Creative creative = resolveCreative(exp.id());
            if (creative == null) {
                LOGGER.warn("Skipping experiment {} because no creative is available or could be fetched", exp.id());
                return;
            }

            String resolvedPageId = resolvePageId(config, exp);
            if (!StringUtils.hasText(resolvedPageId)) {
                LOGGER.warn("Skipping experiment {} because no Facebook page ID is configured", exp.id());
                return;
            }

            String resolvedWebsiteUrl = coalesce(creative.destinationUrl(), config.defaultWebsiteUrl());
            String resolvedLeadGenFormId = coalesce(creative.leadGenFormId(), config.defaultLeadGenFormId());
            boolean hasWebsiteDestination = StringUtils.hasText(resolvedWebsiteUrl);
            boolean hasLeadFormDestination = StringUtils.hasText(resolvedLeadGenFormId);
            if (!hasWebsiteDestination && !hasLeadFormDestination) {
                LOGGER.warn(
                    "Skipping experiment {} because no destination URL or lead form is configured",
                    exp.id()
                );
                return;
            }

            String resolvedMessage = StringUtils.hasText(creative.primaryText())
                ? creative.primaryText()
                : formatCreativeMessage(exp.name(), config);
            String resolvedCallToAction = coalesce(creative.cta(), config.defaultCallToActionType());
            String resolvedInstagramActorId = coalesce(creative.instagramUserId(), config.defaultInstagramActorId());
            String resolvedDestinationType = hasLeadFormDestination ? "LEAD_GENERATION" : config.adSetDestinationType();

            String campaignId = facebookAdsService.createCampaign(config.adAccountId(), exp.name());
            FacebookAdsService.AdSetRequest adSetRequest = new FacebookAdsService.AdSetRequest(
                exp.name() + " - Ad Set",
                campaignId,
                config.adSetDailyBudget(),
                config.adSetBillingEvent(),
                config.adSetOptimizationGoal(),
                resolvedDestinationType,
                config.adSetBidStrategy(),
                config.adSetBidAmount(),
                resolvedPageId,
                config.adSetTargetCountry()
            );
            String adSetId = facebookAdsService.createAdSet(config.adAccountId(), adSetRequest);
            FacebookAdsService.AdCreativeRequest adCreativeRequest = new FacebookAdsService.AdCreativeRequest(
                exp.name() + " - Creative",
                resolvedPageId,
                resolvedInstagramActorId,
                resolvedWebsiteUrl,
                resolvedLeadGenFormId,
                resolvedMessage,
                resolvedCallToAction,
                creative.headline(),
                creative.description()
            );
            String creativeId = facebookAdsService.createAdCreative(config.adAccountId(), adCreativeRequest);
            FacebookAdsService.AdRequest adRequest = new FacebookAdsService.AdRequest(
                exp.name() + " - Ad",
                adSetId,
                creativeId
            );
            String adId = facebookAdsService.createAd(config.adAccountId(), adRequest);
            CreateCampaignRequest req = new CreateCampaignRequest(
                campaignId,
                config.adAccountId(),
                exp.name(),
                "OUTCOME_TRAFFIC",
                "CAMPAIGN",
                exp.id(),
                config.accountId(),
                new CreateCampaignRequest.AdSet(
                    adSetId,
                    adSetRequest.name(),
                    adSetRequest.billingEvent(),
                    adSetRequest.optimizationGoal(),
                    adSetRequest.bidStrategy(),
                    adSetRequest.bidAmount(),
                    adSetRequest.dailyBudget(),
                    null,
                    adSetRequest.targetCountry(),
                    adSetRequest.destinationType(),
                    adSetRequest.pageId()
                ),
                new CreateCampaignRequest.AdCreative(
                    creativeId,
                    adCreativeRequest.pageId(),
                    adCreativeRequest.instagramActorId(),
                    adCreativeRequest.websiteUrl(),
                    adCreativeRequest.leadGenFormId(),
                    adCreativeRequest.message(),
                    adCreativeRequest.callToActionType(),
                    adCreativeRequest.headline(),
                    adCreativeRequest.description()
                ),
                new CreateCampaignRequest.Ad(
                    adId,
                    adRequest.name(),
                    adRequest.adSetId(),
                    adRequest.creativeId()
                )
            );
            String createCampaignUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns");
            LOGGER.info(
                "Reporting Facebook campaign creation to backend: url==>{}, params={}, payload={}",
                createCampaignUrl,
                Collections.emptyMap(),
                req
            );
            backendClient.post()
                .uri(createCampaignUrl)
                .bodyValue(req)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info(
                "Successfully reported Facebook campaign creation to backend: url<=={}, experimentId={}, campaignId={}",
                createCampaignUrl,
                exp.id(),
                campaignId
            );
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
            lastExpiredAccessToken.compareAndSet(null, facebookAdsService.getCurrentAccessToken());
            FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
            if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
                LOGGER.info(
                    "Facebook access token renewed automatically after detecting expiration while processing experiment {}; the worker will retry on the next cycle.",
                    exp.id()
                );
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
                lastExpiredAccessToken.set(null);
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

    private boolean hasTokenChangedSinceExpiration() {
        String expiredToken = lastExpiredAccessToken.get();
        if (expiredToken == null) {
            return false;
        }
        String currentToken = facebookAdsService.getCurrentAccessToken();
        return currentToken != null && !currentToken.equals(expiredToken);
    }

    private String formatCreativeMessage(String experimentName, FacebookWorkerConfiguration config) {
        String template = config.defaultCreativeMessageTemplate();
        if (template == null || template.isBlank()) {
            return experimentName;
        }
        if (template.contains("%s")) {
            return String.format(template, experimentName);
        }
        return template;
    }

    private String resolvePageId(FacebookWorkerConfiguration config, Experiment experiment) {
        String configPageId = config.defaultPageId();
        Experiment.FacebookPage associatedPage = experiment.associatedPage();
        String experimentPageId = associatedPage != null ? associatedPage.pageId() : null;
        return coalesce(configPageId, experimentPageId, experiment.pageId());
    }

    public record Experiment(
        long id,
        String name,
        String pageId,
        FacebookPage page,
        FacebookPage facebookPage
    ) {
        public FacebookPage associatedPage() {
            return page != null ? page : facebookPage;
        }

        public record FacebookPage(Long id, String pageId, String name) {}
    }
    public record CreateCampaignRequest(
        String id,
        String adAccountId,
        String name,
        String objective,
        String budgetMode,
        Long experimentId,
        Long facebookAccountId,
        AdSet adSet,
        AdCreative adCreative,
        Ad ad
    ) {
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
            String pageId
        ) {}

        public record AdCreative(
            String id,
            String pageId,
            String instagramActorId,
            String websiteUrl,
            String leadGenFormId,
            String message,
            String callToActionType,
            String headline,
            String description
        ) {}

        public record Ad(
            String id,
            String name,
            String adSetId,
            String creativeId
        ) {}
    }

    private Creative resolveCreative(long experimentId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/creatives");
        LOGGER.info(
            "Requesting creatives for experiment from backend: url==>{}, params={}",
            url,
            Collections.emptyMap()
        );
        try {
            List<Creative> creatives = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Creative.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received creatives response from backend: url<=={}, response={}",
                url,
                creatives
            );
            if (creatives == null || creatives.isEmpty()) {
                return null;
            }
            return creatives.stream()
                .filter(c -> c.status() != null && "READY".equalsIgnoreCase(c.status()))
                .findFirst()
                .orElse(creatives.get(0));
        } catch (Exception ex) {
            LOGGER.warn("Failed to fetch creatives for experiment {} from backend: url==>{}, message={}", experimentId, url, ex.getMessage());
            LOGGER.debug("Stacktrace while fetching creatives for experiment {}", experimentId, ex);
            return null;
        }
    }

    private static String coalesce(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void markExperimentAsFailed(long experimentId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/status?status=FAILED");
        LOGGER.info(
            "Marking experiment as FAILED in backend: url==>{}, params={}",
            url,
            Collections.emptyMap()
        );
        try {
            backendClient.patch()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info("Marked experiment {} as FAILED after Facebook permission error", experimentId);
        } catch (Exception ex) {
            LOGGER.warn(
                "Could not mark experiment {} as FAILED after Facebook permission error: url==>{}, message={}",
                experimentId,
                url,
                ex.getMessage(),
                ex
            );
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
        String leadGenFormId,
        String instagramUserId,
        String status
    ) {}
}
