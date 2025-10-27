package com.marketinghub.journey.execution.channel;

import com.marketinghub.journey.execution.config.SendGridProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles email dispatch via SendGrid.
 */
@Component
@Slf4j
public class SendGridEmailChannelHandler implements JourneyChannelHandler {
    private final RestTemplate restTemplate;
    private final SendGridProperties properties;

    @Autowired
    public SendGridEmailChannelHandler(RestTemplateBuilder restTemplateBuilder,
                                       SendGridProperties properties) {
        this(restTemplateBuilder.build(), properties);
    }

    SendGridEmailChannelHandler(RestTemplate restTemplate, SendGridProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public JourneyStimulusType supportedType() {
        return JourneyStimulusType.EMAIL;
    }

    @Override
    public ChannelDispatchResult dispatch(JourneyAssignment assignment, JourneyStep step, Map<String, Object> context) {
        if (!properties.isEnabled()) {
            return ChannelDispatchResult.permanentFailure("SendGrid integration disabled", Map.of());
        }
        if (properties.getApiKey() == null || properties.getFromEmail() == null) {
            return ChannelDispatchResult.permanentFailure("SendGrid API key or sender email missing", Map.of());
        }
        String toEmail = resolveRecipient(context);
        if (toEmail == null) {
            return ChannelDispatchResult.permanentFailure("Missing recipient email in context", Map.of());
        }

        try {
            String url = properties.getBaseUrl() + "/mail/send";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(properties.getApiKey());

            Map<String, Object> payload = buildPayload(step, context, toEmail);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().value() == 202 || response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("to", toEmail);
                metadata.put("templateId", step.getMetadata().get("templateId"));
                return ChannelDispatchResult.success(null, metadata);
            }
            if (response.getStatusCode().is5xxServerError() || response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Instant retryAt = parseRetryAfter(response.getHeaders().getFirst("Retry-After"));
                return ChannelDispatchResult.transientFailure("SendGrid transient response: " + response.getStatusCode(), retryAt, Map.of());
            }
            return ChannelDispatchResult.permanentFailure("SendGrid returned status " + response.getStatusCode(), Map.of());
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError() || ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Instant retryAt = parseRetryAfter(ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null);
                return ChannelDispatchResult.transientFailure(
                        "SendGrid error: " + ex.getStatusCode(),
                        retryAt,
                        Map.<String, Object>of("body", ex.getResponseBodyAsString()));
            }
            return ChannelDispatchResult.permanentFailure(
                    "SendGrid error: " + ex.getStatusCode(),
                    Map.<String, Object>of("body", ex.getResponseBodyAsString()));
        } catch (ResourceAccessException ex) {
            log.warn("SendGrid network error", ex);
            return ChannelDispatchResult.transientFailure("SendGrid network error", null, Map.of());
        }
    }

    private Map<String, Object> buildPayload(JourneyStep step, Map<String, Object> context, String toEmail) {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> from = new HashMap<>();
        from.put("email", properties.getFromEmail());
        if (properties.getFromName() != null) {
            from.put("name", properties.getFromName());
        }
        payload.put("from", from);

        Map<String, Object> personalization = new HashMap<>();
        personalization.put("to", List.of(Map.of("email", toEmail)));
        personalization.put("dynamic_template_data", context);
        payload.put("personalizations", List.of(personalization));

        if (step.getMetadata().containsKey("templateId")) {
            payload.put("template_id", step.getMetadata().get("templateId"));
        }
        if (step.getMetadata().containsKey("subject")) {
            personalization.put("subject", step.getMetadata().get("subject"));
        }
        if (step.getMetadata().containsKey("content")) {
            payload.put("content", List.of(Map.of(
                    "type", "text/plain",
                    "value", Objects.toString(step.getMetadata().get("content"))
            )));
        }
        return payload;
    }

    private String resolveRecipient(Map<String, Object> context) {
        Object email = context.get("email");
        if (email instanceof String str && !str.isBlank()) {
            return str;
        }
        Object nested = context.get("lead");
        if (nested instanceof Map<?, ?> leadMap) {
            Object nestedEmail = leadMap.get("email");
            if (nestedEmail instanceof String nestedStr && !nestedStr.isBlank()) {
                return nestedStr;
            }
        }
        return null;
    }

    private Instant parseRetryAfter(String value) {
        if (value == null) {
            return null;
        }
        try {
            long seconds = Long.parseLong(value);
            return Instant.now().plusSeconds(seconds);
        } catch (NumberFormatException ex) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}
