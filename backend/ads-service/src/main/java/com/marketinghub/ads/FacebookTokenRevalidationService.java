package com.marketinghub.ads;

import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final long LONG_LIVED_TOKEN_VALIDITY_DAYS = 60L;

    private final RestTemplate restTemplate;
    private final FacebookAccountRepository repository;
    private final ObjectMapper objectMapper;
    private final String graphApiBaseUrl;
    private final String graphApiVersion;

    public FacebookTokenRevalidationService(
        RestTemplateBuilder restTemplateBuilder,
        FacebookAccountRepository repository,
        ObjectMapper objectMapper,
        @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String graphApiBaseUrl,
        @Value("${facebook.graph-api.version:v23.0}") String graphApiVersion
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.graphApiBaseUrl = trimTrailingSlash(graphApiBaseUrl);
        this.graphApiVersion = trimSlashes(graphApiVersion);
    }

    public RevalidationResult revalidate(FacebookAccount account) {
        LocalDateTime attemptedAt = LocalDateTime.now();
        String url = buildTokenExchangeUrl(account);
        log.info(
            "Requesting Facebook long-lived token via Graph API: accountId={}, endpoint={}/{}",
            account.getId(),
            graphApiBaseUrl,
            graphApiVersion
        );
        log.debug(
            "Facebook token generation parameters: accountId={}, appId={}, accessToken={}, tokenRenewalEnabled={}",
            account.getId(),
            maskForLogs(account.getAppId()),
            maskForLogs(account.getAccessToken()),
            account.isTokenRenewalEnabled()
        );

        try {
            GraphTokenResponse response = restTemplate.getForObject(url, GraphTokenResponse.class);
            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new IllegalStateException("Facebook returned an empty token while generating a new access token");
            }

            LocalDateTime renewedAt = attemptedAt;
            LocalDateTime expiresAt = attemptedAt.plusDays(LONG_LIVED_TOKEN_VALIDITY_DAYS);

            account.setAccessToken(response.accessToken());
            account.setTokenExpiresAt(expiresAt);
            account.setTokenLastRefreshedAt(renewedAt);
            account.setTokenRenewedAt(renewedAt);
            account.setTokenRenewalStatus(FacebookTokenRenewalStatus.SUCCESS.name());
            account.setTokenRenewalLastAttemptAt(attemptedAt);
            account.setTokenRenewalLastError(null);

            repository.save(account);

            log.info("Generated new Facebook access token: accountId={}, expiresAt={}", account.getId(), expiresAt);
            log.debug(
                "Facebook token generation updated credentials: accountId={}, renewedAt={}, expiresInSeconds={}, configuredValidityDays={}",
                account.getId(),
                renewedAt,
                response.expiresIn(),
                LONG_LIVED_TOKEN_VALIDITY_DAYS
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
            GraphErrorResponse error = parseGraphError(ex.getResponseBodyAsString());
            String message = buildFailureMessage(ex, error);
            if (error != null && error.error() != null) {
                GraphError graphError = error.error();
                log.error(
                    "Facebook token generation failed: accountId={}, status={}, type={}, code={}, subCode={}, transient={}, fbtraceId={}, message={}",
                    account.getId(),
                    ex.getRawStatusCode(),
                    graphError.type(),
                    graphError.code(),
                    graphError.errorSubcode(),
                    graphError.isTransient(),
                    graphError.fbtraceId(),
                    graphError.message(),
                    ex
                );
            } else {
                log.error(
                    "Facebook token generation failed: accountId={}, status={}, response={}",
                    account.getId(),
                    ex.getRawStatusCode(),
                    sanitizeBody(ex.getResponseBodyAsString()),
                    ex
                );
            }
            return registerFailure(account, attemptedAt, message);
        } catch (RestClientException ex) {
            log.error(
                "Facebook token generation failed: accountId={}, message={}",
                account.getId(),
                ex.getMessage(),
                ex
            );
            return registerFailure(account, attemptedAt, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                "Facebook token generation failed: accountId={}, message={}",
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

    private String buildFailureMessage(RestClientResponseException ex, GraphErrorResponse error) {
        if (error != null && error.error() != null) {
            GraphError graphError = error.error();
            StringBuilder builder = new StringBuilder("Graph API error during token generation");
            if (StringUtils.hasText(graphError.type())) {
                builder.append(" (type=").append(graphError.type()).append(')');
            }
            if (graphError.code() != null) {
                builder.append(" [code=").append(graphError.code()).append(']');
            }
            if (graphError.errorSubcode() != null) {
                builder.append(" [subcode=").append(graphError.errorSubcode()).append(']');
            }
            boolean hasDetails =
                StringUtils.hasText(graphError.message()) ||
                Boolean.TRUE.equals(graphError.isTransient()) ||
                StringUtils.hasText(graphError.fbtraceId());
            if (hasDetails) {
                builder.append(':');
                if (StringUtils.hasText(graphError.message())) {
                    builder.append(' ').append(graphError.message());
                }
                if (Boolean.TRUE.equals(graphError.isTransient())) {
                    builder.append(' ').append("(transient error)");
                }
                if (StringUtils.hasText(graphError.fbtraceId())) {
                    builder.append(' ')
                        .append("[fbtrace_id=")
                        .append(graphError.fbtraceId())
                        .append(']');
                }
            }
            return builder.toString();
        }
        return String.format("HTTP %d: %s", ex.getRawStatusCode(), sanitizeBody(ex.getResponseBodyAsString()));
    }

    private GraphErrorResponse parseGraphError(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, GraphErrorResponse.class);
        } catch (JsonProcessingException parsingException) {
            log.debug("Unable to parse Graph API error response: {}", responseBody, parsingException);
            return null;
        }
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

    private String maskForLogs(String value) {
        if (!StringUtils.hasText(value)) {
            return "<empty>";
        }
        int length = value.length();
        if (length <= 8) {
            return "<redacted>";
        }
        return value.substring(0, 4) + "..." + value.substring(length - 4);
    }

    private String sanitizeBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "<empty>";
        }
        String trimmed = body.trim();
        if (trimmed.length() > 2048) {
            return trimmed.substring(0, 2048) + "...";
        }
        return trimmed;
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

    private record GraphErrorResponse(
        GraphError error
    ) {}

    private record GraphError(
        String message,
        String type,
        Integer code,
        @JsonProperty("error_subcode") Integer errorSubcode,
        @JsonProperty("is_transient") Boolean isTransient,
        @JsonProperty("fbtrace_id") String fbtraceId
    ) {}
}
