package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalClient.FacebookTokenRenewalStatus;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class FacebookTokenRenewalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookTokenRenewalService.class);
    private static final long FALLBACK_LONG_LIVED_TOKEN_VALIDITY_SECONDS = 60L * 24L * 60L * 60L;

    private final WebClient backendClient;
    private final FacebookAdsService facebookAdsService;
    private final FacebookTokenRenewalClient tokenRenewalClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FacebookTokenRenewalService(
        WebClient.Builder builder,
        FacebookAdsService facebookAdsService,
        FacebookTokenRenewalClient tokenRenewalClient,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.facebookAdsService = facebookAdsService;
        this.tokenRenewalClient = tokenRenewalClient;
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public void renewTokensIfNeeded() {
        List<FacebookAccountRenewalCandidate> candidates = fetchEligibleAccounts();
        if (candidates.isEmpty()) {
            LOGGER.debug("No Facebook accounts require token renewal");
            return;
        }

        candidates.forEach(this::renewCandidateToken);
    }

    private List<FacebookAccountRenewalCandidate> fetchEligibleAccounts() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/accounts/facebook/renewal/eligible");
        LOGGER.info(
            "Requesting Facebook accounts eligible for token renewal from backend: url==>{}, params={}",
            url,
            Collections.emptyMap()
        );
        try {
            List<FacebookAccountRenewalCandidate> response = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(FacebookAccountRenewalCandidate.class)
                .collectList()
                .blockOptional()
                .orElse(Collections.emptyList());
            LOGGER.info(
                "Received eligible Facebook accounts response from backend: url<=={}, response={}",
                url,
                response
            );
            return response;
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to fetch Facebook accounts for token renewal: url<=={}, status={}, message={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
            return Collections.emptyList();
        }
    }

    private void renewCandidateToken(FacebookAccountRenewalCandidate candidate) {
        LOGGER.info(
            "Attempting token renewal for Facebook account id={}, name={}, expiresAt={}",
            candidate.id(),
            candidate.name(),
            candidate.tokenExpiresAt()
        );

        boolean matchesCurrentToken = Objects.equals(
            candidate.accessToken(),
            facebookAdsService.getCurrentAccessToken()
        );

        TokenRenewalAttemptResult attempt = renewTokenForAccount(
            candidate.id(),
            candidate.appId(),
            candidate.appSecret(),
            candidate.accessToken(),
            matchesCurrentToken
        );

        if (attempt.status() == FacebookTokenRenewalStatus.SUCCESS) {
            LOGGER.info(
                "Facebook token generation succeeded for account id={} via Graph API",
                candidate.id()
            );
        } else {
            LOGGER.error(
                "Token generation failed for Facebook account id={}, name={}: {}",
                candidate.id(),
                candidate.name(),
                attempt.errorMessage()
            );
        }
    }

    public TokenRenewalAttemptResult renewTokenForAccount(
        Long accountId,
        String appId,
        String appSecret,
        String currentToken,
        boolean updateInMemory
    ) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(appId, "appId");
        Objects.requireNonNull(appSecret, "appSecret");
        Objects.requireNonNull(currentToken, "currentToken");

        TokenRenewalAttemptResult attempt = performTokenRenewal(accountId, appId, appSecret, currentToken);

        if (attempt.status() == FacebookTokenRenewalStatus.SUCCESS) {
            if (updateInMemory && attempt.accessToken() != null) {
                facebookAdsService.updateAccessToken(attempt.accessToken());
                LOGGER.info(
                    "Updated in-memory Facebook access token after Graph API renewal for account id={}",
                    accountId
                );
            }
            tokenRenewalClient.reportSuccess(
                accountId,
                attempt.accessToken(),
                attempt.tokenExpiresAt(),
                attempt.renewedAt(),
                attempt.attemptedAt()
            );
        } else {
            tokenRenewalClient.reportFailure(accountId, attempt.attemptedAt(), attempt.errorMessage());
        }

        return attempt;
    }

    private TokenRenewalAttemptResult performTokenRenewal(
        Long accountId,
        String appId,
        String appSecret,
        String currentToken
    ) {
        LocalDateTime attemptedAt = LocalDateTime.now();
        try {
            FacebookAdsService.TokenRenewalResponse response = facebookAdsService.renewLongLivedToken(
                appId,
                appSecret,
                currentToken
            );
            LocalDateTime renewedAt = LocalDateTime.now();
            LocalDateTime expiresAt = calculateExpiration(renewedAt, response.expiresInSeconds());
            return new TokenRenewalAttemptResult(
                FacebookTokenRenewalStatus.SUCCESS,
                response.accessToken(),
                expiresAt,
                renewedAt,
                attemptedAt,
                null
            );
        } catch (Exception ex) {
            LOGGER.error(
                "Failed to renew Facebook token via Graph API for account id={}: {}",
                accountId,
                ex.getMessage(),
                ex
            );
            return new TokenRenewalAttemptResult(
                FacebookTokenRenewalStatus.FAILED,
                null,
                null,
                null,
                attemptedAt,
                ex.getMessage()
            );
        }
    }

    private LocalDateTime calculateExpiration(LocalDateTime renewedAt, long expiresInSeconds) {
        long duration = expiresInSeconds > 0 ? expiresInSeconds : FALLBACK_LONG_LIVED_TOKEN_VALIDITY_SECONDS;
        return renewedAt.plusSeconds(duration);
    }

    public record FacebookAccountRenewalCandidate(
        Long id,
        String name,
        String appId,
        String appSecret,
        String accessToken,
        LocalDateTime tokenExpiresAt
    ) {}

    public record TokenRenewalAttemptResult(
        FacebookTokenRenewalStatus status,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt,
        String errorMessage
    ) {}
}

