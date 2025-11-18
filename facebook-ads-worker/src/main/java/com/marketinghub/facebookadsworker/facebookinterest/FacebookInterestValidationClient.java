package com.marketinghub.facebookadsworker.facebookinterest;

import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class FacebookInterestValidationClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookInterestValidationClient.class);

    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FacebookInterestValidationClient(
        WebClient.Builder builder,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public void reportResult(Long interestId, FacebookInterestValidationStatus status, String facebookInterestId, String name) {
        FacebookInterestValidationUpdateRequest payload = new FacebookInterestValidationUpdateRequest(status, facebookInterestId, name);
        String url = buildUpdateUrl(interestId, backendBaseUrl, apiPrefix);
        LOGGER.info(
            "Reporting Facebook interest validation result: url==>{}, payload={}",
            url,
            JsonLogFormatter.wrap(payload)
        );
        try {
            backendClient
                .patch()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info("Backend acknowledged Facebook interest validation result: url<=={}", url);
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to report Facebook interest validation result: url<=={}, status={}, message={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
        }
    }

    public String buildPendingInterestsUrl(String backendBaseUrl, String apiPrefix) {
        return UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-interests/pending");
    }

    public String buildUpdateUrl(Long interestId, String backendBaseUrl, String apiPrefix) {
        return UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-interests/" + interestId);
    }

    public record FacebookInterestValidationUpdateRequest(
        FacebookInterestValidationStatus status,
        String facebookInterestId,
        String name
    ) {}

    public record PendingInterest(Long id, String name) {}
}
