package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FacebookCampaignMetricsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignMetricsService.class);

    private final FacebookAdsService facebookAdsService;
    private final FacebookAccessTokenManager accessTokenManager;
    private final WebClient backendClient;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean configurationUnavailableWarningLogged = new AtomicBoolean(false);

    public FacebookCampaignMetricsService(FacebookAdsService facebookAdsService,
                                          FacebookAccessTokenManager accessTokenManager,
                                          FacebookWorkerConfigurationClient configurationClient,
                                          WebClient.Builder builder,
                                          @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                          @Value("${backend.api-prefix:/api}") String apiPrefix,
                                          ObjectMapper objectMapper) {
        this.facebookAdsService = facebookAdsService;
        this.accessTokenManager = accessTokenManager;
        this.configurationClient = configurationClient;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.objectMapper = objectMapper;
    }

    public void syncCampaignMetrics() {
        Optional<FacebookWorkerConfiguration> configuration = configurationClient.fetchConfiguration();
        if (configuration.isEmpty()) {
            if (configurationUnavailableWarningLogged.compareAndSet(false, true)) {
                LOGGER.warn("Facebook worker configuration is unavailable; skipping campaign metrics sync");
            }
            return;
        }
        configurationUnavailableWarningLogged.set(false);

        FacebookWorkerConfiguration config = configuration.get();
        if (!StringUtils.hasText(config.accessToken())) {
            LOGGER.warn("Facebook worker configuration does not include an access token; skipping campaign metrics sync");
            return;
        }
        ensureAccessToken(config.accessToken());

        List<CampaignMetricsSyncTarget> targets = fetchSyncTargets();
        if (targets.isEmpty()) {
            return;
        }

        for (CampaignMetricsSyncTarget target : targets) {
            processTarget(target);
        }
    }

    private void processTarget(CampaignMetricsSyncTarget target) {
        try {
            JsonNode insights = facebookAdsService.getCampaignInsights(target.campaignId(), buildInsightsQuery());
            JsonNode data = insights.path("data");
            if (!data.isArray() || data.isEmpty()) {
                LOGGER.warn("Facebook returned no insights data for campaign {}", target.campaignId());
                reportMetricsError(target.campaignId(), "Facebook did not return insights data for campaign");
                return;
            }
            JsonNode row = data.get(0);
            CampaignMetricsUpdateRequest payload = mapToPayload(row);
            if (payload == null) {
                reportMetricsError(target.campaignId(), "Could not parse insights payload");
                return;
            }
            sendMetrics(target.campaignId(), payload);
        } catch (FacebookAccessTokenExpiredException ex) {
            LOGGER.warn("Facebook access token expired while fetching metrics for campaign {}", target.campaignId(), ex);
            if (tryRenewAccessToken()) {
                processTarget(target);
            }
        } catch (FacebookPermissionException ex) {
            LOGGER.warn("Facebook permission error while fetching metrics for campaign {}: {}", target.campaignId(), ex.getMessage());
            reportMetricsError(target.campaignId(), "Permission error: " + ex.getMessage());
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Backend connectivity error while syncing metrics for campaign {}", target.campaignId(), ex);
        } catch (Exception ex) {
            LOGGER.warn("Unexpected error while syncing metrics for campaign {}: {}", target.campaignId(), ex.getMessage(), ex);
            reportMetricsError(target.campaignId(), "Unexpected error: " + ex.getMessage());
        }
    }

    private Map<String, String> buildInsightsQuery() {
        Map<String, String> params = new HashMap<>();
        params.put("fields", "campaign_name,impressions,clicks,spend,actions,date_start,date_stop");
        params.put("date_preset", "maximum");
        params.put("time_increment", "all_days");
        return params;
    }

    private CampaignMetricsUpdateRequest mapToPayload(JsonNode row) {
        if (row == null || row.isNull()) {
            return null;
        }
        LocalDate dateStart = parseDate(row.path("date_start").asText(null));
        LocalDate dateStop = parseDate(row.path("date_stop").asText(null));
        Long impressions = parseLong(row.path("impressions"));
        Long clicks = parseLong(row.path("clicks"));
        BigDecimal spend = parseBigDecimal(row.path("spend"));
        Long leads = extractLeadCount(row.path("actions"));
        return new CampaignMetricsUpdateRequest(dateStart, dateStop, impressions, clicks, leads, spend);
    }

    private Long parseLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0L;
        }
        try {
            return node.asLong();
        } catch (Exception ex) {
            return 0L;
        }
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        String text = node.asText();
        if (!StringUtils.hasText(text)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            LOGGER.debug("Could not parse BigDecimal from value {}: {}", text, ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private long extractLeadCount(JsonNode actionsNode) {
        if (actionsNode == null || !actionsNode.isArray()) {
            return 0L;
        }
        long total = 0;
        for (JsonNode action : actionsNode) {
            String type = action.path("action_type").asText("");
            if (!StringUtils.hasText(type)) {
                continue;
            }
            if (type.toLowerCase().contains("lead")) {
                total += parseLong(action.path("value"));
            }
        }
        return total;
    }

    private void sendMetrics(String campaignId, CampaignMetricsUpdateRequest payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/metrics");
        LOGGER.info(
            "Reporting Facebook campaign metrics to backend: url==>{}, payload={}",
            url,
            JsonLogFormatter.wrap(objectMapper, payload)
        );
        backendClient.post()
            .uri(url)
            .bodyValue(payload)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> response.createException().flatMap(e -> {
                LOGGER.warn("Backend rejected metrics for campaign {}: status={} message={}", campaignId, response.statusCode(), e.getMessage());
                return Mono.error(e);
            }))
            .toBodilessEntity()
            .block();
    }

    private void reportMetricsError(String campaignId, String message) {
        String sanitized = sanitizeMessage(message);
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/metrics-error");
        CampaignMetricsErrorRequest request = new CampaignMetricsErrorRequest(sanitized);
        LOGGER.info(
            "Reporting Facebook campaign metrics error to backend: url==>{}, payload={}",
            url,
            JsonLogFormatter.wrap(objectMapper, request)
        );
        try {
            backendClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (Exception ex) {
            LOGGER.warn("Could not report metrics error for campaign {}: {}", campaignId, ex.getMessage(), ex);
        }
    }

    private String sanitizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "Unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private List<CampaignMetricsSyncTarget> fetchSyncTargets() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/metrics/sync-targets");
        LOGGER.info("Requesting campaign metrics sync targets from backend: url==>{}", url);
        try {
            List<CampaignMetricsSyncTarget> targets = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(CampaignMetricsSyncTarget.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received campaign metrics sync targets response: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, targets)
            );
            return targets != null ? targets : Collections.emptyList();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Could not fetch campaign metrics sync targets from backend: url==>{}", url, ex);
            return Collections.emptyList();
        }
    }

    private void ensureAccessToken(String configuredToken) {
        String currentToken = facebookAdsService.getCurrentAccessToken();
        if (!StringUtils.hasText(currentToken) || !currentToken.equals(configuredToken)) {
            facebookAdsService.updateAccessToken(configuredToken);
        }
    }

    private boolean tryRenewAccessToken() {
        FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
            LOGGER.info("Facebook access token renewed automatically during metrics sync");
            return true;
        }
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED) {
            LOGGER.warn("Automatic token renewal is not configured; metrics sync will remain paused until a valid token is provided");
        } else {
            LOGGER.warn("Automatic token renewal failed: {}", renewalResult.errorMessage());
        }
        return false;
    }

    public record CampaignMetricsSyncTarget(String campaignId, long experimentId, Instant lastSyncedAt) {}

    public record CampaignMetricsUpdateRequest(
            LocalDate dateStart,
            LocalDate dateStop,
            Long impressions,
            Long clicks,
            Long leads,
            BigDecimal spend) {}

    public record CampaignMetricsErrorRequest(String message) {}
}
