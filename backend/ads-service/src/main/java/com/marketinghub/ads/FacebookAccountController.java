package com.marketinghub.ads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts/facebook")
public class FacebookAccountController {
    private static final Logger log = LoggerFactory.getLogger(FacebookAccountController.class);
    private final FacebookAccountRepository repository;
    private final FacebookTokenRevalidationService tokenRevalidationService;

    public FacebookAccountController(
        FacebookAccountRepository repository,
        FacebookTokenRevalidationService tokenRevalidationService
    ) {
        this.repository = repository;
        this.tokenRevalidationService = tokenRevalidationService;
    }

    @GetMapping
    public List<FacebookAccount> findAll() {
        return repository.findAll();
    }

    @PostMapping
    public FacebookAccount create(@RequestBody FacebookAccount account) {
        normalizeAccount(account);
        if (account.getAccessToken() == null) {
            account.setTokenExpiresAt(null);
            account.setTokenLastRefreshedAt(null);
            account.setTokenRenewalStatus(FacebookTokenRenewalStatus.NEVER_ATTEMPTED.name());
            account.setTokenRenewalLastAttemptAt(null);
            account.setTokenRenewedAt(null);
            account.setTokenRenewalLastError(null);
        } else if (account.getTokenLastRefreshedAt() == null) {
            account.setTokenLastRefreshedAt(LocalDateTime.now());
        }
        if (account.getTokenRenewalStatus() == null || account.getTokenRenewalStatus().isBlank()) {
            account.setTokenRenewalStatus(FacebookTokenRenewalStatus.NEVER_ATTEMPTED.name());
        }
        if (!account.isTokenRenewalEnabled()) {
            account.setTokenRenewalEnabled(false);
        }
        FacebookAccount saved = repository.save(account);
        enforceSingleWorkerEnabled(saved);
        return saved;
    }

    @PutMapping("/{id}")
    public FacebookAccount update(@PathVariable Long id, @RequestBody FacebookAccount account) {
        FacebookAccount persisted = repository.findById(id).orElseThrow();
        normalizeAccount(account);

        persisted.setName(account.getName());
        persisted.setCurrency(account.getCurrency());
        persisted.setAuthorizedUserId(account.getAuthorizedUserId());
        persisted.setAuthorizedUserName(account.getAuthorizedUserName());
        persisted.setAuthorizedUserEmail(account.getAuthorizedUserEmail());
        persisted.setAppId(account.getAppId());
        persisted.setAdAccountId(account.getAdAccountId());
        persisted.setDefaultWebsiteUrl(account.getDefaultWebsiteUrl());
        persisted.setDefaultCreativeMessageTemplate(account.getDefaultCreativeMessageTemplate());
        persisted.setDefaultCallToActionType(account.getDefaultCallToActionType());
        persisted.setAdSetDailyBudget(account.getAdSetDailyBudget());
        persisted.setAdSetBillingEvent(account.getAdSetBillingEvent());
        persisted.setAdSetOptimizationGoal(account.getAdSetOptimizationGoal());
        persisted.setAdSetDestinationType(account.getAdSetDestinationType());
        persisted.setAdSetBidStrategy(account.getAdSetBidStrategy());
        persisted.setAdSetBidAmount(account.getAdSetBidAmount());
        persisted.setAdSetTargetCountry(account.getAdSetTargetCountry());
        persisted.setTokenRenewalEnabled(account.isTokenRenewalEnabled());
        persisted.setWorkerEnabled(account.isWorkerEnabled());

        if (account.isDefaultPageIdProvided()) {
            persisted.setDefaultPageId(account.getDefaultPageId());
        }
        if (account.isDefaultInstagramActorIdProvided()) {
            persisted.setDefaultInstagramActorId(account.getDefaultInstagramActorId());
        }
        if (account.isDefaultLeadGenFormIdProvided()) {
            persisted.setDefaultLeadGenFormId(account.getDefaultLeadGenFormId());
        }

        if (account.isAccessTokenProvided()) {
            String newToken = account.getAccessToken();
            if (newToken == null) {
                persisted.setAccessToken(null);
                persisted.setTokenExpiresAt(null);
                persisted.setTokenLastRefreshedAt(null);
                persisted.setTokenRenewalStatus(FacebookTokenRenewalStatus.NEVER_ATTEMPTED.name());
                persisted.setTokenRenewalLastAttemptAt(null);
                persisted.setTokenRenewedAt(null);
                persisted.setTokenRenewalLastError(null);
            } else {
                if (!newToken.equals(persisted.getAccessToken())) {
                    persisted.setTokenLastRefreshedAt(LocalDateTime.now());
                }
                persisted.setAccessToken(newToken);
                persisted.setTokenExpiresAt(account.getTokenExpiresAt());
            }
        }

        if (account.isAppSecretProvided()) {
            persisted.overwriteAppSecret(account.getAppSecret());
        }

        if (account.getTokenRenewalStatus() != null) {
            persisted.setTokenRenewalStatus(account.getTokenRenewalStatus());
        }
        
        FacebookAccount updated = repository.save(persisted);
        enforceSingleWorkerEnabled(updated);
        return updated;
    }

