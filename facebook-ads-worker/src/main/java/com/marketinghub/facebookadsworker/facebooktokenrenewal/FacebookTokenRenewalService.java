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
    private final FacebookTokenRevalidationClient tokenRevalidationClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FacebookTokenRenewalService(
        WebClient.Builder builder,
        FacebookAdsService facebookAdsService,
        FacebookTokenRevalidationClient tokenRevalidationClient,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.facebookAdsService = facebookAdsService;
        this.tokenRevalidationClient = tokenRevalidationClient;
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
        LocalDateTime attemptTime = LocalDateTime.now();
        LOGGER.info(
            "Attempting token renewal for Facebook account id={}, name={}, expiresAt={}",
            candidate.id(),
            candidate.name(),
            candidate.tokenExpiresAt()
        );

        FacebookTokenRevalidationClient.TokenRevalidationResponse response = tokenRevalidationClient
            .revalidate(candidate.id())
            .orElse(null);

        if (response == null) {
            LOGGER.error(
                "Token generation request did not return a response: accountId={}, name={}",
                candidate.id(),
                candidate.name()
            );
            return;
        }

        if (response.status() == FacebookTokenRevalidationClient.TokenRevalidationStatus.SUCCESS) {
            boolean matchesCurrentToken = Objects.equals(
                candidate.accessToken(),
                facebookAdsService.getCurrentAccessToken()
            );
            if (matchesCurrentToken && response.accessToken() != null) {
                facebookAdsService.updateAccessToken(response.accessToken());
                LOGGER.info(
                    "Updated in-memory Facebook access token after backend generation for account id={}",
                    candidate.id()
                );
            } else if (!matchesCurrentToken) {
                LOGGER.debug(
                    "Skipping in-memory Facebook token update because candidate token does not match the worker configuration: accountId={}",
                    candidate.id()
                );
            }
            LOGGER.info(
                "Facebook token generation succeeded for account id={} via backend", candidate.id()
            );
        } else {
            LOGGER.error(
                "Token generation failed for Facebook account id={}, name={}: {}",
                candidate.id(),
                candidate.name(),
                response.errorMessage()
            );
        }
    }

    public record FacebookAccountRenewalCandidate(
        Long id,
        String name,
        String appId,
        String appSecret,
        String accessToken,
        LocalDateTime tokenExpiresAt
    ) {}
}
