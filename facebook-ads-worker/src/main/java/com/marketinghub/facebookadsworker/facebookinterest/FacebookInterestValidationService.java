package com.marketinghub.facebookadsworker.facebookinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class FacebookInterestValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookInterestValidationService.class);

    private final WebClient backendClient;
    private final FacebookAdsService facebookAdsService;
    private final FacebookInterestValidationClient validationClient;
    private final ObjectMapper objectMapper;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FacebookInterestValidationService(
        WebClient.Builder builder,
        FacebookAdsService facebookAdsService,
        FacebookInterestValidationClient validationClient,
        ObjectMapper objectMapper,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.facebookAdsService = facebookAdsService;
        this.validationClient = validationClient;
        this.objectMapper = objectMapper;
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public void validatePendingInterests() {
        List<FacebookInterestValidationClient.PendingInterest> pending = fetchPendingInterests();
        if (pending.isEmpty()) {
            LOGGER.debug("No pending Facebook interests to validate");
            return;
        }

        pending.forEach(this::validateInterest);
    }

    private void validateInterest(FacebookInterestValidationClient.PendingInterest interest) {
        Objects.requireNonNull(interest, "interest");
        FacebookAdsService.FacebookInterest match = facebookAdsService.lookupClosestInterest(interest.name());
        if (match == null) {
            LOGGER.warn("Marking Facebook interest id={} as INVALID because no match was found", interest.id());
            validationClient.reportResult(interest.id(), FacebookInterestValidationStatus.INVALID, null, null);
            return;
        }

        String resolvedName = match.name() != null ? match.name() : interest.name();
        LOGGER.info(
            "Resolved Facebook interest id={} to Graph API code {} (name={})",
            interest.id(),
            match.id(),
            resolvedName
        );
        validationClient.reportResult(
            interest.id(),
            FacebookInterestValidationStatus.VALID,
            match.id(),
            resolvedName
        );
    }

    private List<FacebookInterestValidationClient.PendingInterest> fetchPendingInterests() {
        String url = validationClient.buildPendingInterestsUrl(backendBaseUrl, apiPrefix);
        LOGGER.info(
            "Requesting pending Facebook interests for validation: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(Collections.emptyMap())
        );
        try {
            List<FacebookInterestValidationClient.PendingInterest> interests = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(FacebookInterestValidationClient.PendingInterest.class)
                .collectList()
                .blockOptional()
                .orElse(Collections.emptyList());
            LOGGER.info(
                "Received pending Facebook interests from backend: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, interests)
            );
            return interests;
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to fetch pending Facebook interests: url<=={}, status={}, message={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
            return Collections.emptyList();
        }
    }
}
