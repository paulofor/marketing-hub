package com.marketinghub.facebookadsworker.configuration;

import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

import java.util.Optional;

@Component
public class FacebookWorkerConfigurationClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookWorkerConfigurationClient.class);

    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FacebookWorkerConfigurationClient(
        WebClient.Builder builder,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public Optional<FacebookWorkerConfiguration> fetchConfiguration() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/accounts/facebook/worker-config");
        try {
            FacebookWorkerConfiguration configuration = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(FacebookWorkerConfiguration.class)
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    LOGGER.warn("Facebook worker configuration not found in backend; skipping Facebook automation");
                    return Mono.empty();
                })
                .block();
            return Optional.ofNullable(configuration);
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Failed to fetch Facebook worker configuration from backend: status={}, response={}",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                ex
            );
        } catch (WebClientRequestException ex) {
            Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(ex);
            if (rootCause instanceof PrematureCloseException) {
                LOGGER.warn(
                    "Failed to fetch Facebook worker configuration from backend: {}. Connection will be retried on the next cycle.",
                    rootCause.getMessage()
                );
            } else {
                LOGGER.error("Failed to fetch Facebook worker configuration from backend: {}", ex.getMessage(), ex);
            }
        }
        return Optional.empty();
    }

    public record FacebookWorkerConfiguration(
        Long accountId,
        String adAccountId,
        String accessToken,
        String appId,
        String appSecret,
        String defaultPageId,
        String defaultInstagramActorId,
        String defaultWebsiteUrl,
        String defaultCreativeMessageTemplate,
        String defaultCallToActionType,
        String adSetDailyBudget,
        String adSetBillingEvent,
        String adSetOptimizationGoal,
        String adSetDestinationType,
        String adSetBidStrategy,
        String adSetBidAmount,
        String adSetTargetCountry
    ) {}
}
