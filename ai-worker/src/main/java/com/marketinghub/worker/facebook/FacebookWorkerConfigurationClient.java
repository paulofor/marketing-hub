package com.marketinghub.worker.facebook;

import com.marketinghub.worker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight client to reuse the Facebook Ads worker configuration exposed by the backend.
 */
@Component
public class FacebookWorkerConfigurationClient {
    private static final Logger log = LoggerFactory.getLogger(FacebookWorkerConfigurationClient.class);

    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final AtomicBoolean configurationNotFoundLogged;
    private final AtomicBoolean prematureCloseWarningLogged;

    public FacebookWorkerConfigurationClient(
            WebClient.Builder builder,
            @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
            @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.configurationNotFoundLogged = new AtomicBoolean(false);
        this.prematureCloseWarningLogged = new AtomicBoolean(false);
    }

    public Optional<FacebookWorkerConfiguration> fetchConfiguration() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/accounts/facebook/worker-config");
        log.info("Requesting Facebook worker configuration from backend: url==>{}, params={}", url, Collections.emptyMap());
        try {
            FacebookWorkerConfiguration configuration = backendClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(FacebookWorkerConfiguration.class)
                    .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                        if (configurationNotFoundLogged.compareAndSet(false, true)) {
                            log.warn("Facebook worker configuration not found in backend; instant form creation will be skipped");
                        }
                        log.info("Backend responded with HTTP 404 for worker configuration: url<=={}, response={}", url, ex.getResponseBodyAsString());
                        return Mono.empty();
                    })
                    .block();
            if (configuration != null) {
                configurationNotFoundLogged.set(false);
                prematureCloseWarningLogged.set(false);
            }
            log.info("Received Facebook worker configuration response from backend: url<=={}, response={}", url, configuration);
            return Optional.ofNullable(configuration);
        } catch (WebClientResponseException ex) {
            log.error("Failed to fetch Facebook worker configuration from backend: status={}, response={}", ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (WebClientRequestException ex) {
            Throwable rootCause = ex.getRootCause();
            if (rootCause instanceof PrematureCloseException) {
                if (prematureCloseWarningLogged.compareAndSet(false, true)) {
                    log.warn("Connection closed prematurely while fetching Facebook worker configuration; will retry later: {}", rootCause.getMessage());
                }
            } else {
                log.error("Failed to fetch Facebook worker configuration from backend: {}", ex.getMessage(), ex);
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
            String defaultLeadGenFormId,
            String defaultCreativeMessageTemplate,
            String defaultCallToActionType,
            String adSetDailyBudget,
            String adSetBillingEvent,
            String adSetOptimizationGoal,
            String adSetDestinationType,
            String adSetBidStrategy,
            String adSetBidAmount,
            String adSetTargetCountry
    ) {
    }
}
