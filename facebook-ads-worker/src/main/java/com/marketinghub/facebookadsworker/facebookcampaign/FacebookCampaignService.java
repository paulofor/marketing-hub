package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookAdsService.TargetingNormalizationException;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.facebookinstantform.InstantFormPublicationUpdateRequest;
import com.marketinghub.facebookadsworker.facebookcampaign.publication.CampaignPublicationInput;
import com.marketinghub.facebookadsworker.facebookcampaign.publication.CampaignPublicationOutput;
import com.marketinghub.facebookadsworker.facebookcampaign.publication.CampaignPublicationProcessor;
import com.marketinghub.facebookadsworker.pipeline.PipelineWorker;
import com.marketinghub.facebookadsworker.facebookapi.ExperimentFacebookApiLogClient;
import com.marketinghub.facebookadsworker.facebookapi.ExperimentFacebookApiLogContext;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.util.InstantFormPublicationHelper;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Orquestra a publicação de campanhas Facebook para experimentos liberados no Marketing Hub.
 */
@Service
public class FacebookCampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignService.class);
    private static final int AD_CREATIVE_IMAGE_DOWNLOAD_RETRY_MAX_ATTEMPTS = 3;
    private static final int AD_CREATIVE_IMAGE_DOWNLOAD_ERROR_SUBCODE = 3858258;
    private static final String IMAGE_HASH_PLATFORM = "FACEBOOK";
    private static final long MIN_REACH_LOWER_BOUND = 200_000L;
    private static final long MAX_REACH_UPPER_BOUND = 20_000_000L;
    private static final Set<String> META_CALL_TO_ACTION_TYPES = Set.of(
        "OPEN_LINK",
        "LIKE_PAGE",
        "SHOP_NOW",
        "PLAY_GAME",
        "INSTALL_APP",
        "USE_APP",
        "CALL",
        "CALL_ME",
        "INSTALL_MOBILE_APP",
        "USE_MOBILE_APP",
        "MOBILE_DOWNLOAD",
        "BOOK_TRAVEL",
        "LISTEN_MUSIC",
        "WATCH_VIDEO",
        "LEARN_MORE",
        "SIGN_UP",
        "DOWNLOAD",
        "WATCH_MORE",
        "NO_BUTTON",
        "APPLY_NOW",
        "BUY_NOW",
        "GET_OFFER",
        "GET_OFFER_VIEW",
        "BUY_TICKETS",
        "UPDATE_APP",
        "GET_DIRECTIONS",
        "BUY",
        "MESSAGE_PAGE",
        "DONATE",
        "SUBSCRIBE",
        "SELL_NOW",
        "SHARE",
        "DONATE_NOW",
        "GET_QUOTE",
        "CONTACT_US",
        "ORDER_NOW",
        "START_ORDER",
        "ADD_TO_CART",
        "GET_SHOWTIMES",
        "LISTEN_NOW",
        "SEE_MORE",
        "WHATSAPP_MESSAGE"
    );
    private static final Map<String, String> CALL_TO_ACTION_LABEL_ALIASES = buildCallToActionLabelAliases();

    private static final Duration CREATIVE_IMAGE_DOWNLOAD_TIMEOUT = Duration.ofSeconds(20);
    private static final long CREATIVE_IMAGE_MAX_BYTES = 10 * 1024 * 1024L;

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
    private final ObjectMapper objectMapper;
    private final ExperimentFacebookApiLogClient experimentFacebookApiLogClient;
    private final HttpClient assetDownloadClient;
    private final PipelineWorker<CampaignPublicationInput, CampaignPublicationOutput> campaignPublicationWorker;

    /**
     * Creates the campaign service and wires campaign publication through the generic stage worker.
     */
    public FacebookCampaignService(FacebookAdsService facebookAdsService,
                                   FacebookAccessTokenManager accessTokenManager,
                                   WebClient.Builder builder,
                                   FacebookWorkerConfigurationClient configurationClient,
                                   @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                   @Value("${backend.api-prefix:/api}") String apiPrefix,
                                   ObjectMapper objectMapper,
                                   ExperimentFacebookApiLogClient experimentFacebookApiLogClient) {
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
        this.objectMapper = objectMapper;
        this.experimentFacebookApiLogClient = experimentFacebookApiLogClient;
        this.assetDownloadClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.campaignPublicationWorker = new PipelineWorker<>(new CampaignPublicationProcessor(this::processExperiment));
    }

    private Optional<FacebookWorkerConfiguration> prepareConfiguration(String contextLabel) {
        if (accessTokenExpired.get()) {
            if (hasTokenChangedSinceExpiration()) {
                LOGGER.info("Detected refreshed Facebook access token after a previous expiration; resuming {}.", contextLabel);
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
                lastExpiredAccessToken.set(null);
            } else {
                FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
                if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
                    LOGGER.info("Facebook access token renewed automatically after a previous expiration; resuming {}.", contextLabel);
                    accessTokenExpired.set(false);
                    accessTokenExpiryWarningLogged.set(false);
                    lastExpiredAccessToken.set(null);
                } else {
                    if (accessTokenExpiryWarningLogged.compareAndSet(false, true)) {
                        LOGGER.warn("Skipping {} because the configured access token has expired; renew the token and restart the worker.", contextLabel);
                        logAutomaticRenewalOutcome(renewalResult);
                    }
                    return Optional.empty();
                }
            }
        } else {
            accessTokenExpiryWarningLogged.set(false);
        }

        var configuration = configurationClient.fetchConfiguration();
        if (configuration.isEmpty()) {
            if (configurationUnavailableWarningLogged.compareAndSet(false, true)) {
                LOGGER.warn("Facebook worker configuration is unavailable; skipping {}", contextLabel);
            }
            return Optional.empty();
        }
        configurationUnavailableWarningLogged.set(false);

        FacebookWorkerConfiguration config = configuration.get();
        String configuredToken = config.accessToken();
        if (!StringUtils.hasText(configuredToken)) {
            LOGGER.error("Facebook worker configuration is missing an access token; skipping {}", contextLabel);
            return Optional.empty();
        }
        String currentToken = facebookAdsService.getCurrentAccessToken();
        if (!Objects.equals(configuredToken, currentToken)) {
            try {
                facebookAdsService.updateAccessToken(configuredToken);
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Facebook worker configuration returned an invalid access token while preparing {}: {}", contextLabel, ex.getMessage());
                return Optional.empty();
            }
        }
        return Optional.of(config);
    }

    /**
     * Claims released experiments from the backend and executes campaign publication as pipeline stages.
     */
    public void createCampaignsFromExperiments() {
        var configuration = prepareConfiguration("campaign creation");
        if (configuration.isEmpty()) {
            return;
        }
        FacebookWorkerConfiguration config = configuration.get();

        List<Experiment> experiments = Collections.emptyList();
        String experimentsUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/experiments-ready");
        LOGGER.info(
            "Requesting experiments ready for Facebook campaigns from backend: url==>{}, params={}",
            experimentsUrl,
            JsonLogFormatter.wrap(objectMapper, Collections.emptyMap())
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
                JsonLogFormatter.wrap(objectMapper, experiments)
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
            campaignPublicationWorker.process(
                "facebook-campaign-publication",
                String.valueOf(exp.id()),
                new CampaignPublicationInput(exp, config),
                Map.of("experimentId", exp.id())
            );
        });
    }

    public void pauseCampaignsRequestedForStop() {
        var configuration = prepareConfiguration("campaign stop sync");
        if (configuration.isEmpty()) {
            return;
        }
        List<FacebookCampaignStopRequest> requests = fetchStopRequests();
        if (requests.isEmpty()) {
            return;
        }
        requests.forEach(this::handleStopRequest);
    }

    /**
     * Publica um experimento liberado no Facebook Ads usando orçamento no ad set para validação controlada e a melhor fonte de targeting disponível.
     */
    private void processExperiment(Experiment exp, FacebookWorkerConfiguration config) {
        String campaignId = null;
        String adSetId = null;
        List<String> createdAdIds = new ArrayList<>();
        boolean publishReported = false;
        try {
            List<Creative> creatives = resolveCreatives(exp.id());
            if (creatives.isEmpty()) {
                LOGGER.warn("Skipping experiment {} because no creative is available or could be fetched", exp.id());
                return;
            }

            List<Creative> readyCreatives = creatives.stream()
                .filter(c -> c.status() != null && "READY".equalsIgnoreCase(c.status()))
                .toList();
            List<Creative> selectedCreatives = readyCreatives.isEmpty()
                ? List.of(creatives.get(0))
                : readyCreatives;

            Experiment.InstagramAccount instagramAccount = exp.instagramAccount();
            String fallbackInstagramActorId = coalesce(
                instagramAccount != null ? instagramAccount.code() : null,
                config.defaultInstagramActorId()
            );

            String resolvedPageId = resolvePageId(config, exp);
            if (!StringUtils.hasText(resolvedPageId)) {
                LOGGER.warn("Skipping experiment {} because no Facebook page ID is configured", exp.id());
                return;
            }

            InstantFormResolution instantFormResolution = null;
            InstantFormDestination instantFormDestination = null;
            InstantFormPublicationUpdateRequest instantFormUpdate = null;
            if (exp.nextStepInstantForm()) {
                instantFormResolution = ensureInstantFormDestination(exp);
                if (instantFormResolution != null) {
                    instantFormDestination = instantFormResolution.destination();
                    instantFormUpdate = instantFormResolution.publicationUpdate();
                }
            }

            final InstantFormDestination resolvedInstantFormDestination = instantFormDestination;
            Creative primaryCreative = selectedCreatives.get(0);
            String resolvedWebsiteUrl = resolveDestinationUrl(exp, primaryCreative, config, instantFormDestination);
            String resolvedLeadGenFormId = resolveLeadGenFormId(exp, primaryCreative, config, instantFormDestination);
            boolean hasWebsiteDestination = StringUtils.hasText(resolvedWebsiteUrl);
            boolean hasLeadFormDestination = StringUtils.hasText(resolvedLeadGenFormId);
            if (!hasWebsiteDestination && !hasLeadFormDestination) {
                String reason = "experiment is missing a destination URL, lead portal flow URL or lead form";
                LOGGER.warn(
                    "Skipping experiment {} because {}",
                    exp.id(),
                    reason
                );
                markExperimentAsFailed(exp.id(), reason);
                return;
            }

            String defaultMessage = formatCreativeMessage(exp.name(), config);
            List<CreativePublicationPayload> creativePayloads = selectedCreatives.stream()
                .map(creative -> {
                    String websiteUrl = resolveDestinationUrl(exp, creative, config, resolvedInstantFormDestination);
                    String leadGenFormId = resolveLeadGenFormId(exp, creative, config, resolvedInstantFormDestination);
                    return new CreativePublicationPayload(
                        creative,
                        websiteUrl,
                        leadGenFormId,
                        StringUtils.hasText(creative.primaryText()) ? creative.primaryText() : defaultMessage,
                        resolveCallToActionType(creative.cta(), config.defaultCallToActionType(), StringUtils.hasText(leadGenFormId)),
                        creative.headline(),
                        creative.description(),
                        null,
                        resolveCreativeImageUrl(creative.imageUrl()),
                        coalesce(creative.instagramUserId(), fallbackInstagramActorId)
                    );
                })
                .filter(payload -> {
                    if (hasLeadFormDestination && !StringUtils.hasText(payload.leadGenFormId())) {
                        LOGGER.warn(
                            "Skipping creative {} in experiment {} because it lacks a lead form while the campaign runs in lead generation mode",
                            payload.creative().id(),
                            exp.id()
                        );
                        return false;
                    }
                    if (!hasLeadFormDestination && !StringUtils.hasText(payload.websiteUrl())) {
                        LOGGER.warn(
                            "Skipping creative {} in experiment {} because it lacks a website destination",
                            payload.creative().id(),
                            exp.id()
                        );
                        return false;
                    }
                    return true;
                })
                .toList();

            if (creativePayloads.isEmpty()) {
                String reason = "no creatives match the destination requirements for the campaign";
                LOGGER.warn(
                    "Skipping experiment {} because {}",
                    exp.id(),
                    reason
                );
                markExperimentAsFailed(exp.id(), reason);
                return;
            }

            if (!StringUtils.hasText(creativePayloads.get(0).instagramActorId())) {
                LOGGER.warn(
                    "Experiment {} does not have an Instagram actor ID; proceeding without instagram_user_id",
                    exp.id()
                );
            }
            boolean leadCampaignRequired = requiresLeadCampaign(exp) || hasLeadFormDestination;
            String resolvedDestinationType = hasLeadFormDestination ? "ON_AD" : config.adSetDestinationType();
            String resolvedOptimizationGoal = leadCampaignRequired
                ? "LEAD_GENERATION"
                : config.adSetOptimizationGoal();
            String resolvedCampaignObjective = leadCampaignRequired ? "OUTCOME_LEADS" : "OUTCOME_TRAFFIC";

            AdSetPlaybookSpec selectedSpec = null;
            ResolvedTargeting resolvedTargeting;
            String adSetName = exp.name() + " - Ad Set";
            List<AdSetPlaybookSpec> readySpecs = fetchReadyPlaybookSpecs(exp.id());
            selectedSpec = selectPrimarySpec(readySpecs);
            if (selectedSpec != null) {
                resolvedTargeting = new ResolvedTargeting(
                    normalizeAndBroadenTargetingSpec(selectedSpec.targetingSpec()),
                    Collections.emptyList()
                );
                adSetName = buildAdSetName(exp.name(), selectedSpec);
                LOGGER.info(
                    "Using ad set playbook spec {} ({}) for experiment {}",
                    selectedSpec.id(),
                    selectedSpec.slot(),
                    exp.id()
                );
            } else {
                resolvedTargeting = resolveApprovedManualTargeting(exp.id());
            }
            validateReachBeforeCampaignCreation(
                exp.publicationJobId(),
                exp.id(),
                config.adAccountId(),
                resolvedTargeting.targetingJson(),
                FacebookAdsService.BRAZIL_COUNTRY_CODE
            );
            creativePayloads = preloadCreativeImagesForExperiment(exp.publicationJobId(), exp.id(), config.adAccountId(), creativePayloads);
            try {
                campaignId = executeFacebookCallWithLogging(
                    exp.publicationJobId(),
                    exp.id(),
                    ExperimentFacebookApiLogContext.CAMPAIGN_CREATION,
                    () -> facebookAdsService.createCampaign(
                        config.adAccountId(),
                        exp.name(),
                        resolvedCampaignObjective
                    )
                );
            } catch (FacebookAccessTokenExpiredException ex) {
                throw ex;
            }
            FacebookAdsService.AdSetRequest adSetRequest = new FacebookAdsService.AdSetRequest(
                adSetName,
                campaignId,
                resolveDailyBudget(exp, config),
                config.adSetBillingEvent(),
                resolvedOptimizationGoal,
                resolvedDestinationType,
                config.adSetBidStrategy(),
                config.adSetBidAmount(),
                resolvedPageId,
                FacebookAdsService.BRAZIL_COUNTRY_CODE,
                resolvedTargeting.targetingJson(),
                resolvedTargeting.options()
            );
            adSetId = executeFacebookCallWithLogging(
                exp.publicationJobId(),
                exp.id(),
                ExperimentFacebookApiLogContext.CAMPAIGN_AD_SET,
                () -> facebookAdsService.createAdSet(config.adAccountId(), adSetRequest)
            );
            List<CreateCampaignRequest.AdCreative> reportedAdCreatives = new ArrayList<>();
            List<CreateCampaignRequest.Ad> reportedAds = new ArrayList<>();
            int creativeIndex = 1;
            for (CreativePublicationPayload payload : creativePayloads) {
                AdCreativeCreation adCreativeCreation = createAdCreativeWithFallback(
                    config.adAccountId(),
                    exp,
                    resolvedPageId,
                    payload.instagramActorId(),
                    payload.websiteUrl(),
                    payload.leadGenFormId(),
                    payload.message(),
                    payload.callToAction(),
                    payload.headline(),
                    payload.description(),
                    payload.imageHash(),
                    payload.imageUrl()
                );
                FacebookAdsService.AdCreativeRequest adCreativeRequest = adCreativeCreation.request();
                String creativeId = adCreativeCreation.id();
                String adName = creativePayloads.size() == 1
                    ? exp.name() + " - Ad"
                    : exp.name() + " - Ad " + creativeIndex;
                FacebookAdsService.AdRequest adRequest = new FacebookAdsService.AdRequest(
                    adName,
                    adSetId,
                    creativeId
                );
                String createdAdId = executeFacebookCallWithLogging(
                    exp.publicationJobId(),
                    exp.id(),
                    ExperimentFacebookApiLogContext.CAMPAIGN_AD,
                    () -> facebookAdsService.createAd(config.adAccountId(), adRequest)
                );
                createdAdIds.add(createdAdId);
                reportedAdCreatives.add(new CreateCampaignRequest.AdCreative(
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
                ));
                reportedAds.add(new CreateCampaignRequest.Ad(
                    createdAdId,
                    adRequest.name(),
                    adRequest.adSetId(),
                    adRequest.creativeId()
                ));
                creativeIndex++;
            }
            CreateCampaignRequest req = new CreateCampaignRequest(
                campaignId,
                config.adAccountId(),
                exp.name(),
                resolvedCampaignObjective,
                "ACTIVE",
                "ADSET",
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
                    adSetRequest.pageId(),
                    resolvedTargeting.targetingJson(),
                    null
                ),
                reportedAdCreatives.isEmpty() ? null : reportedAdCreatives.get(0),
                reportedAds.isEmpty() ? null : reportedAds.get(0),
                reportedAdCreatives,
                reportedAds
            );
            String createCampaignUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns");
            LOGGER.info(
                "Reporting Facebook campaign creation to backend: url==>{}, params={}, payload={}",
                createCampaignUrl,
                JsonLogFormatter.wrap(objectMapper, Collections.emptyMap()),
                JsonLogFormatter.wrap(objectMapper, req)
            );
            backendClient.post()
                .uri(createCampaignUrl)
                .bodyValue(req)
                .retrieve()
                .toBodilessEntity()
                .block();
            publishReported = true;
            LOGGER.info(
                "Successfully reported Facebook campaign creation to backend: url<=={}, experimentId={}, campaignId={}",
                createCampaignUrl,
                exp.id(),
                campaignId
            );
            if (instantFormUpdate != null && exp.facebookInstantForm() != null) {
                reportInstantFormPublication(exp.facebookInstantForm().id(), instantFormUpdate);
            }
            markExperimentAsRunning(exp.id(), campaignId);
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
            markExperimentAsFailed(exp.id(), ex.getMessage());
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
        } catch (TargetingNormalizationException ex) {
            LOGGER.error(
                "Skipping experiment {} because targeting could not be normalized: {}",
                exp.id(),
                ex.getMessage(),
                ex
            );
            markExperimentAsFailed(exp.id(), ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error(
                "Unexpected error while processing experiment {}: {}",
                exp.id(),
                ex.getMessage(),
                ex
            );
            markExperimentAsFailed(exp.id(), ex.getMessage());
        } finally {
            if (!publishReported) {
                cleanupFailedPublication(campaignId, adSetId, createdAdIds);
            }
        }
    }

    private List<FacebookCampaignStopRequest> fetchStopRequests() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/stop-requests");
        LOGGER.info(
            "Requesting Facebook campaign stop requests from backend: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(objectMapper, Collections.emptyMap())
        );
        try {
            List<FacebookCampaignStopRequest> requests = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(FacebookCampaignStopRequest.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received stop requests from backend: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, requests)
            );
            return requests != null ? requests : Collections.emptyList();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch Facebook campaign stop requests: url==>{}", url, ex);
            return Collections.emptyList();
        }
    }

    private void handleStopRequest(FacebookCampaignStopRequest request) {
        if (request == null || !StringUtils.hasText(request.id())) {
            return;
        }
        if (!StringUtils.hasText(request.externalId())) {
            LOGGER.info(
                "Campaign {} has no externalId on Facebook; marking stop request as completed without calling the API.",
                request.id()
            );
            notifyStopResult(request.id(), true, "Campaign not published on Facebook yet");
            return;
        }
        try {
            facebookAdsService.pauseCampaign(request.externalId());
            LOGGER.info("Paused Facebook campaign {} requested for experiment {}", request.externalId(), request.experimentId());
            notifyStopResult(request.id(), true, null);
        } catch (FacebookAccessTokenExpiredException ex) {
            handleAccessTokenExpiration("campaign stop sync", ex);
            notifyStopResult(request.id(), false, "Access token expired while pausing campaign");
        } catch (FacebookPermissionException ex) {
            LOGGER.error(
                "Facebook permission error while pausing campaign {}: message={}, details={}",
                request.externalId(),
                ex.getMessage(),
                ex.getErrorDetails(),
                ex
            );
            notifyStopResult(request.id(), false, ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("Unexpected error while pausing campaign {}: {}", request.externalId(), ex.getMessage(), ex);
            notifyStopResult(request.id(), false, ex.getMessage());
        }
    }

    private void notifyStopResult(String campaignId, boolean success, String message) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/stop-results");
        FacebookCampaignStopResultPayload payload = new FacebookCampaignStopResultPayload(success, message);
        LOGGER.info(
            "Reporting Facebook campaign stop result to backend: url==>{}, payload={}",
            url,
            JsonLogFormatter.wrap(objectMapper, payload)
        );
        try {
            backendClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (Exception ex) {
            LOGGER.warn("Failed to notify backend about stop result for campaign {}: {}", campaignId, ex.getMessage());
        }
    }

    private void cleanupFailedPublication(String campaignId, String adSetId, List<String> adIds) {
        if (!StringUtils.hasText(campaignId)) {
            return;
        }
        if (adIds != null) {
            for (String adId : adIds) {
                if (!StringUtils.hasText(adId)) {
                    continue;
                }
                try {
                    facebookAdsService.deleteAd(adId);
                } catch (Exception cleanupEx) {
                    LOGGER.warn("Failed to delete Facebook ad {} during cleanup: {}", adId, cleanupEx.getMessage());
                }
            }
        }
        if (StringUtils.hasText(adSetId)) {
            try {
                facebookAdsService.deleteAdSet(adSetId);
            } catch (Exception cleanupEx) {
                LOGGER.warn("Failed to delete Facebook ad set {} during cleanup: {}", adSetId, cleanupEx.getMessage());
            }
        }
        try {
            facebookAdsService.deleteCampaign(campaignId);
        } catch (Exception cleanupEx) {
            LOGGER.warn("Failed to delete Facebook campaign {} during cleanup: {}", campaignId, cleanupEx.getMessage());
        }
    }

    /**
     * Resolve o pacote manual aprovado no backend e bloqueia apenas quando não há nenhum item publicável.
     */
    private ResolvedTargeting resolveApprovedManualTargeting(long experimentId) {
        JsonNode targeting = fetchApprovedTargetingPackage(experimentId);
        if (targeting == null || targeting.isMissingNode() || targeting.isNull()) {
            throw new IllegalStateException(
                "Experimento " + experimentId + " sem pacote de segmentação aprovado; publicação bloqueada para evitar público amplo"
            );
        }

        ObjectNode targetingSpec = objectMapper.createObjectNode();
        appendTargetingElements(targetingSpec, "interests", targeting.path("interests"));
        appendTargetingElements(targetingSpec, "work_positions", targeting.path("jobTitles"));
        appendTargetingElements(targetingSpec, "behaviors", targeting.path("behaviors"));
        if (targetingSpec.isEmpty()) {
            throw new IllegalStateException(
                "Experimento " + experimentId + " sem itens aprovados no targeting manual; publicação bloqueada para evitar público amplo"
            );
        }
        ObjectNode broadenedTargetingSpec = broadenTargetingSpecToOrAudience(targetingSpec);
        LOGGER.info(
            "Using backend-approved manual targeting package for experiment {}: {}",
            experimentId,
            JsonLogFormatter.wrap(objectMapper, broadenedTargetingSpec)
        );
        return new ResolvedTargeting(broadenedTargetingSpec.toString(), Collections.emptyList());
    }

    /**
     * Fetches only the approved targeting package for the experiment to avoid loading the full readiness payload.
     */
    private JsonNode fetchApprovedTargetingPackage(long experimentId) {
        String url = UrlUtils.joinPath(
            backendBaseUrl,
            apiPrefix,
            "/facebook-adsets/experiments/" + experimentId + "/targeting-package"
        );
        LOGGER.info(
            "Requesting approved targeting package from backend: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(objectMapper, Map.of("experimentId", experimentId))
        );
        try {
            JsonNode response = backendClient.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            LOGGER.info(
                "Received approved targeting package from backend: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, response)
            );
            if (response == null || !response.isObject()) {
                return null;
            }
            JsonNode targeting = response.path("targeting");
            return targeting.isMissingNode() || targeting.isNull() ? null : targeting;
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to fetch approved targeting package for experiment {} from backend: url==>{}, message={}",
                experimentId,
                url,
                ex.getMessage(),
                ex
            );
            return null;
        }
    }

    /**
     * Appends approved targeting elements to a Meta targeting field, preferring official Meta IDs.
     */
    private void appendTargetingElements(ObjectNode targetingSpec, String fieldName, JsonNode elements) {
        if (targetingSpec == null || elements == null || !elements.isArray() || elements.isEmpty()) {
            return;
        }
        ArrayNode array = objectMapper.createArrayNode();
        for (JsonNode element : elements) {
            String name = coalesce(
                textValue(element, "metaKey"),
                textValue(element, "term"),
                textValue(element, "name")
            );
            String id = textValue(element, "metaId");
            if (!StringUtils.hasText(name) && !StringUtils.hasText(id)) {
                continue;
            }
            ObjectNode item = objectMapper.createObjectNode();
            if (StringUtils.hasText(id)) {
                item.put("id", id.trim());
            }
            if (StringUtils.hasText(name)) {
                item.put("name", name.trim());
            }
            array.add(item);
        }
        if (!array.isEmpty()) {
            targetingSpec.set(fieldName, array);
        }
    }

    /**
     * Reads a text field from a JSON object when present.
     */
    private String textValue(JsonNode node, String fieldName) {
        if (node == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return StringUtils.hasText(text) ? text.trim() : null;
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

    private String resolveDailyBudget(Experiment experiment, FacebookWorkerConfiguration config) {
        BigDecimal dailyBudget = experiment.dailyBudget();
        if (dailyBudget != null && dailyBudget.compareTo(BigDecimal.ZERO) > 0) {
            return dailyBudget.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return config.adSetDailyBudget();
    }

    /**
     * Valida o alcance do público na Meta antes de criar qualquer campanha do experimento.
     */
    private void validateReachBeforeCampaignCreation(String publicationJobId,
                                                     long experimentId,
                                                     String adAccountId,
                                                     String targetingJson,
                                                     String targetCountry) {
        ObjectNode targetingSpec = parseTargetingSpecForReachValidation(experimentId, targetingJson);
        ensureGeoLocationForReachValidation(targetingSpec, targetCountry);
        JsonNode response = executeFacebookCallWithLogging(
            publicationJobId,
            experimentId,
            ExperimentFacebookApiLogContext.CAMPAIGN_REACH_VALIDATION,
            () -> facebookAdsService.estimateReach(new FacebookAdsService.ReachEstimateRequest(adAccountId, targetingSpec))
        );
        ReachEstimateBounds bounds = extractReachEstimateBounds(response);
        if (!bounds.complete()) {
            String message = "A Meta não retornou os limites de alcance do público. A publicação seguirá como teste controlado porque a ausência de estimativa não prova inviabilidade comercial; monitore entrega, CPM, CTR, leads e vendas nas primeiras horas.";
            experimentFacebookApiLogClient.logPublicationJobFailureStep(
                publicationJobId,
                experimentId,
                "CAMPAIGN_REACH_VALIDATION_WARNING",
                message
            );
            LOGGER.warn(
                "Reach estimate bounds unavailable; continuing controlled publication: experimentId={}, response={}",
                experimentId,
                JsonLogFormatter.wrap(response)
            );
            return;
        }
        if (bounds.lowerBound() < MIN_REACH_LOWER_BOUND || bounds.upperBound() > MAX_REACH_UPPER_BOUND) {
            String message = "Público pequeno demais para publicar: a Meta estimou %d a %d pessoas, mas o mínimo operacional é %d. Revise o público usando critérios mais amplos em OU e libere novamente."
                .formatted(bounds.lowerBound(), bounds.upperBound(), MIN_REACH_LOWER_BOUND);
            experimentFacebookApiLogClient.logPublicationJobFailureStep(
                publicationJobId,
                experimentId,
                "CAMPAIGN_REACH_VALIDATION_BLOCKED",
                message
            );
            throw new IllegalStateException(message);
        }
        LOGGER.info(
            "Reach validation approved before campaign creation: experimentId={}, lowerBound={}, upperBound={}",
            experimentId,
            bounds.lowerBound(),
            bounds.upperBound()
        );
    }

    /**
     * Converte o targeting JSON aprovado em objeto validável pela Graph API.
     */
    private ObjectNode parseTargetingSpecForReachValidation(long experimentId, String targetingJson) {
        if (!StringUtils.hasText(targetingJson)) {
            throw new IllegalStateException("Targeting vazio para validação de alcance do experimento " + experimentId);
        }
        try {
            JsonNode parsed = objectMapper.readTree(targetingJson);
            if (!parsed.isObject()) {
                throw new IllegalStateException("Targeting precisa ser um objeto JSON");
            }
            return (ObjectNode) parsed;
        } catch (Exception ex) {
            throw new IllegalStateException(
                "Targeting inválido para validação de alcance do experimento " + experimentId + ": " + ex.getMessage(),
                ex
            );
        }
    }

    /**
     * Garante localização no payload de estimativa para manter o contrato mínimo exigido pela Meta.
     */
    private void ensureGeoLocationForReachValidation(ObjectNode targetingSpec, String targetCountry) {
        JsonNode existingGeoLocations = targetingSpec.path("geo_locations");
        if (existingGeoLocations.isObject() && existingGeoLocations.size() > 0) {
            return;
        }
        ObjectNode geoLocations = targetingSpec.putObject("geo_locations");
        ArrayNode countries = geoLocations.putArray("countries");
        countries.add(StringUtils.hasText(targetCountry) ? targetCountry : FacebookAdsService.BRAZIL_COUNTRY_CODE);
    }

    /**
     * Extrai os limites de usuários retornados pelo endpoint reachestimate.
     */
    private ReachEstimateBounds extractReachEstimateBounds(JsonNode response) {
        JsonNode dataNode = response != null && response.path("data").isArray() && response.path("data").size() > 0
            ? response.path("data").get(0)
            : response;
        Long lowerBound = readLong(dataNode, "users_lower_bound");
        Long upperBound = readLong(dataNode, "users_upper_bound");
        return new ReachEstimateBounds(lowerBound, upperBound);
    }

    /**
     * Lê um campo numérico opcional de uma resposta JSON.
     */
    private Long readLong(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asLong();
    }

    public record ResolvedTargeting(String targetingJson, List<FacebookAdsService.TargetingOption> options) {}

    private record ReachEstimateBounds(Long lowerBound, Long upperBound) {
        /**
         * Indica se a Meta retornou os dois limites necessários para decidir a publicação.
         */
        boolean complete() {
            return lowerBound != null && upperBound != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdSetPlaybookWorkflowResponse(
        Long workflowId,
        String status,
        String lastError,
        List<AdSetPlaybookSpec> specs
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdSetPlaybookSpec(
        Long id,
        String slot,
        String label,
        Integer ageMin,
        Integer ageMax,
        String targetingSpec,
        String validationStatus,
        String reachStatus,
        Instant createdAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FacebookCampaignStopRequest(
            @JsonProperty("id") String id,
            @JsonProperty("externalId") String externalId,
            String adAccountId,
            Long experimentId,
            String stopReason
    ) {}

    private record FacebookCampaignStopResultPayload(boolean success, String message) {}

    /** Indica se o contrato comercial do experimento exige campanha otimizada para Leads. */
    private boolean requiresLeadCampaign(Experiment experiment) {
        if (experiment == null) {
            return false;
        }
        return (StringUtils.hasText(experiment.campaignObjective())
                && "LEADS".equalsIgnoreCase(experiment.campaignObjective().trim()))
            || StringUtils.hasText(experiment.freeReward());
    }

    public record Experiment(
        long id,
        String name,
        String singlePain,
        String freeReward,
        String funnelPromise,
        String primaryCta,
        String campaignObjective,
        String pageId,
        BigDecimal dailyBudget,
        @JsonAlias({ "page", "associatedFacebookPage", "facebookPageAssociation" })
        FacebookPage facebookPage,
        InstagramAccount instagramAccount,
        @JsonAlias("facebookInstantForm")
        InstantForm facebookInstantForm,
        @JsonAlias("nextStepInstantForm")
        boolean nextStepInstantForm,
        String followUpActionUrl,
        String publicationJobId,
        LeadPortalFlow leadPortalFlow
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
        public record LeadPortalFlow(Long id, String name, String slug, String publicUrl) {}
    }

    public record CreateCampaignRequest(
        String id,
        String adAccountId,
        String name,
        String objective,
        String status,
        String budgetMode,
        Long experimentId,
        Long facebookAccountId,
        AdSet adSet,
        AdCreative adCreative,
        Ad ad,
        List<AdCreative> adCreatives,
        List<Ad> ads
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
            String pageId,
            String targetingJson,
            Long experimentAdSetId
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

    private record CreativePublicationPayload(
        Creative creative,
        String websiteUrl,
        String leadGenFormId,
        String message,
        String callToAction,
        String headline,
        String description,
        String imageHash,
        String imageUrl,
        String instagramActorId
    ) {
        private CreativePublicationPayload withImageHash(String value) {
            return new CreativePublicationPayload(
                creative,
                websiteUrl,
                leadGenFormId,
                message,
                callToAction,
                headline,
                description,
                value,
                imageUrl,
                instagramActorId
            );
        }
    }

    private List<Creative> resolveCreatives(long experimentId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/experiments/" + experimentId + "/creatives-ready");
        LOGGER.info(
            "Requesting creatives for experiment from backend: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(objectMapper, Collections.emptyMap())
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
                JsonLogFormatter.wrap(objectMapper, creatives)
            );
            if (creatives == null) {
                return Collections.emptyList();
            }
            return creatives;
        } catch (Exception ex) {
            LOGGER.warn("Failed to fetch creatives for experiment {} from backend: url==>{}, message={}", experimentId, url, ex.getMessage());
            LOGGER.debug("Stacktrace while fetching creatives for experiment {}", experimentId, ex);
            return Collections.emptyList();
        }
    }

    private List<AdSetPlaybookSpec> fetchReadyPlaybookSpecs(long experimentId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/adset-playbook");
        try {
            AdSetPlaybookWorkflowResponse response = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(AdSetPlaybookWorkflowResponse.class)
                .block();
            if (response == null || response.specs() == null) {
                return Collections.emptyList();
            }
            return response.specs().stream()
                .filter(spec -> spec != null && StringUtils.hasText(spec.targetingSpec()))
                .filter(spec -> "VALID".equalsIgnoreCase(spec.validationStatus()))
                .filter(spec -> "READY".equalsIgnoreCase(spec.reachStatus()))
                .toList();
        } catch (Exception ex) {
            LOGGER.warn("Failed to fetch ad set playbook for experiment {}: {}", experimentId, ex.getMessage());
            LOGGER.debug("Stacktrace while fetching ad set playbook for experiment {}", experimentId, ex);
            return Collections.emptyList();
        }
    }

    private AdSetPlaybookSpec selectPrimarySpec(List<AdSetPlaybookSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return null;
        }
        return specs.stream()
            .sorted(Comparator.<AdSetPlaybookSpec>comparingInt(spec -> slotPriority(spec.slot()))
                .thenComparing(spec -> spec.id() != null ? spec.id() : Long.MAX_VALUE))
            .findFirst()
            .orElse(null);
    }

    private int slotPriority(String slot) {
        if (!StringUtils.hasText(slot)) {
            return Integer.MAX_VALUE;
        }
        return switch (slot.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "MARKETING" -> 0;
            case "SMB" -> 1;
            case "DESIGNERS" -> 2;
            default -> 3;
        };
    }

    private String buildAdSetName(String experimentName, AdSetPlaybookSpec spec) {
        if (spec != null) {
            if (StringUtils.hasText(spec.label())) {
                return experimentName + " - " + spec.label().trim();
            }
            if (StringUtils.hasText(spec.slot())) {
                return experimentName + " - " + spec.slot().trim();
            }
        }
        return experimentName + " - Ad Set";
    }

    /**
     * Normaliza o targeting aprovado e amplia o público colocando interesses, cargos e comportamentos em condição OU.
     */
    private String normalizeAndBroadenTargetingSpec(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node instanceof ObjectNode objectNode) {
                return objectMapper.writeValueAsString(broadenTargetingSpecToOrAudience(objectNode));
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            LOGGER.warn("Failed to normalize targeting spec JSON: {}", ex.getMessage());
            LOGGER.debug("Stacktrace while normalizing targeting spec JSON", ex);
            return raw;
        }
    }

    /**
     * Move critérios detalhados para um único flexible_spec para a Meta tratar o público como OU, não como E entre campos.
     */
    private ObjectNode broadenTargetingSpecToOrAudience(ObjectNode source) {
        ObjectNode broadened = source.deepCopy();
        ObjectNode orGroup = objectMapper.createObjectNode();
        moveArrayFieldToOrGroup(broadened, orGroup, "interests");
        moveArrayFieldToOrGroup(broadened, orGroup, "work_positions");
        moveArrayFieldToOrGroup(broadened, orGroup, "behaviors");
        mergeExistingFlexibleSpecIntoOrGroup(broadened, orGroup);
        if (orGroup.size() > 0) {
            ArrayNode flexibleSpec = objectMapper.createArrayNode();
            flexibleSpec.add(orGroup);
            broadened.set("flexible_spec", flexibleSpec);
        }
        return broadened;
    }

    /**
     * Move um array de segmentação do topo para o grupo OU quando houver itens úteis.
     */
    private void moveArrayFieldToOrGroup(ObjectNode targetingSpec, ObjectNode orGroup, String fieldName) {
        JsonNode value = targetingSpec.get(fieldName);
        targetingSpec.remove(fieldName);
        if (value != null && value.isArray() && !value.isEmpty()) {
            orGroup.set(fieldName, value);
        }
    }

    /**
     * Consolida flexible_spec existente em um único grupo OU para evitar estreitamento por múltiplos grupos.
     */
    private void mergeExistingFlexibleSpecIntoOrGroup(ObjectNode targetingSpec, ObjectNode orGroup) {
        JsonNode existingFlexibleSpec = targetingSpec.get("flexible_spec");
        targetingSpec.remove("flexible_spec");
        if (existingFlexibleSpec == null || !existingFlexibleSpec.isArray()) {
            return;
        }
        for (JsonNode group : existingFlexibleSpec) {
            if (!group.isObject()) {
                continue;
            }
            mergeArrayField(orGroup, group, "interests");
            mergeArrayField(orGroup, group, "work_positions");
            mergeArrayField(orGroup, group, "behaviors");
        }
    }

    /**
     * Junta arrays de uma mesma categoria preservando os objetos oficiais retornados pela Meta.
     */
    private void mergeArrayField(ObjectNode target, JsonNode source, String fieldName) {
        JsonNode sourceArray = source.get(fieldName);
        if (sourceArray == null || !sourceArray.isArray() || sourceArray.isEmpty()) {
            return;
        }
        ArrayNode targetArray;
        JsonNode existing = target.get(fieldName);
        if (existing instanceof ArrayNode existingArray) {
            targetArray = existingArray;
        } else {
            targetArray = objectMapper.createArrayNode();
            target.set(fieldName, targetArray);
        }
        sourceArray.forEach(targetArray::add);
    }

    private <T> T executeFacebookCallWithLogging(String publicationJobId,
                                                 Long experimentId,
                                                 ExperimentFacebookApiLogContext context,
                                                 Supplier<T> action) {
        facebookAdsService.clearLastApiCallDebugInfo();
        try {
            return action.get();
        } finally {
            FacebookAdsService.FacebookApiCallDebugInfo debugInfo = facebookAdsService.consumeLastApiCallDebugInfo();
            experimentFacebookApiLogClient.logCall(
                experimentId,
                context,
                debugInfo
            );
            experimentFacebookApiLogClient.logPublicationJobStep(
                publicationJobId,
                experimentId,
                context.name(),
                debugInfo
            );
        }
    }


    /**
     * Uploads a previously downloaded creative image to Meta as multipart bytes and returns its image_hash.
     */
    private String uploadDownloadedAdImage(String adAccountId, String imageUrl, DownloadedImage downloaded) {
        return facebookAdsService.uploadAdImageFromBytes(
            adAccountId,
            downloaded.bytes(),
            resolveImageFileName(imageUrl),
            downloaded.contentType()
        );
    }

    private DownloadedImage downloadCreativeImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("imageUrl must not be blank");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .timeout(CREATIVE_IMAGE_DOWNLOAD_TIMEOUT)
                .header(HttpHeaders.USER_AGENT, "MarketingHubFacebookAdsWorker/1.0")
                .build();
            HttpResponse<byte[]> response = assetDownloadClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Creative image download failed with status " + status);
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new IllegalStateException("Creative image download returned an empty body");
            }
            if (body.length > CREATIVE_IMAGE_MAX_BYTES) {
                throw new IllegalStateException("Creative image exceeds max allowed size");
            }
            String contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(null);
            return new DownloadedImage(body, contentType);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Creative image download interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to download creative image: " + ex.getMessage(), ex);
        }
    }

    private String resolveImageFileName(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return "creative-" + UUID.randomUUID() + ".jpg";
        }
        try {
            String path = URI.create(imageUrl).getPath();
            if (StringUtils.hasText(path)) {
                int idx = path.lastIndexOf('/');
                if (idx >= 0 && idx + 1 < path.length()) {
                    String candidate = path.substring(idx + 1);
                    if (StringUtils.hasText(candidate)) {
                        return candidate;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("Failed to extract filename from image URL {}: {}", imageUrl, ex.getMessage());
        }
        return "creative-" + UUID.randomUUID() + ".jpg";
    }

    private record DownloadedImage(byte[] bytes, String contentType) {}
    private record CanonicalImageHashUpsertRequest(
        String platform,
        String adAccountId,
        String localHash,
        String metaImageHash
    ) {}
    private record CanonicalImageHashResponse(
        @JsonAlias({"metaImageHash", "meta_image_hash", "imageHash", "image_hash"}) String metaImageHash
    ) {}

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(raw);
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

    /**
     * Monta os aliases de labels comerciais para o enum tecnico aceito pela Meta.
     */
    private static Map<String, String> buildCallToActionLabelAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("saiba mais", "LEARN_MORE");
        aliases.put("conheca mais", "LEARN_MORE");
        aliases.put("ver mais", "LEARN_MORE");
        aliases.put("abrir", "LEARN_MORE");
        aliases.put("abrir a planilha de evidencias", "LEARN_MORE");
        aliases.put("acessar", "LEARN_MORE");
        aliases.put("quero conhecer", "LEARN_MORE");
        aliases.put("inscrever se", "SIGN_UP");
        aliases.put("cadastre se", "SIGN_UP");
        aliases.put("receber a previa", "SIGN_UP");
        aliases.put("comprar agora", "SHOP_NOW");
        aliases.put("comprar", "SHOP_NOW");
        aliases.put("fale conosco", "CONTACT_US");
        aliases.put("pedir cotacao", "GET_QUOTE");
        aliases.put("baixar", "DOWNLOAD");
        return Map.copyOf(aliases);
    }

    /**
     * Resolve o tipo tecnico de CTA que sera enviado para o campo call_to_action.type da Meta.
     */
    private String resolveCallToActionType(String creativeCallToAction, String defaultCallToActionType, boolean leadFormDestination) {
        Optional<String> creativeType = normalizeCallToActionCandidate(creativeCallToAction);
        if (creativeType.isPresent()) {
            return creativeType.get();
        }
        Optional<String> defaultType = normalizeCallToActionCandidate(defaultCallToActionType);
        if (defaultType.isPresent()) {
            return defaultType.get();
        }
        String fallback = leadFormDestination ? "SIGN_UP" : "LEARN_MORE";
        if (StringUtils.hasText(creativeCallToAction) || StringUtils.hasText(defaultCallToActionType)) {
            LOGGER.warn(
                "CTA comercial nao corresponde a enum tecnico da Meta; usando fallback: creativeCta={}, defaultCta={}, fallback={}",
                creativeCallToAction,
                defaultCallToActionType,
                fallback
            );
        }
        return fallback;
    }

    /**
     * Normaliza um candidato de CTA para enum Meta ou alias comercial conhecido.
     */
    private Optional<String> normalizeCallToActionCandidate(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return Optional.empty();
        }
        String trimmed = candidate.trim();
        String enumCandidate = trimmed.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (META_CALL_TO_ACTION_TYPES.contains(enumCandidate)) {
            return Optional.of(enumCandidate);
        }
        String normalizedLabel = normalizeCallToActionLabel(trimmed);
        String alias = CALL_TO_ACTION_LABEL_ALIASES.get(normalizedLabel);
        if (alias != null) {
            return Optional.of(alias);
        }
        return Optional.empty();
    }

    /**
     * Remove acentos e pontuacao para comparar labels comerciais de CTA.
     */
    private static String normalizeCallToActionLabel(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }

    /**
     * Cria o criativo com a CTA tecnica ja normalizada e aplica fallback sem Instagram quando necessario.
     */
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
        String imageHash,
        String imageUrl
    ) {
        if (StringUtils.hasText(imageHash)) {
            LOGGER.info(
                "Using image_hash as primary asset for creative publication: experimentId={}, hash={}",
                experiment.id(),
                imageHash
            );
        } else if (!StringUtils.hasText(imageUrl)) {
            LOGGER.warn(
                "Creating Facebook ad creative without image because URL and image_hash are empty: experimentId={}",
                experiment.id()
            );
        } else {
            LOGGER.warn(
                "Falling back to external image URL because image_hash is unavailable: experimentId={}, url={}",
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
            String creativeId = createAdCreativeWithImageDownloadRetry(adAccountId, experiment.publicationJobId(), experiment.id(), primaryRequest);
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

            String creativeId = createAdCreativeWithImageDownloadRetry(adAccountId, experiment.publicationJobId(), experiment.id(), fallbackRequest);
            LOGGER.info(
                "Created Facebook ad creative without Instagram user ID after permission error: experimentId={}, creativeId={}",
                experiment.id(),
                creativeId
            );
            return new AdCreativeCreation(creativeId, fallbackRequest);
        }
    }

    /**
     * Resolves every creative image to a Meta image_hash before campaign publication.
     */
    private List<CreativePublicationPayload> preloadCreativeImagesForExperiment(
        String publicationJobId,
        long experimentId,
        String adAccountId,
        List<CreativePublicationPayload> creativePayloads
    ) {
        List<CreativePublicationPayload> resolvedPayloads = new ArrayList<>(creativePayloads.size());
        Map<String, String> localHashCache = new java.util.HashMap<>();
        for (CreativePublicationPayload payload : creativePayloads) {
            if (!StringUtils.hasText(payload.imageUrl())) {
                LOGGER.warn(
                    "Creative image preload skipped because image URL is empty: experimentId={}, creativeId={}",
                    experimentId,
                    payload.creative().id()
                );
                resolvedPayloads.add(payload.withImageHash(null));
                continue;
            }
            try {
                String uploadedImageHash = resolveOrUploadCanonicalImageHash(
                    publicationJobId,
                    experimentId,
                    adAccountId,
                    payload.creative().id(),
                    payload.imageUrl(),
                    localHashCache
                );
                LOGGER.info(
                    "Creative image preload succeeded and will use image_hash as primary path: experimentId={}, creativeId={}, hash={}",
                    experimentId,
                    payload.creative().id(),
                    uploadedImageHash
                );
                resolvedPayloads.add(payload.withImageHash(uploadedImageHash));
            } catch (Exception ex) {
                LOGGER.error(
                    "Creative image preload failed; campaign publication requires byte upload and will not fall back to picture URL: experimentId={}, creativeId={}, imageUrl={}, message={}",
                    experimentId,
                    payload.creative().id(),
                    payload.imageUrl(),
                    ex.getMessage(),
                    ex
                );
                throw ex;
            }
        }
        return resolvedPayloads;
    }

    /**
     * Reuses a cached canonical image_hash or uploads the downloaded image bytes to Meta.
     */
    private String resolveOrUploadCanonicalImageHash(
        String publicationJobId,
        long experimentId,
        String adAccountId,
        Long creativeId,
        String imageUrl,
        Map<String, String> localHashCache
    ) {
        LOGGER.info(
            "Preloading creative image using canonical hash deduplication: experimentId={}, creativeId={}, imageUrl={}",
            experimentId,
            creativeId,
            imageUrl
        );
        DownloadedImage downloadedImage;
        try {
            downloadedImage = downloadCreativeImage(imageUrl);
        } catch (Exception ex) {
            LOGGER.warn(
                "Could not download creative image for canonical byte upload; campaign publication will fail without URL fallback: experimentId={}, creativeId={}, imageUrl={}, message={}",
                experimentId,
                creativeId,
                imageUrl,
                ex.getMessage()
            );
            throw ex;
        }
        String localHash = computeSha256(downloadedImage.bytes());
        if (localHashCache.containsKey(localHash)) {
            String cachedHash = localHashCache.get(localHash);
            LOGGER.info(
                "Reusing image_hash from in-memory deduplication cache: experimentId={}, creativeId={}, localHash={}, hash={}",
                experimentId,
                creativeId,
                localHash,
                cachedHash
            );
            return cachedHash;
        }

        Optional<String> canonicalHash = lookupCanonicalImageHash(adAccountId, localHash);
        if (canonicalHash.isPresent()) {
            String existingHash = canonicalHash.get();
            localHashCache.put(localHash, existingHash);
            LOGGER.info(
                "Reusing canonical image_hash returned by backend repository: experimentId={}, creativeId={}, localHash={}, hash={}",
                experimentId,
                creativeId,
                localHash,
                existingHash
            );
            return existingHash;
        }

        String uploadedImageHash = executeFacebookCallWithLogging(
            publicationJobId,
            experimentId,
            ExperimentFacebookApiLogContext.CAMPAIGN_AD_CREATIVE,
            () -> uploadDownloadedAdImage(adAccountId, imageUrl, downloadedImage)
        );
        localHashCache.put(localHash, uploadedImageHash);
        LOGGER.info(
            "Creative image uploaded to Meta and image_hash captured for canonical persistence: experimentId={}, creativeId={}, localHash={}, hash={}",
            experimentId,
            creativeId,
            localHash,
            uploadedImageHash
        );
        persistCanonicalImageHash(adAccountId, localHash, uploadedImageHash);
        return uploadedImageHash;
    }

    private Optional<String> lookupCanonicalImageHash(String adAccountId, String localHash) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/facebook-campaigns/image-hash-mappings/resolve");
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("platform", IMAGE_HASH_PLATFORM)
            .queryParam("adAccountId", adAccountId)
            .queryParam("localHash", localHash)
            .build(true)
            .toUri();
        LOGGER.info(
            "Requesting canonical image hash mapping from backend: url==>{}, params={}",
            uri,
            JsonLogFormatter.wrap(objectMapper, Map.of("platform", IMAGE_HASH_PLATFORM, "adAccountId", adAccountId, "localHash", localHash))
        );
        try {
            CanonicalImageHashResponse response = backendClient.get()
                .uri(uri)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
                        return Mono.empty();
                    }
                    if (clientResponse.statusCode().isError()) {
                        return clientResponse.createException().flatMap(Mono::error);
                    }
                    return clientResponse.bodyToMono(CanonicalImageHashResponse.class);
                })
                .block();
            LOGGER.info(
                "Received canonical image hash mapping from backend: url<=={}, response={}",
                uri,
                JsonLogFormatter.wrap(objectMapper, response)
            );
            return Optional.ofNullable(response)
                .map(CanonicalImageHashResponse::metaImageHash)
                .filter(StringUtils::hasText);
        } catch (Exception ex) {
            LOGGER.warn("Failed to resolve canonical image hash mapping from backend: url==>{}, message={}", uri, ex.getMessage());
            return Optional.empty();
        }
    }

    private void persistCanonicalImageHash(String adAccountId, String localHash, String metaImageHash) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/facebook-campaigns/image-hash-mappings");
        CanonicalImageHashUpsertRequest payload = new CanonicalImageHashUpsertRequest(
            IMAGE_HASH_PLATFORM,
            adAccountId,
            localHash,
            metaImageHash
        );
        LOGGER.info(
            "Persisting canonical image hash mapping in backend: url==>{}, payload={}",
            url,
            JsonLogFormatter.wrap(objectMapper, payload)
        );
        try {
            CanonicalImageHashResponse response = backendClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(CanonicalImageHashResponse.class)
                .block();
            LOGGER.info(
                "Canonical image hash mapping persisted in backend: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, response)
            );
        } catch (Exception ex) {
            LOGGER.warn("Failed to persist canonical image hash mapping in backend: url==>{}, message={}", url, ex.getMessage());
        }
    }

    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute SHA-256 hash for creative image", ex);
        }
    }

    /**
     * Creates an ad creative and, for legacy URL requests, retries transient image download errors via byte upload.
     */
    private String createAdCreativeWithImageDownloadRetry(
        String adAccountId,
        String publicationJobId,
        long experimentId,
        FacebookAdsService.AdCreativeRequest request
    ) {
        int attempt = 1;
        boolean triedImageLibraryFallback = false;
        FacebookAdsService.AdCreativeRequest requestInUse = request;
        while (true) {
            try {
                FacebookAdsService.AdCreativeRequest requestSnapshot = requestInUse;
                return executeFacebookCallWithLogging(
                    publicationJobId,
                    experimentId,
                    ExperimentFacebookApiLogContext.CAMPAIGN_AD_CREATIVE,
                    () -> facebookAdsService.createAdCreative(adAccountId, requestSnapshot)
                );
            } catch (WebClientResponseException ex) {
                if (!isCreativeImageDownloadError(ex) || attempt >= AD_CREATIVE_IMAGE_DOWNLOAD_RETRY_MAX_ATTEMPTS) {
                    throw ex;
                }
                if (!triedImageLibraryFallback
                    && StringUtils.hasText(requestInUse.imageUrl())
                    && !StringUtils.hasText(requestInUse.imageHash())) {
                    String imageUrlSnapshot = requestInUse.imageUrl();
                    try {
                        String uploadedImageHash = executeFacebookCallWithLogging(
                            publicationJobId,
                            experimentId,
                            ExperimentFacebookApiLogContext.CAMPAIGN_AD_CREATIVE,
                            () -> {
                                DownloadedImage downloadedImage = downloadCreativeImage(imageUrlSnapshot);
                                return uploadDownloadedAdImage(adAccountId, imageUrlSnapshot, downloadedImage);
                            }
                        );
                        requestInUse = withImageHashOnly(requestInUse, uploadedImageHash);
                        triedImageLibraryFallback = true;
                        LOGGER.info(
                            "Uploaded creative image to Facebook ad library after download error and switched request to image_hash: experimentId={}, hash={}",
                            experimentId,
                            uploadedImageHash
                        );
                    } catch (Exception uploadEx) {
                        LOGGER.warn(
                            "Could not upload image to Facebook ad library after download error; keeping URL fallback: experimentId={}, message={}",
                            experimentId,
                            uploadEx.getMessage()
                        );
                    }
                }
                LOGGER.warn(
                    "Retrying ad creative creation after transient image download error: experimentId={}, attempt={}/{}, status={}, message={}",
                    experimentId,
                    attempt,
                    AD_CREATIVE_IMAGE_DOWNLOAD_RETRY_MAX_ATTEMPTS,
                    ex.getRawStatusCode(),
                    ex.getMessage()
                );
                attempt++;
            }
        }
    }

    private FacebookAdsService.AdCreativeRequest withImageHashOnly(
        FacebookAdsService.AdCreativeRequest request,
        String imageHash
    ) {
        return new FacebookAdsService.AdCreativeRequest(
            request.name(),
            request.pageId(),
            request.instagramActorId(),
            request.websiteUrl(),
            request.leadGenFormId(),
            request.message(),
            imageHash,
            null,
            request.callToActionType(),
            request.headline(),
            request.description()
        );
    }

    private boolean isCreativeImageDownloadError(WebClientResponseException ex) {
        if (ex == null || ex.getRawStatusCode() != 400) {
            return false;
        }
        JsonNode errorNode = parseJson(ex.getResponseBodyAsString());
        if (errorNode == null) {
            return false;
        }
        JsonNode details = errorNode.path("error");
        if (details.path("error_subcode").asInt() == AD_CREATIVE_IMAGE_DOWNLOAD_ERROR_SUBCODE) {
            return true;
        }
        String combinedMessage = (
            details.path("message").asText("")
                + " "
                + details.path("error_user_title").asText("")
                + " "
                + details.path("error_user_msg").asText("")
        ).toLowerCase();
        return combinedMessage.contains("imagem não foi baixada")
            || combinedMessage.contains("image was not downloaded")
            || combinedMessage.contains("unable to download")
            || combinedMessage.contains("não foi possível baixar sua imagem");
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

    private void markExperimentAsFailed(long experimentId, String reason) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/status?status=FAILED");
        LOGGER.info(
            "Marking experiment as FAILED in backend: url==>{}, params={}, reason={}",
            url,
            JsonLogFormatter.wrap(objectMapper, Collections.emptyMap()),
            reason
        );
        try {
            backendClient.patch()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info(
                "Marked experiment {} as FAILED after campaign processing error: {}",
                experimentId,
                reason
            );
        } catch (Exception ex) {
            LOGGER.warn(
                "Could not mark experiment {} as FAILED after campaign processing error: url==>{}, message={}",
                experimentId,
                url,
                ex.getMessage(),
                ex
            );
        }
    }

    private void markExperimentAsRunning(long experimentId, String campaignId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/status?status=RUNNING");
        LOGGER.info(
            "Marking experiment as RUNNING in backend after Facebook campaign publication: url==>{}, params={}, campaignId={}",
            url,
            JsonLogFormatter.wrap(objectMapper, Collections.emptyMap()),
            campaignId
        );
        try {
            backendClient.patch()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info(
                "Marked experiment {} as RUNNING after Facebook campaign publication: campaignId={}",
                experimentId,
                campaignId
            );
        } catch (Exception ex) {
            LOGGER.warn(
                "Could not mark experiment {} as RUNNING after Facebook campaign publication: url==>{}, message={}, campaignId={}",
                experimentId,
                url,
                ex.getMessage(),
                campaignId,
                ex
            );
        }
    }

    private InstantFormResolution ensureInstantFormDestination(Experiment experiment) {
        Experiment.InstantForm form = experiment.facebookInstantForm();
        if (form == null) {
            return null;
        }
        String facebookFormId = StringUtils.hasText(form.facebookFormId()) ? form.facebookFormId().trim() : null;
        String shareLink = form.shareLink();
        String normalizedFormId = InstantFormPublicationHelper.normalizeInstantFormId(LOGGER, facebookFormId, shareLink);
        if (form.published()) {
            if (!StringUtils.hasText(shareLink) && StringUtils.hasText(normalizedFormId)) {
                shareLink = InstantFormPublicationHelper.buildInstantFormShareLink(normalizedFormId);
            }
            if (!StringUtils.hasText(normalizedFormId)) {
                LOGGER.info(
                    "Instant form {} is published but Meta has not assigned the final lead form identifier yet; the CTA will use the share link only.",
                    form.id()
                );
            }
            return new InstantFormResolution(new InstantFormDestination(shareLink, normalizedFormId), null);
        }
        String publishIdentifier = StringUtils.hasText(normalizedFormId) ? normalizedFormId : facebookFormId;
        if (StringUtils.hasText(publishIdentifier)) {
            publishIdentifier = publishIdentifier.trim();
        }
        if (!StringUtils.hasText(publishIdentifier)) {
            LOGGER.warn(
                "Experiment {} references an instant form without a resolvable Facebook identifier; skipping publication",
                experiment.id()
            );
            return new InstantFormResolution(new InstantFormDestination(shareLink, normalizedFormId), null);
        }

        String status = form.status();
        try {
            LOGGER.info(
                "Publishing approved instant form before creating Facebook campaign: experimentId={}, formId={}",
                experiment.id(),
                publishIdentifier
            );
            facebookAdsService.publishInstantForm(publishIdentifier);
            JsonNode details = facebookAdsService.fetchInstantForm(publishIdentifier);
            if (details != null) {
                status = details.path("status").asText(status);
            }
            String resolvedFormId = InstantFormPublicationHelper.normalizeInstantFormId(
                LOGGER,
                details != null ? details.path("id").asText(publishIdentifier) : publishIdentifier,
                shareLink
            );
            if (StringUtils.hasText(resolvedFormId) && !resolvedFormId.equals(normalizedFormId)) {
                LOGGER.info(
                    "Instant form identifier returned by Facebook differs from backend metadata: previous={}, resolved={}",
                    normalizedFormId,
                    resolvedFormId
                );
                normalizedFormId = resolvedFormId;
            }
            if (!StringUtils.hasText(normalizedFormId)) {
                normalizedFormId = resolvedFormId;
            }
            if (!StringUtils.hasText(normalizedFormId)) {
                LOGGER.info(
                    "Meta did not return the final lead form identifier for instant form {}; the CTA will continue to rely on the share link until it is available.",
                    form.id()
                );
            }
            if (!StringUtils.hasText(shareLink) && details != null) {
                String shareLinkFromFacebook = details.path("share_link").asText(null);
                if (StringUtils.hasText(shareLinkFromFacebook)) {
                    shareLink = shareLinkFromFacebook.trim();
                }
            }
            if (StringUtils.hasText(normalizedFormId)) {
                shareLink = InstantFormPublicationHelper.buildInstantFormShareLink(normalizedFormId);
            }
        } catch (FacebookAccessTokenExpiredException ex) {
            handleAccessTokenExpiration("instant form publication", ex);
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
        InstantFormPublicationUpdateRequest publicationUpdate = new InstantFormPublicationUpdateRequest(
            true,
            Instant.now(),
            shareLink,
            status,
            normalizedFormId
        );
        return new InstantFormResolution(new InstantFormDestination(shareLink, normalizedFormId), publicationUpdate);
    }

    private void handleAccessTokenExpiration(String processLabel, FacebookAccessTokenExpiredException ex) {
        lastExpiredAccessToken.compareAndSet(null, facebookAdsService.getCurrentAccessToken());
        FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
            LOGGER.info(
                "Facebook access token renewed automatically after detecting expiration while {}.",
                processLabel
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
                "Facebook access token expired while {}; the worker will pause {} until renewal. message={}, details={}",
                processLabel,
                processLabel,
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
            JsonLogFormatter.wrap(objectMapper, request)
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

    private record AdCreativeCreation(String id, FacebookAdsService.AdCreativeRequest request) {}

    private record InstantFormResolution(InstantFormDestination destination, InstantFormPublicationUpdateRequest publicationUpdate) {}

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

    private String appendCampaignTrackingParameter(String baseUrl, Experiment experiment) {
        if (!StringUtils.hasText(baseUrl) || experiment == null) {
            return baseUrl;
        }
        try {
            return UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .replaceQueryParam("campaign")
                    .queryParam("campaign", "exp-" + experiment.id())
                    .build(true)
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Failed to append campaign parameter to lead portal URL {}: {}", baseUrl, ex.getMessage());
            return baseUrl;
        }
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
        if (StringUtils.hasText(experiment.followUpActionUrl())) {
            String destinationUrl = appendCampaignTrackingParameter(experiment.followUpActionUrl(), experiment);
            LOGGER.info(
                "Using experiment standalone follow-up URL as destination for experiment {}: url={}",
                experiment.id(),
                destinationUrl
            );
            return destinationUrl;
        }
        Experiment.LeadPortalFlow leadPortalFlow = experiment.leadPortalFlow();
        if (leadPortalFlow != null) {
            if (StringUtils.hasText(leadPortalFlow.publicUrl())) {
                String destinationUrl = appendCampaignTrackingParameter(leadPortalFlow.publicUrl(), experiment);
                LOGGER.info(
                    "Using lead portal flow public URL as destination for experiment {}: flowId={}, url={}",
                    experiment.id(),
                    leadPortalFlow.id(),
                    destinationUrl
                );
                return destinationUrl;
            } else {
                LOGGER.warn(
                    "Lead portal flow {} selected for experiment {} but it has no public URL; falling back to creative destination",
                    leadPortalFlow.id(),
                    experiment.id()
                );
            }
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

}