    @GetMapping("/worker-config")
    public FacebookWorkerConfiguration workerConfiguration() {
        FacebookAccount account = repository
            .findFirstByWorkerEnabledTrue()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facebook worker configuration not found"));
        validateWorkerConfiguration(account);
        return toWorkerConfiguration(account);
    }

    @GetMapping("/renewal/eligible")
    public List<FacebookAccountRenewalCandidate> findEligibleForRenewal() {
        return repository
            .findAll()
            .stream()
            .filter(FacebookAccount::isTokenRenewalEnabled)
            .filter(account -> account.getAccessToken() != null && !account.getAccessToken().isBlank())
            .filter(account -> account.getAppId() != null && !account.getAppId().isBlank())
            .filter(account -> account.getAppSecret() != null && !account.getAppSecret().isBlank())
            .filter(FacebookAccount::isTokenRenewalRequired)
            .map(account -> new FacebookAccountRenewalCandidate(
                account.getId(),
                account.getName(),
                account.getAppId(),
                account.getAppSecret(),
                account.getAccessToken(),
                account.getTokenExpiresAt(),
                account.getTokenRenewalStatus(),
                account.getTokenRenewalLastAttemptAt(),
                account.getTokenRenewalLastError()
            ))
            .collect(Collectors.toList());
    }

    @PostMapping("/{id}/token/renewal")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public FacebookAccount registerRenewal(
        @PathVariable Long id,
        @RequestBody FacebookTokenRenewalRequest request
    ) {
        FacebookAccount account = repository.findById(id).orElseThrow();
        LocalDateTime attemptedAt = request.attemptedAt() != null ? request.attemptedAt() : LocalDateTime.now();
        account.setTokenRenewalLastAttemptAt(attemptedAt);
        account.setTokenRenewalStatus(request.status().name());

        if (request.status() == FacebookTokenRenewalStatus.SUCCESS) {
            if (request.accessToken() == null || request.accessToken().isBlank()) {
                throw new IllegalArgumentException("Renewal requires a non-empty access token");
            }
            account.setAccessToken(request.accessToken());
            account.setTokenExpiresAt(request.tokenExpiresAt());
            LocalDateTime refreshedAt = request.renewedAt() != null ? request.renewedAt() : attemptedAt;
            account.setTokenLastRefreshedAt(refreshedAt);
            account.setTokenRenewedAt(refreshedAt);
            account.setTokenRenewalLastError(null);
        } else if (request.status() == FacebookTokenRenewalStatus.FAILED) {
            account.setTokenRenewalLastError(trimToNull(request.errorMessage()));
        }

        return repository.save(account);
    }

