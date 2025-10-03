package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.Optional;

@Component
public class FacebookTokenRevalidationClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookTokenRevalidationClient.class);

    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FacebookTokenRevalidationClient(
        WebClient.Builder builder,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public Optional<TokenRevalidationResponse> revalidate(Long accountId) {
        String path = "/accounts/facebook/" + accountId + "/token/revalidation";
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, path);
        LOGGER.info(
            "Requesting new Facebook access token from backend: url==>{}, params={}",
            url,
            Collections.emptyMap()
        );
        try {
            TokenRevalidationResponse response = backendClient
                .post()
                .uri(url)
                .retrieve()
                .bodyToMono(TokenRevalidationResponse.class)
                .block();
            LOGGER.info(
                "Received Facebook token generation response from backend: url<=={}, response={}",
                url,
                response
            );
            return Optional.ofNullable(response);
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to generate new Facebook access token via backend: url<=={}, status={}, message={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
            return Optional.empty();
        }
    }

    public enum TokenRevalidationStatus {
        SUCCESS,
        FAILED
    }

    public record TokenRevalidationResponse(
        TokenRevalidationStatus status,
        String accessToken,
        java.time.LocalDateTime tokenExpiresAt,
        java.time.LocalDateTime renewedAt,
        java.time.LocalDateTime attemptedAt,
        String errorMessage
    ) {}
}
