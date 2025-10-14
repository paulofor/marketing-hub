package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

            Experiment.InstagramAccount instagramAccount = exp.instagramAccount();
            if (instagramAccount == null || !StringUtils.hasText(instagramAccount.code())) {
                LOGGER.warn(
                    "Skipping experiment {} because no Instagram account is configured",
                    exp.id()
                );
                return;
            }

            String resolvedPageId = resolvePageId(config, exp);
            if (!StringUtils.hasText(resolvedPageId)) {
                LOGGER.warn("Skipping experiment {} because no Facebook page ID is configured", exp.id());
                return;
            }

            InstantFormDestination instantFormDestination = null;
            if (exp.nextStepInstantForm()) {
                instantFormDestination = ensureInstantFormDestination(exp);
            }

            String resolvedWebsiteUrl = resolveDestinationUrl(exp, creative, config, instantFormDestination);
            String resolvedLeadGenFormId = resolveLeadGenFormId(exp, creative, config, instantFormDestination);
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
            String resolvedInstagramActorId = coalesce(
                creative.instagramUserId(),
                instagramAccount.code(),
                config.defaultInstagramActorId()
            );
            String resolvedDestinationType = hasLeadFormDestination ? "FACEBOOK" : config.adSetDestinationType();

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
            String resolvedImageUrl = resolveCreativeImageUrl(creative.imageUrl());
            AdCreativeCreation adCreativeCreation = createAdCreativeWithFallback(
                config.adAccountId(),
                exp,
                resolvedPageId,
                resolvedInstagramActorId,
                resolvedWebsiteUrl,
                resolvedLeadGenFormId,
                resolvedMessage,
                resolvedCallToAction,
                creative.headline(),
                creative.description(),
                resolvedImageUrl
            );
            FacebookAdsService.AdCreativeRequest adCreativeRequest = adCreativeCreation.request();
            String creativeId = adCreativeCreation.id();
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
                    adCreativeRequest.imageHash(),
                    adCreativeRequest.imageUrl(),
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
        @JsonAlias({ "page", "associatedFacebookPage", "facebookPageAssociation" })
        FacebookPage facebookPage,
        InstagramAccount instagramAccount,
        @JsonAlias("facebookInstantForm")
        InstantForm facebookInstantForm,
        @JsonAlias("nextStepInstantForm")
        boolean nextStepInstantForm
    ) {
        public FacebookPage associatedPage() {
            return facebookPage;
        }

        public record FacebookPage(Long id, String pageId, String name) {}
        public record InstagramAccount(Long id, String handle, String code, String name) {}
        public record InstantForm(
            Long id,
            String facebookFormId,
            String name,
            String status,
            boolean approved,
            boolean published,
            String shareLink
        ) {}
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
            String imageHash,
            String imageUrl,
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

    private AdCreativeCreation createAdCreativeWithFallback(
        String adAccountId,
        Experiment experiment,
        String pageId,
        String instagramActorId,
        String websiteUrl,
        String leadGenFormId,
        String message,
        String callToAction,
        String headline,
        String description,
        String imageUrl
    ) {
        String imageHash = null;
        if (!StringUtils.hasText(imageUrl)) {
            LOGGER.warn(
                "Skipping Facebook ad image upload because the creative URL is empty: experimentId={}",
                experiment.id()
            );
        } else {
            LOGGER.debug(
                "Using external image URL for Facebook creative without uploading to the ad library: experimentId={}, url={}",
                experiment.id(),
                imageUrl
            );
        }

        FacebookAdsService.AdCreativeRequest primaryRequest = new FacebookAdsService.AdCreativeRequest(
            experiment.name() + " - Creative",
            pageId,
            instagramActorId,
            websiteUrl,
            leadGenFormId,
            message,
            imageHash,
            imageUrl,
            callToAction,
            headline,
            description
        );

        try {
            String creativeId = facebookAdsService.createAdCreative(adAccountId, primaryRequest);
            return new AdCreativeCreation(creativeId, primaryRequest);
        } catch (FacebookPermissionException ex) {
            if (!StringUtils.hasText(instagramActorId) || !isInstagramPermissionError(ex)) {
                throw ex;
            }

            LOGGER.warn(
                "Retrying Facebook ad creative creation without Instagram user ID after permission error: experimentId={}, instagramActorId={}, message={}, details={}",
                experiment.id(),
                instagramActorId,
                ex.getMessage(),
                ex.getErrorDetails()
            );

            FacebookAdsService.AdCreativeRequest fallbackRequest = new FacebookAdsService.AdCreativeRequest(
                experiment.name() + " - Creative",
                pageId,
                null,
                websiteUrl,
                leadGenFormId,
                message,
                imageHash,
                imageUrl,
                callToAction,
                headline,
                description
            );

            String creativeId = facebookAdsService.createAdCreative(adAccountId, fallbackRequest);
            LOGGER.info(
                "Created Facebook ad creative without Instagram user ID after permission error: experimentId={}, creativeId={}",
                experiment.id(),
                creativeId
            );
            return new AdCreativeCreation(creativeId, fallbackRequest);
        }
    }

    private boolean isInstagramPermissionError(FacebookPermissionException ex) {
        if (ex == null) {
            return false;
        }
        ObjectNode errorDetails = ex.getErrorDetails();
        if (errorDetails != null) {
            if (errorDetails.path("error_subcode").asInt() == 1815199) {
                return true;
            }
            StringBuilder combined = new StringBuilder();
            appendIfPresent(errorDetails, combined, "message");
            appendIfPresent(errorDetails, combined, "error_user_title");
            appendIfPresent(errorDetails, combined, "error_user_msg");
            if (combined.toString().toLowerCase().contains("instagram")) {
                return true;
            }
        }
        String message = ex.getMessage();
        return message != null && message.toLowerCase().contains("instagram");
    }

    private void appendIfPresent(ObjectNode errorDetails, StringBuilder builder, String field) {
        if (errorDetails.hasNonNull(field)) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(errorDetails.get(field).asText(""));
        }
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

    private InstantFormDestination ensureInstantFormDestination(Experiment experiment) {
        Experiment.InstantForm form = experiment.facebookInstantForm();
        if (form == null) {
            return null;
        }
        String facebookFormId = form.facebookFormId();
        String shareLink = form.shareLink();
        String normalizedFormId = normalizeInstantFormId(facebookFormId, shareLink);
        if (form.published()) {
            if (!StringUtils.hasText(shareLink) && StringUtils.hasText(normalizedFormId)) {
                shareLink = buildInstantFormShareLink(normalizedFormId);
            }
            return new InstantFormDestination(shareLink, normalizedFormId);
        }
        if (!StringUtils.hasText(normalizedFormId)) {
            LOGGER.warn(
                "Experiment {} references an instant form without a Facebook form ID; skipping publication",
                experiment.id()
            );
            return new InstantFormDestination(shareLink, normalizedFormId);
        }
        try {
            LOGGER.info(
                "Publishing approved instant form before creating Facebook campaign: experimentId={}, formId={}",
                experiment.id(),
                normalizedFormId
            );
            facebookAdsService.publishInstantForm(normalizedFormId);
            JsonNode details = facebookAdsService.fetchInstantForm(normalizedFormId);
            String status = details != null ? details.path("status").asText(null) : null;
            String resolvedFormId = normalizeInstantFormId(
                details != null ? details.path("id").asText(null) : normalizedFormId,
                shareLink
            );
            if (StringUtils.hasText(resolvedFormId) && !resolvedFormId.equals(normalizedFormId)) {
                LOGGER.info(
                    "Normalized instant form identifier returned by Facebook: original={}, resolved={}",
                    normalizedFormId,
                    resolvedFormId
                );
                normalizedFormId = resolvedFormId;
            }
            shareLink = buildInstantFormShareLink(normalizedFormId);
            reportInstantFormPublication(
                form.id(),
                new InstantFormPublicationUpdateRequest(true, Instant.now(), shareLink, status)
            );
        } catch (FacebookAccessTokenExpiredException ex) {
            handleAccessTokenExpirationDuringPublication(ex);
        } catch (FacebookPermissionException ex) {
            LOGGER.error(
                "Facebook permission error while publishing instant form: experimentId={}, formId={}, message={}, details={}",
                experiment.id(),
                normalizedFormId,
                ex.getMessage(),
                ex.getErrorDetails(),
                ex
            );
        } catch (Exception ex) {
            LOGGER.error(
                "Unexpected error while publishing instant form: experimentId={}, formId={}, message={}",
                experiment.id(),
                normalizedFormId,
                ex.getMessage(),
                ex
            );
        }
        return new InstantFormDestination(shareLink, normalizedFormId);
    }

    private void handleAccessTokenExpirationDuringPublication(FacebookAccessTokenExpiredException ex) {
        lastExpiredAccessToken.compareAndSet(null, facebookAdsService.getCurrentAccessToken());
        FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
            LOGGER.info(
                "Facebook access token renewed automatically after detecting expiration while publishing instant forms."
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
                "Facebook access token expired while publishing instant forms; the worker will pause publication until renewal. message={}, details={}",
                ex.getMessage(),
                ex.getErrorDetails()
            );
            logAutomaticRenewalOutcome(renewalResult);
        }
    }

    private void reportInstantFormPublication(long formId, InstantFormPublicationUpdateRequest request) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/instant-forms/" + formId + "/publication");
        LOGGER.info(
            "Reporting instant form publication to backend: url==>{}, payload={}",
            url,
            request
        );
        try {
            backendClient.patch()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info("Successfully reported instant form publication to backend: url<=={}", url);
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to report instant form publication to backend: url==>{}, message={}",
                url,
                ex.getMessage(),
                ex
            );
        }
    }

    private String buildInstantFormShareLink(String facebookFormId) {
        if (!StringUtils.hasText(facebookFormId)) {
            return null;
        }
        return "https://www.facebook.com/ads/leadgen/?id=" + facebookFormId;
    }

    private String normalizeInstantFormId(String facebookFormId, String shareLink) {
        String fromShareLink = extractInstantFormIdFromShareLink(shareLink);
        if (StringUtils.hasText(fromShareLink)) {
            return fromShareLink;
        }
        if (!StringUtils.hasText(facebookFormId)) {
            return null;
        }
        String trimmed = facebookFormId.trim();
        if (trimmed.startsWith("ai_form_")) {
            String normalized = trimmed.substring("ai_".length());
            LOGGER.info(
                "Detected AI instant form identifier; normalizing from {} to {}",
                trimmed,
                normalized
            );
            return normalized;
        }
        return trimmed;
    }

    private String extractInstantFormIdFromShareLink(String shareLink) {
        if (!StringUtils.hasText(shareLink)) {
            return null;
        }
        try {
            var uriComponents = UriComponentsBuilder.fromUriString(shareLink).build();
            String id = uriComponents.getQueryParams().getFirst("form_id");
            if (!StringUtils.hasText(id)) {
                id = uriComponents.getQueryParams().getFirst("id");
            }
            return StringUtils.hasText(id) ? id.trim() : null;
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Could not extract instant form identifier from share link {}: {}", shareLink, ex.getMessage());
            return null;
        }
    }

    private record AdCreativeCreation(String id, FacebookAdsService.AdCreativeRequest request) {}

    private record InstantFormDestination(String shareLink, String formId) {}

    private String resolveCreativeImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        String trimmed = imageUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String base = backendBaseUrl.endsWith("/") ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1) : backendBaseUrl;
        String path = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return base + path;
    }

    private String resolveDestinationUrl(
        Experiment experiment,
        Creative creative,
        FacebookWorkerConfiguration config,
        InstantFormDestination instantFormDestination
    ) {
        if (instantFormDestination != null && StringUtils.hasText(instantFormDestination.shareLink())) {
            LOGGER.info(
                "Using instant form share link as destination URL for experiment {}: link={}",
                experiment.id(),
                instantFormDestination.shareLink()
            );
            return instantFormDestination.shareLink();
        }
        return coalesce(creative.destinationUrl(), config.defaultWebsiteUrl());
    }

    private String resolveLeadGenFormId(
        Experiment experiment,
        Creative creative,
        FacebookWorkerConfiguration config,
        InstantFormDestination instantFormDestination
    ) {
        if (instantFormDestination != null && StringUtils.hasText(instantFormDestination.formId())) {
            LOGGER.info(
                "Using instant form ID as lead generation destination for experiment {}: formId={}",
                experiment.id(),
                instantFormDestination.formId()
            );
            return instantFormDestination.formId();
        }
        return coalesce(creative.leadGenFormId(), config.defaultLeadGenFormId());
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

    private record InstantFormPublicationUpdateRequest(
        boolean published,
        Instant publishedAt,
        String shareLink,
        String status
    ) {}
}
