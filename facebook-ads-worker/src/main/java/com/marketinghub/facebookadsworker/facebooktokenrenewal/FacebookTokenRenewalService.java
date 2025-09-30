package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import com.marketinghub.facebookadsworker.FacebookAdsService;
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

    private final WebClient backendClient;
    private final FacebookAdsService facebookAdsService;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FacebookTokenRenewalService(
        WebClient.Builder builder,
        FacebookAdsService facebookAdsService,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.facebookAdsService = facebookAdsService;
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
            "Requesting Facebook accounts eligible for token renewal from backend: url={}, params={}",
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
                "Received eligible Facebook accounts response from backend: url={}, response={}",
                url,
                response
            );
            return response;
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to fetch Facebook accounts for token renewal: url={}, status={}, message={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
            return Collections.emptyList();
        }
    }

    private void renewCandidateToken(FacebookAccountRenewalCandidate candidate) {
        LocalDateTime attemptTime = LocalDateTime.now();
        LOGGER.info(
            "Attempting token renewal for Facebook account id={}, name={}, expiresAt={}",
            candidate.id(),
            candidate.name(),
            candidate.tokenExpiresAt()
        );

        try {
            FacebookAdsService.TokenRenewalResponse response = facebookAdsService.renewLongLivedToken(
                candidate.appId(),
                candidate.appSecret(),
                candidate.accessToken()
            );

            LocalDateTime renewedAt = attemptTime;
            LocalDateTime expiresAt = attemptTime.plusSeconds(Math.max(response.expiresInSeconds(), 0));

            boolean matchesCurrentToken = Objects.equals(
                candidate.accessToken(),
                facebookAdsService.getCurrentAccessToken()
            );
            if (matchesCurrentToken) {
                facebookAdsService.updateAccessToken(response.accessToken());
                LOGGER.info(
                    "Updated in-memory Facebook access token after backend renewal for account id={}",
                    candidate.id()
                );
            } else {
                LOGGER.debug(
                    "Skipping in-memory Facebook token update because candidate token does not match the worker configuration: accountId={}",
                    candidate.id()
                );
            }

            reportResult(candidate.id(), new RenewalResultPayload(
                TokenRenewalStatus.SUCCESS,
                response.accessToken(),
                expiresAt,
                renewedAt,
                attemptTime,
                null
            ));
        } catch (Exception ex) {
            LOGGER.error(
                "Token renewal failed for Facebook account id={}, name={}: {}",
                candidate.id(),
                candidate.name(),
                ex.getMessage(),
                ex
            );
            reportResult(candidate.id(), new RenewalResultPayload(
                TokenRenewalStatus.FAILED,
                null,
                null,
                null,
                attemptTime,
                ex.getMessage()
            ));
        }
    }

    private void reportResult(Long accountId, RenewalResultPayload payload) {
        String path = "/accounts/facebook/" + accountId + "/token/renewal";
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, path);
        LOGGER.info(
            "Reporting Facebook token renewal result to backend: url={}, params={}, payload={}",
            url,
            Collections.emptyMap(),
            payload
        );
        try {
            backendClient
                .post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info(
                "Successfully reported Facebook token renewal result to backend: url={}, accountId={}",
                url,
                accountId
            );
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to report token renewal result to backend for account {}: url={}, status={}, message={}",
                accountId,
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
        }
    }

    public enum TokenRenewalStatus {
        SUCCESS,
        FAILED
    }

    public record FacebookAccountRenewalCandidate(
        Long id,
        String name,
        String appId,
        String appSecret,
        String accessToken,
        LocalDateTime tokenExpiresAt
    ) {}

    public record RenewalResultPayload(
        TokenRenewalStatus status,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt,
        String errorMessage
    ) {}
}
