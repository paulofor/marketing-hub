package com.marketinghub.facebookadsworker.facebooktargeting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Cliente HTTP usado para reportar o status dos candidatos ao backend.
 */
@Component
public class TargetingBackendClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetingBackendClient.class);

    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public TargetingBackendClient(WebClient.Builder builder,
                                  @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                  @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public void reportResolution(Long candidateId, TargetingCandidateResolutionUpdate payload) {
        if (candidateId == null || payload == null) {
            return;
        }
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/targeting/candidates/" + candidateId);
        LOGGER.info(
            "Reporting targeting candidate resolution: url==>{}, payload={}",
            url,
            JsonLogFormatter.wrap(payload)
        );
        try {
            backendClient
                .patch()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info("Backend acknowledged targeting candidate resolution: url<=={}", url);
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to report targeting candidate resolution: url<=={}, status={}, message={}, payload={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                JsonLogFormatter.wrap(payload),
                ex
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TargetingCandidateResolutionUpdate(
        @JsonProperty("status") TargetingCandidateStatus status,
        @JsonProperty("rejection_reason") String rejectionReason,
        @JsonProperty("options") List<TargetingOptionPayload> options
    ) {
        public TargetingCandidateResolutionUpdate {
            if (options == null) {
                options = Collections.emptyList();
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TargetingOptionPayload(
        @JsonProperty("facebook_id") String facebookId,
        @JsonProperty("name") String name,
        @JsonProperty("type") TargetingCandidateType type,
        @JsonProperty("audience_size") Long audienceSize,
        @JsonProperty("match_score") BigDecimal matchScore,
        @JsonProperty("path") List<String> path,
        @JsonProperty("search_locale") String searchLocale,
        @JsonProperty("search_country") String searchCountry,
        @JsonProperty("search_term") String searchTerm
    ) {
        public TargetingOptionPayload {
            if (CollectionUtils.isEmpty(path)) {
                path = Collections.emptyList();
            }
        }
    }
}
