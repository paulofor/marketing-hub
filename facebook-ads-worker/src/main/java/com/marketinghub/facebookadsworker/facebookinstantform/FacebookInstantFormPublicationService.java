package com.marketinghub.facebookadsworker.facebookinstantform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookAdsService.InstantFormCreationRequest;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
public class FacebookInstantFormPublicationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookInstantFormPublicationService.class);
    private static final Pattern INSTANT_FORM_OPTION_VALUE_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]+");

    private final FacebookAdsService facebookAdsService;
    private final FacebookAccessTokenManager accessTokenManager;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final MeterRegistry meterRegistry;
    private final boolean dryRun;
    private final AtomicBoolean accessTokenExpired;
    private final AtomicBoolean accessTokenExpiryWarningLogged;
    private final AtomicReference<String> lastExpiredAccessToken;
    private final AtomicBoolean configurationUnavailableWarningLogged;
    private final AtomicReference<String> cachedGlobalPrivacyPolicyUrl;
    private final AtomicBoolean privacyPolicyNotFoundLogged;
    private final ObjectMapper objectMapper;

    public FacebookInstantFormPublicationService(FacebookAdsService facebookAdsService,
                                                 FacebookAccessTokenManager accessTokenManager,
                                                 WebClient.Builder builder,
                                                 FacebookWorkerConfigurationClient configurationClient,
                                                 MeterRegistry meterRegistry,
                                                 @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                                 @Value("${backend.api-prefix:/api}") String apiPrefix,
                                                 @Value("${facebook.instant-forms.dry-run:false}") boolean dryRun,
                                                 ObjectMapper objectMapper) {
        this.facebookAdsService = facebookAdsService;
        this.accessTokenManager = accessTokenManager;
        this.configurationClient = configurationClient;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.meterRegistry = meterRegistry;
        this.dryRun = dryRun;
        this.accessTokenExpired = new AtomicBoolean(false);
        this.accessTokenExpiryWarningLogged = new AtomicBoolean(false);
        this.lastExpiredAccessToken = new AtomicReference<>(null);
        this.configurationUnavailableWarningLogged = new AtomicBoolean(false);
        this.cachedGlobalPrivacyPolicyUrl = new AtomicReference<>(null);
        this.privacyPolicyNotFoundLogged = new AtomicBoolean(false);
        this.objectMapper = objectMapper;
    }

    public void processApprovedInstantFormDrafts() {
        if (accessTokenExpired.get()) {
            if (hasTokenChangedSinceExpiration()) {
                LOGGER.info(
                    "Detected refreshed Facebook access token after a previous expiration; resuming instant form creation."
                );
                accessTokenExpired.set(false);
                accessTokenExpiryWarningLogged.set(false);
                lastExpiredAccessToken.set(null);
            } else {
                FacebookAccessTokenManager.RenewalAttemptResult renewalResult =
                    accessTokenManager.tryRenewAccessTokenIfPossible();
                if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
                    LOGGER.info(
                        "Facebook access token renewed automatically after a previous expiration; resuming instant form creation."
                    );
                    accessTokenExpired.set(false);
                    accessTokenExpiryWarningLogged.set(false);
                    lastExpiredAccessToken.set(null);
                } else {
                    if (accessTokenExpiryWarningLogged.compareAndSet(false, true)) {
                        LOGGER.warn(
                            "Skipping instant form creation because the configured Facebook access token has expired; renew the token and restart the worker."
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
                LOGGER.warn("Facebook worker configuration is unavailable; skipping instant form creation");
            }
            return;
        }
        configurationUnavailableWarningLogged.set(false);

        FacebookWorkerConfiguration config = configuration.get();
        String configuredToken = config.accessToken();
        if (!StringUtils.hasText(configuredToken)) {
            LOGGER.error("Facebook worker configuration is missing an access token; skipping instant form creation");
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

        List<InstantForm> forms = fetchApprovedInstantFormDrafts();
        if (forms == null || forms.isEmpty()) {
            meterRegistry.counter("facebook.instant_form.creation.fetched", "outcome", "empty").increment();
            return;
        }

        meterRegistry.counter("facebook.instant_form.creation.fetched", "outcome", "available")
            .increment(forms.size());

        String globalPrivacyPolicyUrl = resolveGlobalPrivacyPolicyUrl();

        forms.forEach(form -> processInstantFormDraft(form, globalPrivacyPolicyUrl));
    }

    private List<InstantForm> fetchApprovedInstantFormDrafts() {
        Optional<List<InstantForm>> approvedDrafts = fetchInstantFormsFromBackend(
            "/instant-forms/approved-drafts",
            false
        );
        if (approvedDrafts.isPresent()) {
            return approvedDrafts.get();
        }
        return fetchInstantFormsFromBackend("/instant-forms/ready-to-publish", true).orElse(Collections.emptyList());
    }

    private Optional<List<InstantForm>> fetchInstantFormsFromBackend(String path, boolean treatNotFoundAsEmpty) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, path);
        LOGGER.info(
            "Requesting instant forms from backend: url==>{}, params={}",
            url,
            Collections.emptyMap()
        );
        try {
            List<InstantForm> forms = backendClient.get()
                .uri(url)
                .exchangeToFlux(response -> {
                    if (response.statusCode().isError()) {
                        return response.createException().flatMapMany(Mono::error);
                    }
                    return response.bodyToFlux(InstantForm.class);
                })
                .collectList()
                .block();
            LOGGER.info(
                "Received instant form response from backend: url<=={}, response={}",
                url,
                forms
            );
            if (forms == null) {
                return Optional.of(Collections.emptyList());
            }
            return Optional.of(forms);
        } catch (WebClientResponseException.NotFound ex) {
            if (treatNotFoundAsEmpty) {
                LOGGER.info("Backend responded with 404 for instant form request: url<=={}", url);
                return Optional.of(Collections.emptyList());
            }
            LOGGER.info(
                "Instant form endpoint not available; falling back to legacy path: url<=={}, status={}",
                url,
                ex.getStatusCode()
            );
            return Optional.empty();
        } catch (WebClientRequestException ex) {
            LOGGER.warn(
                "Failed to fetch instant forms from backend: url==>{}",
                url,
                ex
            );
            return Optional.of(Collections.emptyList());
        } catch (Exception ex) {
            LOGGER.error(
                "Unexpected error while fetching instant forms from backend: url==>{}, message={}",
                url,
                ex.getMessage(),
                ex
            );
            return Optional.of(Collections.emptyList());
        }
    }

    private String resolveGlobalPrivacyPolicyUrl() {
        String fetched = fetchGlobalPrivacyPolicyUrlFromBackend();
        if (StringUtils.hasText(fetched)) {
            cachedGlobalPrivacyPolicyUrl.set(fetched);
            return fetched;
        }
        String cached = cachedGlobalPrivacyPolicyUrl.get();
        if (StringUtils.hasText(cached)) {
            LOGGER.info("Using cached global privacy policy URL for instant form creation: url={}", cached);
        }
        return cached;
    }

    private String fetchGlobalPrivacyPolicyUrlFromBackend() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/settings/privacy_policy_url");
        LOGGER.info("Requesting global privacy policy URL from backend: url==>{}", url);
        try {
            GeneralSetting setting = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GeneralSetting.class)
                .block();
            LOGGER.info("Received global privacy policy response from backend: url<=={}, response={}", url, setting);
            if (setting != null && StringUtils.hasText(setting.value())) {
                privacyPolicyNotFoundLogged.set(false);
                return setting.value().trim();
            }
        } catch (WebClientResponseException.NotFound ex) {
            if (privacyPolicyNotFoundLogged.compareAndSet(false, true)) {
                LOGGER.warn("Global privacy policy URL not configured in backend; proceeding without a default value");
            }
            LOGGER.info("Backend responded with 404 when fetching global privacy policy URL: url<=={}", url);
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to fetch global privacy policy URL from backend: url==>{}, message={}",
                url,
                ex.getMessage(),
                ex
            );
        }
        return null;
    }

    private void processInstantFormDraft(InstantForm form, String globalPrivacyPolicyUrl) {
        if (form == null) {
            return;
        }
        meterRegistry.counter("facebook.instant_form.creation.processed", "stage", "attempt").increment();

        String existingIdentifier = normalizeExternalIdentifier(form.facebookFormId(), form.externalId());
        if (StringUtils.hasText(existingIdentifier)) {
            LOGGER.info(
                "Skipping instant form {} because the backend already stores an external identifier: facebookFormId={}",
                form.id(),
                existingIdentifier
            );
            meterRegistry.counter("facebook.instant_form.creation.skipped", "reason", "already_created").increment();
            return;
        }
        if (form.id() == null) {
            LOGGER.warn("Skipping instant form creation because the backend did not provide a valid identifier");
            meterRegistry.counter("facebook.instant_form.creation.skipped", "reason", "missing_backend_id").increment();
            return;
        }

        InstantFormDetails details = fetchInstantFormDetails(form.id());
        if (details == null) {
            LOGGER.warn(
                "Skipping instant form {} because backend details could not be retrieved",
                form.id()
            );
            meterRegistry.counter("facebook.instant_form.creation.skipped", "reason", "missing_details").increment();
            return;
        }

        String persistedIdentifier = normalizeExternalIdentifier(details.facebookFormId(), details.externalId());
        if (StringUtils.hasText(persistedIdentifier)) {
            LOGGER.info(
                "Skipping instant form {} because backend details already include an external identifier: {}",
                form.id(),
                persistedIdentifier
            );
            meterRegistry.counter("facebook.instant_form.creation.skipped", "reason", "already_persisted").increment();
            return;
        }

        String pageExternalId = normalizeFacebookPageExternalId(form.facebookPageExternalId(), details.facebookPageExternalId());
        if (!StringUtils.hasText(pageExternalId)) {
            LOGGER.warn(
                "Skipping instant form {} because the Facebook page external identifier is missing",
                form.id()
            );
            meterRegistry.counter("facebook.instant_form.creation.skipped", "reason", "missing_page").increment();
            return;
        }

        InstantFormCreationRequest creationRequest = buildInstantFormCreationRequest(form, details, globalPrivacyPolicyUrl);
        if (creationRequest == null) {
            meterRegistry.counter("facebook.instant_form.creation.skipped", "reason", "invalid_payload").increment();
            return;
        }

        if (dryRun || Boolean.TRUE.equals(details.dryRun())) {
            LOGGER.info(
                "Dry-run enabled; skipping Meta creation for instant form {} on page {}",
                form.id(),
                pageExternalId
            );
            meterRegistry.counter("facebook.instant_form.creation.dry_run", "page_id", pageExternalId).increment();
            return;
        }

        boolean attemptedCreation = false;
        long startNanos = 0L;
        String outcome = "success";
        String errorTag = "none";

        try {
            attemptedCreation = true;
            startNanos = System.nanoTime();
            LOGGER.info(
                "Creating Facebook instant form: id={}, pageExternalId={}, name={}",
                form.id(),
                pageExternalId,
                creationRequest.name()
            );
            String createdId = facebookAdsService.createInstantForm(pageExternalId, creationRequest);
            if (!StringUtils.hasText(createdId)) {
                outcome = "error";
                errorTag = "missing_id";
                LOGGER.warn(
                    "Facebook did not return an identifier when creating instant form {} on page {}; skipping persistence",
                    form.id(),
                    pageExternalId
                );
                meterRegistry.counter("facebook.instant_form.creation.error", "status", "MISSING_ID").increment();
                return;
            }
            String normalizedCreatedId = createdId.trim();
            LOGGER.info(
                "Facebook instant form created: id={}, facebookFormId={}, pageExternalId={}",
                form.id(),
                normalizedCreatedId,
                pageExternalId
            );
            meterRegistry.counter("facebook.instant_form.creation.success", "page_id", pageExternalId).increment();
            reportInstantFormCreation(
                form.id(),
                normalizedCreatedId,
                StringUtils.hasText(details.status()) ? details.status() : "CREATED"
            );
        } catch (FacebookAccessTokenExpiredException ex) {
            outcome = "error";
            errorTag = "token_expired";
            meterRegistry.counter("facebook.instant_form.creation.error", "status", "TOKEN_EXPIRED").increment();
            handleAccessTokenExpiration("creating instant forms", ex);
        } catch (FacebookPermissionException ex) {
            outcome = "error";
            errorTag = "permission";
            meterRegistry.counter("facebook.instant_form.creation.error", "status", "PERMISSION").increment();
            LOGGER.error(
                "Facebook permission error while creating instant form {}: message={}, details={}",
                form.id(),
                ex.getMessage(),
                ex.getErrorDetails(),
                ex
            );
        } catch (WebClientResponseException ex) {
            outcome = "error";
            errorTag = String.valueOf(ex.getRawStatusCode());
            meterRegistry.counter(
                "facebook.instant_form.creation.error",
                "status",
                String.valueOf(ex.getRawStatusCode())
            ).increment();
            LOGGER.error(
                "Facebook API error while creating instant form {}: status={}, responseBody={}",
                form.id(),
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                ex
            );
        } catch (Exception ex) {
            outcome = "error";
            errorTag = "unexpected";
            meterRegistry.counter("facebook.instant_form.creation.error", "status", "UNEXPECTED").increment();
            LOGGER.error(
                "Unexpected error while creating instant form {}: message={}",
                form.id(),
                ex.getMessage(),
                ex
            );
        } finally {
            if (attemptedCreation) {
                long duration = System.nanoTime() - startNanos;
                meterRegistry.timer(
                        "facebook.instant_form.creation.duration",
                        "outcome",
                        outcome,
                        "error",
                        errorTag,
                        "page_id",
                        pageExternalId
                    )
                    .record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        }
    }

    private String normalizeExternalIdentifier(String... identifiers) {
        if (identifiers == null) {
            return null;
        }
        for (String identifier : identifiers) {
            if (StringUtils.hasText(identifier)) {
                String trimmed = identifier.trim();
                if (StringUtils.hasText(trimmed)) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private InstantFormCreationRequest buildInstantFormCreationRequest(
        InstantForm form,
        InstantFormDetails details,
        String globalPrivacyPolicyUrl
    ) {
        String name = StringUtils.hasText(details.name()) ? details.name().trim() : form.name();
        if (!StringUtils.hasText(name)) {
            LOGGER.warn(
                "Instant form {} is missing a name; skipping Meta creation",
                form != null ? form.id() : null
            );
            return null;
        }
        String locale = normalizeLocale(details.locale());
        InstantFormCreationRequest.PrivacyPolicy privacyPolicy = resolvePrivacyPolicy(details, globalPrivacyPolicyUrl);
        if (privacyPolicy == null || !StringUtils.hasText(privacyPolicy.url())) {
            LOGGER.warn(
                "Instant form {} does not define a privacy policy URL; skipping Meta creation",
                form != null ? form.id() : null
            );
            return null;
        }
        String followUpActionUrl = resolveFollowUpActionUrl(form, details);
        if (!StringUtils.hasText(followUpActionUrl)) {
            LOGGER.warn(
                "Instant form {} does not define a follow-up action URL; skipping Meta creation",
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
        List<Question> questions = deserializeQuestions(details.questions());
        if (questions != null) {
            for (Question question : questions) {
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

    private List<Question> deserializeQuestions(String rawQuestions) {
        if (!StringUtils.hasText(rawQuestions)) {
            return null;
        }
        try {
            return objectMapper.readValue(rawQuestions, new TypeReference<List<Question>>() {});
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to deserialize instant form questions from backend payload: message={}",
                ex.getMessage(),
                ex
            );
            return null;
        }
    }

    private InstantFormCreationRequest.Question mapQuestion(Question question) {
        if (question == null || !StringUtils.hasText(question.type())) {
            return null;
        }
        String type = question.type().trim();
        if ("LEGAL".equalsIgnoreCase(type)) {
            String questionLabel = StringUtils.hasText(question.label()) ? question.label().trim() : null;
            if (StringUtils.hasText(questionLabel)) {
                LOGGER.warn(
                    "Skipping instant form question '{}' because the Graph API does not accept LEGAL question types.",
                    questionLabel
                );
            } else {
                LOGGER.warn("Skipping LEGAL instant form question because the Graph API does not accept this type.");
            }
            return null;
        }
        String key = StringUtils.hasText(question.key()) ? question.key().trim() : null;
        String label = StringUtils.hasText(question.label()) ? question.label().trim() : null;
        List<Map<String, Object>> options = null;
        if (question.options() != null && !question.options().isEmpty()) {
            options = new ArrayList<>(question.options().size());
            for (QuestionOption option : question.options()) {
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
                String sanitizedOptionValue = sanitizeInstantFormOptionValue(optionValue, optionLabel);
                if (StringUtils.hasText(sanitizedOptionValue)) {
                    mappedOption.put("value", sanitizedOptionValue);
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

    private String sanitizeInstantFormOptionValue(String rawValue, String optionLabel) {
        String primary = normalizeInstantFormOptionValue(rawValue);
        if (StringUtils.hasText(primary)) {
            return primary;
        }
        String fallback = normalizeInstantFormOptionValue(optionLabel);
        if (StringUtils.hasText(fallback)) {
            if (StringUtils.hasText(rawValue)) {
                LOGGER.debug(
                    "Instant form option value '{}' contained unsupported characters; generated fallback '{}' from label '{}'.",
                    rawValue.trim(),
                    fallback,
                    optionLabel
                );
            }
            return fallback;
        }
        if (StringUtils.hasText(rawValue)) {
            LOGGER.warn(
                "Skipping instant form option value '{}' because it cannot be normalized to the allowed pattern.",
                rawValue.trim()
            );
        }
        return null;
    }

    private String normalizeInstantFormOptionValue(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String trimmed = source.trim();
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9 _-]", "");
        normalized = normalized.replaceAll("\\s+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^[_-]+", "");
        normalized = normalized.replaceAll("[_-]+$", "");
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!INSTANT_FORM_OPTION_VALUE_PATTERN.matcher(normalized).matches()) {
            normalized = normalized.replaceAll("[^A-Za-z0-9_-]", "");
        }
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        return normalized;
    }

    private InstantFormCreationRequest.PrivacyPolicy resolvePrivacyPolicy(
        InstantFormDetails details,
        String globalPrivacyPolicyUrl
    ) {
        String linkText = "Política de Privacidade";
        if (details.privacyPolicy() != null && StringUtils.hasText(details.privacyPolicy().linkText())) {
            linkText = details.privacyPolicy().linkText().trim();
        }

        String url = null;
        if (details.privacyPolicy() != null && StringUtils.hasText(details.privacyPolicy().url())) {
            url = details.privacyPolicy().url().trim();
        } else if (StringUtils.hasText(details.privacyPolicyUrl())) {
            url = details.privacyPolicyUrl().trim();
        } else if (StringUtils.hasText(details.experimentPrivacyPolicyUrl())) {
            url = details.experimentPrivacyPolicyUrl().trim();
        } else if (StringUtils.hasText(globalPrivacyPolicyUrl)) {
            url = globalPrivacyPolicyUrl.trim();
        }

        if (!StringUtils.hasText(url)) {
            return null;
        }
        return new InstantFormCreationRequest.PrivacyPolicy(url, linkText);
    }

    private String resolveFollowUpActionUrl(InstantForm form, InstantFormDetails details) {
        if (details.followUpAction() != null && StringUtils.hasText(details.followUpAction().url())) {
            return details.followUpAction().url().trim();
        }
        if (StringUtils.hasText(details.followUpActionUrl())) {
            return details.followUpActionUrl().trim();
        }
        if (StringUtils.hasText(details.experimentFollowUpUrl())) {
            return details.experimentFollowUpUrl().trim();
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
            "Reporting instant form status to backend: url==>{}, payload={}",
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
            LOGGER.info("Successfully reported instant form status to backend: url<=={}", url);
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to report instant form status to backend: url==>{}, message={}",
                url,
                ex.getMessage(),
                ex
            );
        }
    }

    private void reportInstantFormCreation(long formId, String facebookFormId, String status) {
        LOGGER.info(
            "Reporting instant form creation to backend: id={}, facebookFormId={}, status={}",
            formId,
            facebookFormId,
            status
        );
        InstantFormPublicationUpdateRequest request = new InstantFormPublicationUpdateRequest(
            false,
            null,
            null,
            StringUtils.hasText(status) ? status : "CREATED",
            facebookFormId
        );
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
                "Facebook access token expired while {}; the worker will pause instant form creation until renewal. message={}, details={}",
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
        String externalId,
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
        String externalId,
        String name,
        String status,
        String locale,
        String followUpActionUrl,
        String followUpActionText,
        FollowUpAction followUpAction,
        String privacyPolicyUrl,
        PrivacyPolicy privacyPolicy,
        String questions,
        String experimentFollowUpUrl,
        String experimentPrivacyPolicyUrl,
        Boolean dryRun
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeneralSetting(String name, String value) {
    }
}
