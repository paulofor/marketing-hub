package com.marketinghub.journey.execution.channel;

import com.marketinghub.journey.execution.config.MetaMarketingProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler responsible for dispatching ads through Meta Marketing API.
 */
@Component
@Slf4j
public class MetaAdsChannelHandler implements JourneyChannelHandler {
    private final RestTemplate restTemplate;
    private final MetaMarketingProperties properties;

    public MetaAdsChannelHandler(RestTemplateBuilder restTemplateBuilder,
                                 MetaMarketingProperties properties) {
        this(restTemplateBuilder.build(), properties);
    }

    MetaAdsChannelHandler(RestTemplate restTemplate, MetaMarketingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public JourneyStimulusType supportedType() {
        return JourneyStimulusType.AD;
    }

    @Override
    public ChannelDispatchResult dispatch(JourneyAssignment assignment, JourneyStep step, Map<String, Object> context) {
        if (!properties.isEnabled()) {
            return ChannelDispatchResult.permanentFailure("Meta Marketing API integration disabled", Map.of());
        }
        if (properties.getAccessToken() == null || properties.getAdAccountId() == null) {
            return ChannelDispatchResult.permanentFailure("Meta access token or ad account not configured", Map.of());
        }

        try {
            String url = properties.getBaseUrl() + "/act_" + properties.getAdAccountId() + "/ads";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(properties.getAccessToken());

            Map<String, Object> payload = buildPayload(step);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<MetaAdResponse> response = restTemplate.exchange(url, HttpMethod.POST, request, MetaAdResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MetaAdResponse body = response.getBody();
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("adId", body.id());
                metadata.put("creativeId", safeMetadata(step).get("creativeId"));
                metadata.put("status", payload.get("status"));
                return ChannelDispatchResult.success(body.id(), metadata);
            }
            if (response.getStatusCode().is5xxServerError() || response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Instant retryAt = resolveRetryAt(response.getHeaders());
                return ChannelDispatchResult.transientFailure("Meta API transient response: " + response.getStatusCode(), retryAt, Map.of());
            }
            return ChannelDispatchResult.permanentFailure("Meta API returned status " + response.getStatusCode(), Map.of());
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError() || ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Instant retryAt = resolveRetryAt(ex.getResponseHeaders());
                return ChannelDispatchResult.transientFailure("Meta API error: " + ex.getStatusCode(), retryAt, Map.of("body", ex.getResponseBodyAsString()));
            }
            return ChannelDispatchResult.permanentFailure("Meta API error: " + ex.getStatusCode(), Map.of("body", ex.getResponseBodyAsString()));
        } catch (ResourceAccessException ex) {
            log.warn("Meta API network error", ex);
            return ChannelDispatchResult.transientFailure("Meta API network error", null, Map.of());
        }
    }

    private Map<String, Object> buildPayload(JourneyStep step) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", step.getName() != null ? step.getName() : "Journey Step " + step.getId());
        Map<String, String> metadata = safeMetadata(step);
        String status = metadata.getOrDefault("status", properties.getDefaultAdStatus());
        payload.put("status", status);
        if (metadata.containsKey("adsetId")) {
            payload.put("adset_id", metadata.get("adsetId"));
        }
        if (metadata.containsKey("campaignId")) {
            payload.put("campaign_id", metadata.get("campaignId"));
        }
        if (metadata.containsKey("creativeId")) {
            Map<String, Object> creative = new HashMap<>();
            creative.put("creative_id", metadata.get("creativeId"));
            payload.put("creative", creative);
        }
        if (metadata.containsKey("bidAmount")) {
            payload.put("bid_amount", metadata.get("bidAmount"));
        }
        return payload;
    }

    private Map<String, String> safeMetadata(JourneyStep step) {
        Map<String, String> metadata = step.getMetadata();
        return metadata != null ? metadata : Map.of();
    }

    private Instant resolveRetryAt(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter == null) {
            return null;
        }
        try {
            long seconds = Long.parseLong(retryAfter);
            return Instant.now().plusSeconds(seconds);
        } catch (NumberFormatException ignored) {
            try {
                return Instant.parse(retryAfter);
            } catch (DateTimeParseException e) {
                log.debug("Unable to parse Retry-After header: {}", retryAfter);
                return null;
            }
        }
    }

    private record MetaAdResponse(String id) {
    }
}