    @PostMapping("/{id}/token/revalidation")
    public FacebookTokenRevalidationResponse revalidateToken(@PathVariable Long id) {
        FacebookAccount account = repository.findById(id).orElseThrow();
        validateRevalidationPrerequisites(account);
        FacebookTokenRevalidationService.RevalidationResult result = tokenRevalidationService.revalidate(account);
        return new FacebookTokenRevalidationResponse(
            result.status(),
            result.accessToken(),
            result.tokenExpiresAt(),
            result.renewedAt(),
            result.attemptedAt(),
            result.errorMessage()
        );
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private void normalizeAccount(FacebookAccount account) {
        account.setName(trim(account.getName()));
        account.setCurrency(trim(account.getCurrency()));
        account.setAccessToken(trimToNull(account.getAccessToken()));
        account.setAuthorizedUserId(trimToNull(account.getAuthorizedUserId()));
        account.setAuthorizedUserName(trimToNull(account.getAuthorizedUserName()));
        account.setAuthorizedUserEmail(trimToNull(account.getAuthorizedUserEmail()));
        account.setAppId(trimToNull(account.getAppId()));
        account.setAdAccountId(trimToNull(account.getAdAccountId()));
        account.setDefaultPageId(trimToNull(account.getDefaultPageId()));
        account.setDefaultWebsiteUrl(trimToNull(account.getDefaultWebsiteUrl()));
        account.setDefaultLeadGenFormId(trimToNull(account.getDefaultLeadGenFormId()));
        account.setDefaultInstagramActorId(trimToNull(account.getDefaultInstagramActorId()));
        account.setDefaultCreativeMessageTemplate(trimToNull(account.getDefaultCreativeMessageTemplate()));
        account.setDefaultCallToActionType(trimToNull(account.getDefaultCallToActionType()));
        account.setAdSetDailyBudget(trimToNull(account.getAdSetDailyBudget()));
        account.setAdSetBillingEvent(trimToNull(account.getAdSetBillingEvent()));
        account.setAdSetOptimizationGoal(trimToNull(account.getAdSetOptimizationGoal()));
        account.setAdSetDestinationType(trimToNull(account.getAdSetDestinationType()));
        account.setAdSetBidStrategy(trimToNull(account.getAdSetBidStrategy()));
        account.setAdSetBidAmount(trimToNull(account.getAdSetBidAmount()));
        account.setAdSetTargetCountry(trimToNull(account.getAdSetTargetCountry()));
        if (account.isAppSecretProvided()) {
            account.overwriteAppSecret(trimToNull(account.getAppSecret()));
        }
        account.setTokenRenewalLastError(trimToNull(account.getTokenRenewalLastError()));
        if (account.getTokenRenewalStatus() != null) {
            String normalizedStatus = account.getTokenRenewalStatus().trim().toUpperCase();
            try {
                account.setTokenRenewalStatus(FacebookTokenRenewalStatus.valueOf(normalizedStatus).name());
            } catch (IllegalArgumentException ex) {
                account.setTokenRenewalStatus(FacebookTokenRenewalStatus.NEVER_ATTEMPTED.name());
            }
        }
    }

    private void enforceSingleWorkerEnabled(FacebookAccount saved) {
        if (!saved.isWorkerEnabled()) {
            return;
        }
        repository
            .findAll()
            .stream()
            .filter(other -> !other.getId().equals(saved.getId()))
            .filter(FacebookAccount::isWorkerEnabled)
            .forEach(other -> {
                other.setWorkerEnabled(false);
                repository.save(other);
            });
    }

    private FacebookWorkerConfiguration toWorkerConfiguration(FacebookAccount account) {
        String creativeMessageTemplate = StringUtils.hasText(account.getDefaultCreativeMessageTemplate())
            ? account.getDefaultCreativeMessageTemplate()
            : "%s";
        String callToAction = StringUtils.hasText(account.getDefaultCallToActionType())
            ? account.getDefaultCallToActionType()
            : "LEARN_MORE";
        return new FacebookWorkerConfiguration(
            account.getId(),
            account.getAdAccountId(),
            account.getAccessToken(),
            account.getAppId(),
            account.getAppSecret(),
            account.getDefaultPageId(),
            account.getDefaultInstagramActorId(),
            account.getDefaultWebsiteUrl(),
            account.getDefaultLeadGenFormId(),
            creativeMessageTemplate,
            callToAction,
            account.getAdSetDailyBudget(),
            account.getAdSetBillingEvent(),
            account.getAdSetOptimizationGoal(),
            account.getAdSetDestinationType(),
            account.getAdSetBidStrategy(),
            account.getAdSetBidAmount(),
            account.getAdSetTargetCountry()
        );
    }

    private void validateWorkerConfiguration(FacebookAccount account) {
        requireField(account, account.getAccessToken(), "access token", FacebookWorkerValidationError.ACCESS_TOKEN_MISSING);
        requireField(account, account.getAdAccountId(), "ad account id", FacebookWorkerValidationError.AD_ACCOUNT_ID_MISSING);
        if (!StringUtils.hasText(account.getDefaultWebsiteUrl()) && !StringUtils.hasText(account.getDefaultLeadGenFormId())) {
            log.warn(
                "Facebook worker configuration for account id={} name='{}' is missing both default website URL and default lead form ID; creatives without overrides will be skipped.",
                account.getId(),
                StringUtils.hasText(account.getName()) ? account.getName() : "(unnamed)"
            );
        }
        requireField(
            account,
            account.getAdSetDailyBudget(),
            "ad set daily budget",
            FacebookWorkerValidationError.AD_SET_DAILY_BUDGET_MISSING
        );
        requireField(
            account,
            account.getAdSetBillingEvent(),
            "ad set billing event",
            FacebookWorkerValidationError.AD_SET_BILLING_EVENT_MISSING
        );
        requireField(
            account,
            account.getAdSetOptimizationGoal(),
            "ad set optimization goal",
            FacebookWorkerValidationError.AD_SET_OPTIMIZATION_GOAL_MISSING
        );
        requireField(
            account,
            account.getAdSetDestinationType(),
            "ad set destination type",
            FacebookWorkerValidationError.AD_SET_DESTINATION_TYPE_MISSING
        );
        requireField(
            account,
            account.getAdSetTargetCountry(),
            "target country",
            FacebookWorkerValidationError.TARGET_COUNTRY_MISSING
        );
        recordWorkerValidationSuccess(account);
    }

    private void requireField(
        FacebookAccount account,
        String value,
        String fieldDescription,
        FacebookWorkerValidationError error
    ) {
        if (StringUtils.hasText(value)) {
            return;
        }
        log.warn(
            "Facebook worker configuration validation failed for account id={} name='{}': missing {} (code={}).",
            account.getId(),
            StringUtils.hasText(account.getName()) ? account.getName() : "(unnamed)",
            fieldDescription,
            error.code()
        );
        recordWorkerValidationFailure(account, error);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.apiMessage());
    }

