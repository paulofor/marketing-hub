package com.marketinghub.facebookadsworker.facebookinstantform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookAdsService.InstantFormCreationRequest;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            facebookFormId = tryCreateInstantFormDraft(form);
            if (!StringUtils.hasText(facebookFormId)) {
                try {
                    facebookFormId = resolveFacebookFormIdFromFacebook(form);
                } catch (FacebookAccessTokenExpiredException ex) {
                    handleAccessTokenExpiration("publishing instant forms", ex);
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
            handleAccessTokenExpiration("publishing instant forms", ex);
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

    private String tryCreateInstantFormDraft(InstantForm form) {
        if (form == null || form.id() == null) {
            return null;
        }
        InstantFormDetails details = fetchInstantFormDetails(form.id());
        if (details == null) {
            LOGGER.warn(
                "Skipping creation of Facebook instant form draft for {} because backend details could not be retrieved",
                form.id()
            );
            return null;
        }
        if (StringUtils.hasText(details.facebookFormId())) {
            return normalizeFacebookFormId(details.facebookFormId());
        }
        String pageExternalId = normalizeFacebookPageExternalId(form.facebookPageExternalId(), details.facebookPageExternalId());
        if (!StringUtils.hasText(pageExternalId)) {
            LOGGER.warn(
                "Skipping creation of Facebook instant form draft for {} because the Facebook page external identifier is missing",
                form.id()
            );
            return null;
        }
        InstantFormCreationRequest creationRequest = buildInstantFormCreationRequest(form, details);
        if (creationRequest == null) {
            return null;
        }
        try {
            LOGGER.info(
                "Creating Facebook instant form draft: id={}, pageExternalId={}, name={}",
                form.id(),
                pageExternalId,
                creationRequest.name()
            );
            String createdId = facebookAdsService.createInstantForm(pageExternalId, creationRequest);
            if (!StringUtils.hasText(createdId)) {
                LOGGER.warn(
                    "Facebook did not return an identifier when creating instant form {} on page {}; skipping draft persistence",
                    form.id(),
                    pageExternalId
                );
                return null;
            }
            String normalizedCreatedId = createdId.trim();
            LOGGER.info(
                "Facebook instant form draft created: id={}, facebookFormId={}, pageExternalId={}",
                form.id(),
                normalizedCreatedId,
                pageExternalId
            );
            reportInstantFormDraftIdentifier(
                form.id(),
                normalizedCreatedId,
                StringUtils.hasText(details.status()) ? details.status() : "DRAFT"
            );
            return normalizedCreatedId;
        } catch (FacebookAccessTokenExpiredException ex) {
            handleAccessTokenExpiration("creating instant forms", ex);
            return null;
        } catch (FacebookPermissionException ex) {
            LOGGER.error(
                "Facebook permission error while creating instant form {}: message={}, details={}",
                form.id(),
                ex.getMessage(),
                ex.getErrorDetails(),
                ex
            );
            return null;
        } catch (Exception ex) {
            LOGGER.error(
                "Unexpected error while creating instant form {}: message={}",
                form.id(),
                ex.getMessage(),
                ex
            );
            return null;
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

    private InstantFormCreationRequest buildInstantFormCreationRequest(InstantForm form, InstantFormDetails details) {
        String name = StringUtils.hasText(details.name()) ? details.name().trim() : form.name();
        if (!StringUtils.hasText(name)) {
            LOGGER.warn("Instant form {} is missing a name; skipping Facebook draft creation", form != null ? form.id() : null);
            return null;
        }
        String locale = normalizeLocale(details.locale());
        InstantFormCreationRequest.PrivacyPolicy privacyPolicy = resolvePrivacyPolicy(details);
        if (privacyPolicy == null || !StringUtils.hasText(privacyPolicy.url())) {
            LOGGER.warn(
                "Instant form {} does not define a privacy policy URL; skipping Facebook draft creation",
                form != null ? form.id() : null
            );
            return null;
        }
        String followUpActionUrl = resolveFollowUpActionUrl(form, details);
        if (!StringUtils.hasText(followUpActionUrl)) {
            LOGGER.warn(
                "Instant form {} does not define a follow-up action URL; skipping Facebook draft creation",
                form != null ? form.id() : null
            );
            return null;
        }
        String followUpActionText = resolveFollowUpActionText(details);
        List<InstantFormCreationRequest.Question> questions = buildQuestions(details);

        return new InstantFormCreationRequest(
            name.trim(),
            locale,
            privacyPolicy,
            questions,
            followUpActionText,
            followUpActionUrl
        );
    }

    private List<InstantFormCreationRequest.Question> buildQuestions(InstantFormDetails details) {
        List<InstantFormCreationRequest.Question> resolved = new ArrayList<>();
        if (details.questions() != null) {
            for (InstantFormDetails.Question question : details.questions()) {
                InstantFormCreationRequest.Question mapped = mapQuestion(question);
                if (mapped != null) {
                    resolved.add(mapped);
                }
            }
        }
        if (resolved.isEmpty()) {
            resolved.add(new InstantFormCreationRequest.Question("FULL_NAME"));
            resolved.add(new InstantFormCreationRequest.Question("EMAIL"));
        }
        return resolved;
    }

    private InstantFormCreationRequest.Question mapQuestion(InstantFormDetails.Question question) {
        if (question == null || !StringUtils.hasText(question.type())) {
            return null;
        }
        String type = question.type().trim();
        String key = StringUtils.hasText(question.key()) ? question.key().trim() : null;
        String label = StringUtils.hasText(question.label()) ? question.label().trim() : null;
        List<Map<String, Object>> options = null;
        if (question.options() != null && !question.options().isEmpty()) {
            options = new ArrayList<>(question.options().size());
            for (InstantFormDetails.QuestionOption option : question.options()) {
                if (option == null) {
                    continue;
                }
                String optionLabel = StringUtils.hasText(option.label()) ? option.label().trim() : null;
                String optionValue = StringUtils.hasText(option.value()) ? option.value().trim() : optionLabel;
                if (!StringUtils.hasText(optionLabel) && !StringUtils.hasText(optionValue)) {
                    continue;
                }
                Map<String, Object> mappedOption = new LinkedHashMap<>();
                if (StringUtils.hasText(optionLabel)) {
                    mappedOption.put("label", optionLabel);
                }
                if (StringUtils.hasText(optionValue)) {
                    mappedOption.put("value", optionValue);
                }
                if (!mappedOption.isEmpty()) {
                    options.add(mappedOption);
                }
            }
            if (options.isEmpty()) {
                options = null;
            }
        }
        String helperText = StringUtils.hasText(question.helperText()) ? question.helperText().trim() : null;
        Boolean required = question.required();
        Boolean allowMultiSelect = question.allowMultiSelect();
        return new InstantFormCreationRequest.Question(type, key, label, options, helperText, required, allowMultiSelect);
    }

    private InstantFormCreationRequest.PrivacyPolicy resolvePrivacyPolicy(InstantFormDetails details) {
        if (details.privacyPolicy() != null && StringUtils.hasText(details.privacyPolicy().url())) {
            String url = details.privacyPolicy().url().trim();
            String linkText = StringUtils.hasText(details.privacyPolicy().linkText())
                ? details.privacyPolicy().linkText().trim()
                : "Política de Privacidade";
            return new InstantFormCreationRequest.PrivacyPolicy(url, linkText);
        }
        if (StringUtils.hasText(details.privacyPolicyUrl())) {
            return new InstantFormCreationRequest.PrivacyPolicy(details.privacyPolicyUrl().trim(), "Política de Privacidade");
        }
        return null;
    }

    private String resolveFollowUpActionUrl(InstantForm form, InstantFormDetails details) {
        if (details.followUpAction() != null && StringUtils.hasText(details.followUpAction().url())) {
            return details.followUpAction().url().trim();
        }
        if (StringUtils.hasText(details.followUpActionUrl())) {
            return details.followUpActionUrl().trim();
        }
        if (form != null && StringUtils.hasText(form.shareLink())) {
            return form.shareLink().trim();
        }
        return null;
    }

    private String resolveFollowUpActionText(InstantFormDetails details) {
        if (details.followUpAction() != null && StringUtils.hasText(details.followUpAction().text())) {
            return details.followUpAction().text().trim();
        }
        if (StringUtils.hasText(details.followUpActionText())) {
            return details.followUpActionText().trim();
        }
        return "Visitar site";
    }

    private String normalizeFacebookPageExternalId(String fromForm, String fromDetails) {
        String candidate = StringUtils.hasText(fromForm) ? fromForm.trim() : null;
        if (!StringUtils.hasText(candidate) && StringUtils.hasText(fromDetails)) {
            candidate = fromDetails.trim();
        }
        return StringUtils.hasText(candidate) ? candidate : null;
    }

    private InstantFormDetails fetchInstantFormDetails(long formId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/instant-forms/" + formId);
        LOGGER.info("Requesting instant form details from backend: url==>{}", url);
        try {
            InstantFormDetails details = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(InstantFormDetails.class)
                .block();
            LOGGER.info("Received instant form details from backend: url<=={}, details={}", url, details);
            return details;
        } catch (WebClientResponseException.NotFound ex) {
            LOGGER.warn("Instant form {} was not found in backend while fetching details for creation", formId);
            return null;
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to fetch instant form {} details from backend: url==>{}, message={}",
                formId,
                url,
                ex.getMessage(),
                ex
            );
            return null;
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

    private void reportInstantFormDraftIdentifier(long formId, String facebookFormId, String status) {
        LOGGER.info(
            "Reporting instant form draft identifier to backend: id={}, facebookFormId={}, status={}",
            formId,
            facebookFormId,
            status
        );
        InstantFormPublicationUpdateRequest request = new InstantFormPublicationUpdateRequest(false, null, null, status, facebookFormId);
        reportInstantFormPublication(formId, request);
    }

    private void handleAccessTokenExpiration(String context, FacebookAccessTokenExpiredException ex) {
        lastExpiredAccessToken.compareAndSet(null, facebookAdsService.getCurrentAccessToken());
        FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
            LOGGER.info("Facebook access token renewed automatically after detecting expiration while {}.", context);
            accessTokenExpired.set(false);
            accessTokenExpiryWarningLogged.set(false);
            lastExpiredAccessToken.set(null);
            return;
        }
        boolean firstDetection = accessTokenExpired.compareAndSet(false, true);
        accessTokenExpiryWarningLogged.set(false);
        if (firstDetection) {
            LOGGER.error(
                "Facebook access token expired while {}; the worker will pause publication until renewal. message={}, details={}",
                context,
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

    private String normalizeLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            return "pt_BR";
        }
        String trimmed = locale.trim();
        if (trimmed.length() == 5 && trimmed.charAt(2) == '-') {
            return trimmed.substring(0, 2).toLowerCase(Locale.ROOT) + "_" + trimmed.substring(3).toUpperCase(Locale.ROOT);
        }
        if (trimmed.length() == 5 && trimmed.charAt(2) == '_') {
            return trimmed.substring(0, 2).toLowerCase(Locale.ROOT) + "_" + trimmed.substring(3).toUpperCase(Locale.ROOT);
        }
        return trimmed;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InstantFormDetails(
        Long id,
        String facebookPageExternalId,
        String facebookFormId,
        String name,
        String status,
        String locale,
        String followUpActionUrl,
        String followUpActionText,
        FollowUpAction followUpAction,
        String privacyPolicyUrl,
        PrivacyPolicy privacyPolicy,
        List<Question> questions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FollowUpAction(String url, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PrivacyPolicy(String url, String linkText) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Question(
        String type,
        String key,
        String label,
        String helperText,
        Boolean required,
        Boolean allowMultiSelect,
        List<QuestionOption> options
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuestionOption(String label, String value) {
    }
}
