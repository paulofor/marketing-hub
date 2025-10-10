package com.marketinghub.journey.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.execution.config.Ga4Properties;
import com.marketinghub.journey.execution.config.MetaMarketingProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bridges server-side events to Meta Pixel and GA4 Measurement Protocol.
 */
@Service
@Slf4j
public class TelemetryService {
    private final RestTemplate metaRestTemplate;
    private final RestTemplate gaRestTemplate;
    private final MetaMarketingProperties metaProperties;
    private final Ga4Properties ga4Properties;
    private final ObjectMapper objectMapper;

    public TelemetryService(RestTemplateBuilder restTemplateBuilder,
                            MetaMarketingProperties metaProperties,
                            Ga4Properties ga4Properties,
                            ObjectMapper objectMapper) {
        this.metaRestTemplate = restTemplateBuilder.build();
        this.gaRestTemplate = restTemplateBuilder.build();
        this.metaProperties = metaProperties;
        this.ga4Properties = ga4Properties;
        this.objectMapper = objectMapper;
    }

    public void emitStepDispatched(JourneyAssignment assignment,
                                   JourneyStep step,
                                   Map<String, Object> context,
                                   Map<String, Object> dispatchMetadata) {
        Instant now = Instant.now();
        if (metaProperties.isPixelEnabled()) {
            sendPixelEvent(assignment, step, dispatchMetadata, now);
        }
        if (ga4Properties.isEnabled()) {
            sendGa4Event(assignment, step, dispatchMetadata, now, context);
        }
    }

    private void sendPixelEvent(JourneyAssignment assignment,
                                JourneyStep step,
                                Map<String, Object> dispatchMetadata,
                                Instant now) {
        if (metaProperties.getPixelId() == null || metaProperties.getAccessToken() == null) {
            log.debug("Pixel not configured, skipping telemetry");
            return;
        }
        String url = metaProperties.getBaseUrl() + "/" + metaProperties.getPixelId() + "/events";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(metaProperties.getAccessToken());

        Map<String, Object> customData = new HashMap<>();
        customData.put("journey_id", assignment.getJourney().getId());
        customData.put("step_id", step.getId());
        customData.put("stimulus_type", step.getStimulusType().name());
        customData.put("phase", step.getPhase().name());
        if (dispatchMetadata != null) {
            Object providerMessageId = dispatchMetadata.get("providerMessageId");
            if (providerMessageId != null) {
                customData.put("provider_message_id", providerMessageId);
            }
            Object value = dispatchMetadata.get("value");
            if (value != null) {
                customData.put("value", value);
            }
        }

        Map<String, Object> event = new HashMap<>();
        event.put("event_name", resolvePixelEventName(step));
        event.put("event_time", now.getEpochSecond());
        event.put("event_id", buildEventId(assignment, step));
        event.put("custom_data", customData);

        Map<String, Object> payload = Map.of("data", List.of(event));
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            metaRestTemplate.exchange(url, HttpMethod.POST, request, String.class);
        } catch (RestClientException ex) {
            log.warn("Failed to send Meta Pixel event", ex);
        }
    }

    private void sendGa4Event(JourneyAssignment assignment,
                              JourneyStep step,
                              Map<String, Object> dispatchMetadata,
                              Instant now,
                              Map<String, Object> context) {
        if (ga4Properties.getMeasurementId() == null || ga4Properties.getApiSecret() == null) {
            log.debug("GA4 not configured, skipping telemetry");
            return;
        }
        String url = ga4Properties.getEndpoint() + "?measurement_id=" + ga4Properties.getMeasurementId() + "&api_secret=" + ga4Properties.getApiSecret();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> params = new HashMap<>();
        params.put("journey_id", assignment.getJourney().getId());
        params.put("step_id", step.getId());
        params.put("stimulus_type", step.getStimulusType().name());
        params.put("journey_phase", step.getPhase().name());
        params.put("engagement_time_msec", 1);
        if (dispatchMetadata != null) {
            Object providerMessageId = dispatchMetadata.get("providerMessageId");
            if (providerMessageId != null) {
                params.put("provider_message_id", providerMessageId);
            }
            Object value = dispatchMetadata.get("value");
            if (value != null) {
                params.put("value", value);
            }
        }
        if (context != null) {
            Object source = context.get("source");
            if (source instanceof String src) {
                params.put("source", src);
            }
        }

        Map<String, Object> event = new HashMap<>();
        event.put("name", resolveGa4EventName(step));
        event.put("params", params);

        Map<String, Object> payload = new HashMap<>();
        payload.put("client_id", resolveClientId(assignment));
        payload.put("timestamp_micros", now.toEpochMilli() * 1000);
        payload.put("events", List.of(event));
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpEntity<String> request = new HttpEntity<>(json, headers);
            gaRestTemplate.exchange(url, HttpMethod.POST, request, String.class);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialise GA4 payload", e);
        } catch (RestClientException ex) {
            log.warn("Failed to send GA4 event", ex);
        }
    }

    private String resolvePixelEventName(JourneyStep step) {
        String metadataEvent = step.getMetadata().get("pixelEvent");
        if (metadataEvent != null && !metadataEvent.isBlank()) {
            return metadataEvent;
        }
        return switch (step.getPhase()) {
            case ACTION -> "Purchase";
            case DESIRE -> "AddToCart";
            default -> "Lead";
        };
    }

    private String resolveGa4EventName(JourneyStep step) {
        String metadataEvent = step.getMetadata().get("ga4Event");
        if (metadataEvent != null && !metadataEvent.isBlank()) {
            return metadataEvent;
        }
        return switch (step.getStimulusType()) {
            case AD -> "ad_impression";
            case EMAIL -> "email_sent";
            case WHATSAPP -> "whatsapp_message_sent";
            case LANDING_PAGE -> "landing_view";
            case INSTANT_FORM -> "instant_form_view";
        };
    }

    private String buildEventId(JourneyAssignment assignment, JourneyStep step) {
        return assignment.getId() + "-" + step.getId();
    }

    private String resolveClientId(JourneyAssignment assignment) {
        UUID actor = assignment.getLead() != null ? assignment.getLead().getId() : null;
        if (actor != null) {
            return actor.toString();
        }
        return assignment.getId().toString();
    }
}
