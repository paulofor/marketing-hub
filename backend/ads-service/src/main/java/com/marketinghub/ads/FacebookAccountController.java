package com.marketinghub.ads;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts/facebook")
public class FacebookAccountController {
    private final FacebookAccountRepository repository;

    public FacebookAccountController(FacebookAccountRepository repository) {
        this.repository = repository;
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
        return repository.save(account);
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
        persisted.setTokenRenewalEnabled(account.isTokenRenewalEnabled());

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

        if (account.isAppSecretProvided()) {
            persisted.overwriteAppSecret(account.getAppSecret());
        }

        if (account.getTokenRenewalStatus() != null) {
            persisted.setTokenRenewalStatus(account.getTokenRenewalStatus());
        }
        
        return repository.save(persisted);
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
}
