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
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidateStatus;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidateType;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
    private final ObjectMapper objectMapper;
    private final ExperimentFacebookApiLogClient experimentFacebookApiLogClient;

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
            processExperiment(exp, config);
        });
    }

    private void processExperiment(Experiment exp, FacebookWorkerConfiguration config) {
        String campaignId = null;
        String adSetId = null;
        String adId = null;
        boolean publishReported = false;
        try {
            Creative creative = resolveCreative(exp.id());
            if (creative == null) {
                LOGGER.warn("Skipping experiment {} because no creative is available or could be fetched", exp.id());
                return;
            }

            Experiment.InstagramAccount instagramAccount = exp.instagramAccount();

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

            String resolvedWebsiteUrl = resolveDestinationUrl(exp, creative, config, instantFormDestination);
            String resolvedLeadGenFormId = resolveLeadGenFormId(exp, creative, config, instantFormDestination);
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

            String resolvedMessage = StringUtils.hasText(creative.primaryText())
                ? creative.primaryText()
                : formatCreativeMessage(exp.name(), config);
            String resolvedCallToAction = coalesce(creative.cta(), config.defaultCallToActionType());
            String resolvedInstagramActorId = coalesce(
                creative.instagramUserId(),
                instagramAccount != null ? instagramAccount.code() : null,
                config.defaultInstagramActorId()
            );
            if (!StringUtils.hasText(resolvedInstagramActorId)) {
                LOGGER.warn(
                    "Experiment {} does not have an Instagram actor ID; proceeding without instagram_user_id",
                    exp.id()
                );
            }
            String resolvedDestinationType = hasLeadFormDestination ? "ON_AD" : config.adSetDestinationType();
            String resolvedOptimizationGoal = hasLeadFormDestination
                ? "LEAD_GENERATION"
                : config.adSetOptimizationGoal();
            String resolvedCampaignObjective = hasLeadFormDestination ? "OUTCOME_LEADS" : "OUTCOME_TRAFFIC";

            List<ExperimentAdSet> experimentAdSets = Collections.emptyList();
            ExperimentAdSet selectedAdSet = null;
            AdSetPlaybookSpec selectedSpec = null;
            ResolvedTargeting resolvedTargeting = new ResolvedTargeting(null, Collections.emptyList());
            String adSetName = exp.name() + " - Ad Set";
            if (!exp.nextStepInstantForm()) {
                List<AdSetPlaybookSpec> readySpecs = fetchReadyPlaybookSpecs(exp.id());
                selectedSpec = selectPrimarySpec(readySpecs);
                experimentAdSets = fetchExperimentAdSets(exp.id());
                selectedAdSet = selectExperimentAdSet(experimentAdSets);
                if (selectedSpec != null) {
                    resolvedTargeting = new ResolvedTargeting(
                        normalizeTargetingSpec(selectedSpec.targetingSpec()),
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
                    resolvedTargeting = resolveTargetingFromBackend(selectedAdSet);
                }
            }
            try {
                campaignId = executeFacebookCallWithLogging(
                    exp.id(),
                    ExperimentFacebookApiLogContext.CAMPAIGN_CREATION,
                    () -> facebookAdsService.createCampaign(
                        config.adAccountId(),
                        exp.name(),
                        resolvedCampaignObjective
                    )
                );
            } catch (FacebookAccessTokenExpiredException ex) {
                if (!exp.nextStepInstantForm()) {
                    fetchExperimentAdSets(exp.id());
                }
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
                exp.id(),
                ExperimentFacebookApiLogContext.CAMPAIGN_AD_SET,
                () -> facebookAdsService.createAdSet(config.adAccountId(), adSetRequest)
            );
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
            adId = executeFacebookCallWithLogging(
                exp.id(),
                ExperimentFacebookApiLogContext.CAMPAIGN_AD,
                () -> facebookAdsService.createAd(config.adAccountId(), adRequest)
            );
            CreateCampaignRequest req = new CreateCampaignRequest(
                campaignId,
                config.adAccountId(),
                exp.name(),
                resolvedCampaignObjective,
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
                    adSetRequest.pageId(),
                    resolvedTargeting.targetingJson(),
                    selectedAdSet != null ? selectedAdSet.id() : null
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
                cleanupFailedPublication(campaignId, adSetId, adId);
            }
        }
    }

    private void cleanupFailedPublication(String campaignId, String adSetId, String adId) {
        if (!StringUtils.hasText(campaignId)) {
            return;
        }
        if (StringUtils.hasText(adId)) {
            try {
                facebookAdsService.deleteAd(adId);
            } catch (Exception cleanupEx) {
                LOGGER.warn("Failed to delete Facebook ad {} during cleanup: {}", adId, cleanupEx.getMessage());
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

    private List<ExperimentAdSet> fetchExperimentAdSets(long experimentId) {
        String url = UriComponentsBuilder
            .fromHttpUrl(UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/adsets"))
            .queryParam("experimentId", experimentId)
            .toUriString();
        LOGGER.info(
            "Requesting ad sets for experiment from backend: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(objectMapper, Map.of("experimentId", experimentId))
        );
        try {
            List<ExperimentAdSet> adSets = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(ExperimentAdSet.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received ad sets response from backend: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, adSets)
            );
            return adSets != null ? adSets : Collections.emptyList();
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to fetch ad sets for experiment {} from backend: url==>{}, message={}",
                experimentId,
                url,
                ex.getMessage()
            );
            LOGGER.debug("Stacktrace while fetching ad sets for experiment {}", experimentId, ex);
            return Collections.emptyList();
        }
    }

    private ExperimentAdSet selectExperimentAdSet(List<ExperimentAdSet> adSets) {
        if (adSets == null || adSets.isEmpty()) {
            return null;
        }
        Optional<ExperimentAdSet> withTargetingRequest = adSets.stream()
            .filter(adSet -> adSet.targetingRequestId() != null)
            .findFirst();
        if (withTargetingRequest.isPresent()) {
            return withTargetingRequest.get();
        }
        return adSets.stream()
            .filter(adSet -> StringUtils.hasText(adSet.targetingJson()))
            .findFirst()
            .orElse(adSets.get(0));
    }

    private ResolvedTargeting resolveTargetingFromBackend(ExperimentAdSet adSet) {
        if (adSet == null) {
            return new ResolvedTargeting(null, Collections.emptyList());
        }

        if (adSet.targetingRequestId() != null) {
            List<FacebookAdsService.TargetingOption> options = fetchValidatedTargetingOptions(adSet.targetingRequestId());
            if (options.isEmpty()) {
                throw new IllegalStateException(
                    "No validated targeting options found for request %s".formatted(adSet.targetingRequestId())
                );
            }

            ObjectNode targeting = objectMapper.createObjectNode();
            appendOptionsToTargeting(targeting, options);
            return new ResolvedTargeting(targeting.isEmpty() ? null : targeting.toString(), options);
        }

        if (StringUtils.hasText(adSet.targetingJson())) {
            LOGGER.warn(
                "Experiment ad set {} does not provide targetingRequestId. Falling back to legacy targetingJson payload.",
                adSet.id()
            );
            return new ResolvedTargeting(adSet.targetingJson(), Collections.emptyList());
        }
        LOGGER.warn(
            "Experiment ad set {} does not provide targetingRequestId. Falling back to legacy line-based targeting fields.",
            adSet.id()
        );
        ObjectNode targeting = objectMapper.createObjectNode();
        mergeTargetingValues(targeting, "interests", adSet.interests());
        mergeTargetingValues(targeting, "work_positions", adSet.jobTitles());
        mergeTargetingValues(targeting, "behaviors", adSet.behaviors());
        return new ResolvedTargeting(targeting.isEmpty() ? null : targeting.toString(), Collections.emptyList());
    }

    private void mergeTargetingValues(ObjectNode targeting, String fieldName, String rawValues) {
        if (targeting == null || !StringUtils.hasText(rawValues)) {
            return;
        }
        List<String> values = splitLines(rawValues);
        if (values.isEmpty()) {
            return;
        }
        ArrayNode array = targeting.putArray(fieldName);
        values.forEach(value -> {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("name", value);
            array.add(item);
        });
    }

    private void appendOptionsToTargeting(ObjectNode targeting, List<FacebookAdsService.TargetingOption> options) {
        if (targeting == null || options == null || options.isEmpty()) {
            return;
        }
        Map<TargetingCandidateType, ArrayNode> grouped = new EnumMap<>(TargetingCandidateType.class);
        Map<TargetingCandidateType, Set<String>> seen = new EnumMap<>(TargetingCandidateType.class);
        for (FacebookAdsService.TargetingOption option : options) {
            if (option == null || !StringUtils.hasText(option.facebookId())) {
                continue;
            }
            TargetingCandidateType type = option.type();
            if (type == null) {
                continue;
            }
            String fieldName = mapFieldName(type);
            if (fieldName == null) {
                continue;
            }
            Set<String> dedup = seen.computeIfAbsent(type, key -> new HashSet<>());
            String facebookId = option.facebookId().trim();
            if (!dedup.add(facebookId)) {
                continue;
            }
            ArrayNode array = grouped.computeIfAbsent(type, key -> targeting.putArray(fieldName));
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", facebookId);
            if (StringUtils.hasText(option.name())) {
                node.put("name", option.name());
            }
            array.add(node);
        }
    }

    private String mapFieldName(TargetingCandidateType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case BEHAVIOR -> "behaviors";
            case WORK_POSITION -> "work_positions";
            case INTEREST -> "interests";
        };
    }

    private List<String> splitLines(String rawValues) {
        if (!StringUtils.hasText(rawValues)) {
            return List.of();
        }
        String[] parts = rawValues.split("\\r?\\n");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                unique.add(trimmed);
            }
        }
        return new ArrayList<>(unique);
    }


    private List<FacebookAdsService.TargetingOption> fetchValidatedTargetingOptions(UUID targetingRequestId) {
        String url = UriComponentsBuilder
            .fromHttpUrl(UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/targeting/requests/" + targetingRequestId))
            .queryParam("includeCandidates", true)
            .toUriString();
        LOGGER.info(
            "Requesting targeting request from backend: url==>{}, requestId={}",
            url,
            targetingRequestId
        );
        try {
            TargetingRequestDetails response = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(TargetingRequestDetails.class)
                .block();
            List<FacebookAdsService.TargetingOption> options = extractValidatedOptions(response);
            LOGGER.info(
                "Received {} targeting options from backend for request {}",
                options.size(),
                targetingRequestId
            );
            return options;
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to fetch targeting request {} from backend: url==>{}, message={}",
                targetingRequestId,
                url,
                ex.getMessage()
            );
            LOGGER.debug("Stacktrace while fetching targeting request {}", targetingRequestId, ex);
            return Collections.emptyList();
        }
    }

    private List<FacebookAdsService.TargetingOption> extractValidatedOptions(TargetingRequestDetails request) {
        if (request == null || request.candidates() == null) {
            return Collections.emptyList();
        }
        List<FacebookAdsService.TargetingOption> options = new ArrayList<>();
        for (TargetingCandidateDetails candidate : request.candidates()) {
            if (candidate == null || candidate.status() != TargetingCandidateStatus.VALIDATED) {
                continue;
            }
            TargetingCandidateType type = candidate.tipo();
            if (candidate.options() == null) {
                continue;
            }
            for (TargetingOptionDetails option : candidate.options()) {
                if (option == null || !StringUtils.hasText(option.facebookId())) {
                    continue;
                }
                TargetingCandidateType resolvedType = option.type() != null ? option.type() : type;
                if (resolvedType == null) {
                    continue;
                }
                options.add(new FacebookAdsService.TargetingOption(
                    option.facebookId().trim(),
                    option.name(),
                    resolvedType,
                    option.audienceSize()
                ));
            }
        }
        return options;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExperimentAdSet(
        Long id,
        Long experimentId,
        String location,
        String interests,
        String jobTitles,
        String behaviors,
        String targetingJson,
        UUID targetingRequestId,
        String prompt,
        String model
    ) {}

    public record ResolvedTargeting(String targetingJson, List<FacebookAdsService.TargetingOption> options) {}

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

    public record Experiment(
        long id,
        String name,
        String pageId,
        BigDecimal dailyBudget,
        @JsonAlias({ "page", "associatedFacebookPage", "facebookPageAssociation" })
        FacebookPage facebookPage,
        InstagramAccount instagramAccount,
        @JsonAlias("facebookInstantForm")
        InstantForm facebookInstantForm,
        @JsonAlias("nextStepInstantForm")
        boolean nextStepInstantForm,
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetingRequestDetails(List<TargetingCandidateDetails> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetingCandidateDetails(
        Long id,
        TargetingCandidateType tipo,
        TargetingCandidateStatus status,
        List<TargetingOptionDetails> options
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetingOptionDetails(
        @JsonProperty("facebook_id") String facebookId,
        String name,
        TargetingCandidateType type,
        @JsonProperty("audience_size") Long audienceSize
    ) {}

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

    private Creative resolveCreative(long experimentId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/creatives");
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

    private String normalizeTargetingSpec(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            LOGGER.warn("Failed to normalize targeting spec JSON: {}", ex.getMessage());
            LOGGER.debug("Stacktrace while normalizing targeting spec JSON", ex);
            return raw;
        }
    }

    private <T> T executeFacebookCallWithLogging(Long experimentId,
                                                 ExperimentFacebookApiLogContext context,
                                                 Supplier<T> action) {
        facebookAdsService.clearLastApiCallDebugInfo();
        try {
            return action.get();
        } finally {
            experimentFacebookApiLogClient.logCall(
                experimentId,
                context,
                facebookAdsService.consumeLastApiCallDebugInfo()
            );
        }
    }

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
            String creativeId = executeFacebookCallWithLogging(
                experiment.id(),
                ExperimentFacebookApiLogContext.CAMPAIGN_AD_CREATIVE,
                () -> facebookAdsService.createAdCreative(adAccountId, primaryRequest)
            );
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

            String creativeId = executeFacebookCallWithLogging(
                experiment.id(),
                ExperimentFacebookApiLogContext.CAMPAIGN_AD_CREATIVE,
                () -> facebookAdsService.createAdCreative(adAccountId, fallbackRequest)
            );
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
        InstantFormPublicationUpdateRequest publicationUpdate = new InstantFormPublicationUpdateRequest(
            true,
            Instant.now(),
            shareLink,
            status,
            normalizedFormId
        );
        return new InstantFormResolution(new InstantFormDestination(shareLink, normalizedFormId), publicationUpdate);
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
        Experiment.LeadPortalFlow leadPortalFlow = experiment.leadPortalFlow();
        if (leadPortalFlow != null) {
            if (StringUtils.hasText(leadPortalFlow.publicUrl())) {
                LOGGER.info(
                    "Using lead portal flow public URL as destination for experiment {}: flowId={}, url={}",
                    experiment.id(),
                    leadPortalFlow.id(),
                    leadPortalFlow.publicUrl()
                );
                return leadPortalFlow.publicUrl();
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
