package com.marketinghub.facebookadsworker.facebookinstantform;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.util.InstantFormPublicationHelper;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FacebookInstantFormPublicationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookInstantFormPublicationService.class);

    private final FacebookAdsService facebookAdsService;
    private final FacebookAccessTokenManager accessTokenManager;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final AtomicBoolean accessTokenExpired;
    private final AtomicBoolean accessTokenExpiryWarningLogged;
    private final AtomicReference<String> lastExpiredAccessToken;
    private final AtomicBoolean configurationUnavailableWarningLogged;

    public FacebookInstantFormPublicationService(FacebookAdsService facebookAdsService,
                                                 FacebookAccessTokenManager accessTokenManager,
                                                 WebClient.Builder builder,
                                                 FacebookWorkerConfigurationClient configurationClient,
                                                 @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                                 @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.facebookAdsService = facebookAdsService;
        this.accessTokenManager = accessTokenManager;
        this.configurationClient = configurationClient;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.accessTokenExpired = new AtomicBoolean(false);
        this.accessTokenExpiryWarningLogged = new AtomicBoolean(false);
        this.lastExpiredAccessToken = new AtomicReference<>(null);
        this.configurationUnavailableWarningLogged = new AtomicBoolean(false);
    }

    public void publishApprovedInstantForms() {
        if (accessTokenExpired.get()) {
            if (hasTokenChangedSinceExpiration()) {
                LOGGER.info(
                    "Detected refreshed Facebook access token after a previous expiration; resuming instant form publication."
                );
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
                lastExpiredAccessToken.set(null);
            } else {
                FacebookAccessTokenManager.RenewalAttemptResult renewalResult =
                    accessTokenManager.tryRenewAccessTokenIfPossible();
                if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
                    LOGGER.info(
                        "Facebook access token renewed automatically after a previous expiration; resuming instant form publication."
                    );
                    accessTokenExpired.set(false);
                    accessTokenExpiryWarningLogged.set(false);
                    lastExpiredAccessToken.set(null);
                } else {
                    if (accessTokenExpiryWarningLogged.compareAndSet(false, true)) {
                        LOGGER.warn(
                            "Skipping instant form publication because the configured Facebook access token has expired; renew the token and restart the worker."
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
                LOGGER.warn("Facebook worker configuration is unavailable; skipping instant form publication");
            }
            return;
        }
        configurationUnavailableWarningLogged.set(false);

        FacebookWorkerConfiguration config = configuration.get();
        String configuredToken = config.accessToken();
        if (!StringUtils.hasText(configuredToken)) {
            LOGGER.error("Facebook worker configuration is missing an access token; skipping instant form publication");
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

        List<InstantForm> forms = fetchInstantFormsReadyForPublication();
        if (forms == null || forms.isEmpty()) {
            return;
        }

        forms.forEach(this::processInstantForm);
    }

    private List<InstantForm> fetchInstantFormsReadyForPublication() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/instant-forms/ready-to-publish");
        LOGGER.info(
            "Requesting instant forms ready for publication from backend: url==>{}, params={}",
            url,
            Collections.emptyMap()
        );
        try {
            List<InstantForm> forms = backendClient.get()
                .uri(url)
                .exchangeToFlux(response -> {
                    if (response.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }
                    if (response.statusCode().isError()) {
                        return response.createException().flatMapMany(Mono::error);
                    }
                    return response.bodyToFlux(InstantForm.class);
                })
                .collectList()
                .block();
            LOGGER.info(
                "Received instant form publication response from backend: url<=={}, response={}",
                url,
                forms
            );
            return forms;
        } catch (WebClientRequestException ex) {
            LOGGER.warn(
                "Failed to fetch instant forms ready for publication from backend: url==>{}",
                url,
                ex
            );
            return Collections.emptyList();
        } catch (Exception ex) {
            LOGGER.error(
                "Unexpected error while fetching instant forms ready for publication: url==>{}, message={}",
                url,
                ex.getMessage(),
                ex
            );
            return Collections.emptyList();
        }
    }

    private void processInstantForm(InstantForm form) {
        if (form == null) {
            return;
        }
        String facebookFormId = normalizeFacebookFormId(form.facebookFormId());
        if (!StringUtils.hasText(facebookFormId)) {
            try {
                facebookFormId = resolveFacebookFormIdFromFacebook(form);
            } catch (FacebookAccessTokenExpiredException ex) {
                handleAccessTokenExpirationDuringPublication(ex);
                return;
            } catch (FacebookPermissionException ex) {
                LOGGER.error(
                    "Facebook permission error while resolving instant form {} identifier: message={}, details={}",
                    form.id(),
                    ex.getMessage(),
                    ex.getErrorDetails(),
                    ex
                );
                return;
            } catch (Exception ex) {
                LOGGER.warn(
                    "Failed to resolve Facebook form ID for instant form {}: message={}",
                    form.id(),
                    ex.getMessage(),
                    ex
                );
                return;
            }
        }
        if (!StringUtils.hasText(facebookFormId)) {
            LOGGER.warn(
                "Skipping instant form {} because the Facebook form ID is missing and could not be resolved; persist the draft identifier returned when the form was created (Meta provides the final ID only after publication)",
                form.id()
            );
            return;
        }
        try {
            LOGGER.info(
                "Publishing approved instant form: id={}, facebookFormId={}",
                form.id(),
                facebookFormId
            );
            facebookAdsService.publishInstantForm(facebookFormId);
            JsonNode details = facebookAdsService.fetchInstantForm(facebookFormId);
            String status = details != null ? details.path("status").asText(null) : null;
            if (!StringUtils.hasText(status)) {
                status = form.status();
            }
            String resolvedFormId = InstantFormPublicationHelper.normalizeInstantFormId(
                LOGGER,
                details != null ? details.path("id").asText(facebookFormId) : facebookFormId,
                form.shareLink()
            );
            String shareLink = resolveShareLink(details, form.shareLink(), resolvedFormId);
            InstantFormPublicationUpdateRequest request = new InstantFormPublicationUpdateRequest(
                true,
                Instant.now(),
                shareLink,
                status,
                resolvedFormId
            );
            reportInstantFormPublication(form.id(), request);
        } catch (FacebookAccessTokenExpiredException ex) {
            handleAccessTokenExpirationDuringPublication(ex);
        } catch (FacebookPermissionException ex) {
            LOGGER.error(
                "Facebook permission error while publishing instant form {}: message={}, details={}",
                form.id(),
                ex.getMessage(),
                ex.getErrorDetails(),
                ex
            );
        } catch (Exception ex) {
            LOGGER.error(
                "Unexpected error while publishing instant form {}: message={}",
                form.id(),
                ex.getMessage(),
                ex
            );
        }
    }

    private String resolveFacebookFormIdFromFacebook(InstantForm form) {
        String pageExternalId = StringUtils.hasText(form.facebookPageExternalId()) ? form.facebookPageExternalId().trim() : null;
        if (!StringUtils.hasText(pageExternalId)) {
            LOGGER.warn(
                "Instant form {} is missing the Facebook form ID and the backend did not provide the page external identifier; skipping resolution attempt",
                form != null ? form.id() : null
            );
            return null;
        }
        String formName = StringUtils.hasText(form.name()) ? form.name().trim() : null;
        if (!StringUtils.hasText(formName)) {
            LOGGER.warn(
                "Instant form {} is missing the Facebook form ID and does not have a name to match against drafts on page {}; skipping resolution attempt",
                form.id(),
                pageExternalId
            );
            return null;
        }
        LOGGER.info(
            "Attempting to resolve missing Facebook form ID for instant form {} by querying page {} with name {}",
            form.id(),
            pageExternalId,
            formName
        );
        String resolvedIdentifier = facebookAdsService.findInstantFormIdentifier(pageExternalId, formName);
        if (StringUtils.hasText(resolvedIdentifier)) {
            LOGGER.info(
                "Resolved missing Facebook form ID for instant form {}: facebookFormId={}, pageId={}",
                form.id(),
                resolvedIdentifier,
                pageExternalId
            );
            return resolvedIdentifier.trim();
        }
        LOGGER.warn(
            "Could not resolve Facebook form ID for instant form {} using page {} and name {}; ensure the draft identifier returned by Meta is persisted",
            form.id(),
            pageExternalId,
            formName
        );
        return null;
    }

    private String normalizeFacebookFormId(String facebookFormId) {
        if (!StringUtils.hasText(facebookFormId)) {
            return null;
        }
        String trimmed = facebookFormId.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private String resolveShareLink(JsonNode details, String existingShareLink, String normalizedFormId) {
        String shareLink = StringUtils.hasText(existingShareLink) ? existingShareLink.trim() : null;
        if (details != null && !StringUtils.hasText(shareLink)) {
            String fromDetails = details.path("share_link").asText(null);
            if (StringUtils.hasText(fromDetails)) {
                shareLink = fromDetails.trim();
            }
        }
        if (StringUtils.hasText(normalizedFormId)) {
            shareLink = InstantFormPublicationHelper.buildInstantFormShareLink(normalizedFormId);
        }
        return StringUtils.hasText(shareLink) ? shareLink.trim() : null;
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

    private boolean hasTokenChangedSinceExpiration() {
        String expiredToken = lastExpiredAccessToken.get();
        if (expiredToken == null) {
            return false;
        }
        String currentToken = facebookAdsService.getCurrentAccessToken();
        return currentToken != null && !currentToken.equals(expiredToken);
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

    private record InstantForm(
        Long id,
        String facebookFormId,
        String name,
        String status,
        Long facebookPageId,
        String facebookPageExternalId,
        String facebookPageName,
        boolean approved,
        boolean published,
        String shareLink
    ) {
    }
}
