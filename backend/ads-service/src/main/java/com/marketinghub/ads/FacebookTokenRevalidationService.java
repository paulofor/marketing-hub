package com.marketinghub.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Service
public class FacebookTokenRevalidationService {
    private static final Logger log = LoggerFactory.getLogger(FacebookTokenRevalidationService.class);

    private final RestTemplate restTemplate;
    private final FacebookAccountRepository repository;
    private final String graphApiBaseUrl;
    private final String graphApiVersion;

    public FacebookTokenRevalidationService(
        RestTemplateBuilder restTemplateBuilder,
        FacebookAccountRepository repository,
        @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String graphApiBaseUrl,
        @Value("${facebook.graph-api.version:v23.0}") String graphApiVersion
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.repository = repository;
        this.graphApiBaseUrl = trimTrailingSlash(graphApiBaseUrl);
        this.graphApiVersion = trimSlashes(graphApiVersion);
    }

    public RevalidationResult revalidate(FacebookAccount account) {
        LocalDateTime attemptedAt = LocalDateTime.now();
        String url = buildTokenExchangeUrl(account);
        log.info(
            "Requesting Facebook token revalidation via Graph API: accountId={}, url={}",
            account.getId(),
            url
        );

        try {
            GraphTokenResponse response = restTemplate.getForObject(url, GraphTokenResponse.class);
            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new IllegalStateException("Facebook returned an empty token while revalidating the access token");
            }

            LocalDateTime renewedAt = attemptedAt;
            LocalDateTime expiresAt = response.expiresIn() != null
                ? attemptedAt.plusSeconds(Math.max(response.expiresIn(), 0))
                : null;

            account.setAccessToken(response.accessToken());
            account.setTokenExpiresAt(expiresAt);
            account.setTokenLastRefreshedAt(renewedAt);
            account.setTokenRenewedAt(renewedAt);
            account.setTokenRenewalStatus(FacebookTokenRenewalStatus.SUCCESS.name());
            account.setTokenRenewalLastAttemptAt(attemptedAt);
            account.setTokenRenewalLastError(null);

            repository.save(account);

            log.info(
                "Facebook token revalidation succeeded: accountId={}, expiresAt={}",
                account.getId(),
                expiresAt
            );

            return new RevalidationResult(
                FacebookTokenRenewalStatus.SUCCESS,
                response.accessToken(),
                expiresAt,
                renewedAt,
                attemptedAt,
                null
            );
        } catch (RestClientResponseException ex) {
            String message = String.format(
                "HTTP %d: %s",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString()
            );
            log.error(
                "Facebook token revalidation failed: accountId={}, status={}, response={}",
                account.getId(),
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                ex
            );
            return registerFailure(account, attemptedAt, message);
        } catch (RestClientException ex) {
            log.error(
                "Facebook token revalidation failed: accountId={}, message={}",
                account.getId(),
                ex.getMessage(),
                ex
            );
            return registerFailure(account, attemptedAt, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                "Facebook token revalidation failed: accountId={}, message={}",
                account.getId(),
                ex.getMessage(),
                ex
            );
            return registerFailure(account, attemptedAt, ex.getMessage());
        }
    }

    private RevalidationResult registerFailure(FacebookAccount account, LocalDateTime attemptedAt, String errorMessage) {
        account.setTokenRenewalStatus(FacebookTokenRenewalStatus.FAILED.name());
        account.setTokenRenewalLastAttemptAt(attemptedAt);
        account.setTokenRenewalLastError(StringUtils.hasText(errorMessage) ? errorMessage : null);
        repository.save(account);
        return new RevalidationResult(
            FacebookTokenRenewalStatus.FAILED,
            null,
            null,
            null,
            attemptedAt,
            errorMessage
        );
    }

    private String buildTokenExchangeUrl(FacebookAccount account) {
        return UriComponentsBuilder
            .fromHttpUrl(graphApiBaseUrl)
            .pathSegment(graphApiVersion)
            .pathSegment("oauth")
            .pathSegment("access_token")
            .queryParam("grant_type", "fb_exchange_token")
            .queryParam("client_id", account.getAppId())
            .queryParam("client_secret", account.getAppSecret())
            .queryParam("fb_exchange_token", account.getAccessToken())
            .build()
            .toUriString();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://graph.facebook.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimSlashes(String value) {
        if (value == null || value.isBlank()) {
            return "v23.0";
        }
        String trimmed = value;
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public record RevalidationResult(
        FacebookTokenRenewalStatus status,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt,
        String errorMessage
    ) {}

    private record GraphTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn
    ) {}
}