    private void recordWorkerValidationFailure(FacebookAccount account, FacebookWorkerValidationError error) {
        LocalDateTime now = LocalDateTime.now();
        account.setWorkerLastValidationAt(now);
        account.setWorkerLastValidationErrorCode(error.code());
        account.setWorkerLastValidationErrorDetail(error.apiMessage());
        repository.save(account);
    }

    private void recordWorkerValidationSuccess(FacebookAccount account) {
        LocalDateTime now = LocalDateTime.now();
        account.setWorkerLastValidationAt(now);
        if (
            account.getWorkerLastValidationErrorCode() != null ||
            account.getWorkerLastValidationErrorDetail() != null
        ) {
            account.setWorkerLastValidationErrorCode(null);
            account.setWorkerLastValidationErrorDetail(null);
        }
        repository.save(account);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateRevalidationPrerequisites(FacebookAccount account) {
        if (!StringUtils.hasText(account.getAccessToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta sem token configurado para revalidação");
        }
        if (!StringUtils.hasText(account.getAppId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o App ID antes de revalidar o token");
        }
        if (!StringUtils.hasText(account.getAppSecret())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o App Secret antes de revalidar o token");
        }
    }

    public record FacebookAccountRenewalCandidate(
        Long id,
        String name,
        String appId,
        String appSecret,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        String tokenRenewalStatus,
        LocalDateTime tokenRenewalLastAttemptAt,
        String tokenRenewalLastError
    ) {}

    public record FacebookTokenRenewalRequest(
        FacebookTokenRenewalStatus status,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt,
        String errorMessage
    ) {}

    public record FacebookTokenRevalidationResponse(
        FacebookTokenRenewalStatus status,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt,
        String errorMessage
    ) {}

    public record FacebookWorkerConfiguration(
        Long accountId,
        String adAccountId,
        String accessToken,
        String appId,
        String appSecret,
        String defaultPageId,
        String defaultInstagramActorId,
        String defaultWebsiteUrl,
        String defaultLeadGenFormId,
        String defaultCreativeMessageTemplate,
        String defaultCallToActionType,
        String adSetDailyBudget,
        String adSetBillingEvent,
        String adSetOptimizationGoal,
        String adSetDestinationType,
        String adSetBidStrategy,
        String adSetBidAmount,
        String adSetTargetCountry
    ) {}
}
