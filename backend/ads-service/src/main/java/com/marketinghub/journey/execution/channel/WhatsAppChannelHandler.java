package com.marketinghub.journey.execution.channel;

import com.marketinghub.journey.execution.config.WhatsAppProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Handles WhatsApp messaging through Meta Cloud API.
 */
@Component
@Slf4j
public class WhatsAppChannelHandler implements JourneyChannelHandler {
    private final RestTemplate restTemplate;
    private final WhatsAppProperties properties;

    public WhatsAppChannelHandler(RestTemplateBuilder restTemplateBuilder,
                                  WhatsAppProperties properties) {
        this(restTemplateBuilder.build(), properties);
    }

    WhatsAppChannelHandler(RestTemplate restTemplate, WhatsAppProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public JourneyStimulusType supportedType() {
        return JourneyStimulusType.WHATSAPP;
    }

    @Override
    public ChannelDispatchResult dispatch(JourneyAssignment assignment, JourneyStep step, Map<String, Object> context) {
        if (!properties.isEnabled()) {
            return ChannelDispatchResult.permanentFailure("WhatsApp integration disabled", Map.of());
        }
        if (properties.getAccessToken() == null || properties.getPhoneNumberId() == null) {
            return ChannelDispatchResult.permanentFailure("WhatsApp credentials missing", Map.of());
        }
        String to = resolvePhone(context);
        if (to == null) {
            return ChannelDispatchResult.permanentFailure("Missing WhatsApp phone in context", Map.of());
        }

        try {
            String url = properties.getBaseUrl() + "/" + properties.getPhoneNumberId() + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(properties.getAccessToken());

            Map<String, Object> payload = buildPayload(step, context, to);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<WhatsAppResponse> response = restTemplate.exchange(url, HttpMethod.POST, request, WhatsAppResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                WhatsAppResponse body = response.getBody();
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("to", to);
                metadata.put("type", payload.get("type"));
                String messageId = null;
                if (body.messages() != null && !body.messages().isEmpty()) {
                    messageId = body.messages().get(0).id();
                    metadata.put("messageId", messageId);
                }
                return ChannelDispatchResult.success(messageId, metadata);
            }
            if (response.getStatusCode().is5xxServerError() || response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Instant retryAt = parseRetryAfter(response.getHeaders().getFirst("Retry-After"));
                return ChannelDispatchResult.transientFailure("WhatsApp transient response: " + response.getStatusCode(), retryAt, Map.of());
            }
            return ChannelDispatchResult.permanentFailure("WhatsApp returned status " + response.getStatusCode(), Map.of());
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError() || ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Instant retryAt = parseRetryAfter(ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null);
                return ChannelDispatchResult.transientFailure("WhatsApp error: " + ex.getStatusCode(), retryAt, Map.of("body", ex.getResponseBodyAsString()));
            }
            return ChannelDispatchResult.permanentFailure("WhatsApp error: " + ex.getStatusCode(), Map.of("body", ex.getResponseBodyAsString()));
        } catch (ResourceAccessException ex) {
            log.warn("WhatsApp network error", ex);
            return ChannelDispatchResult.transientFailure("WhatsApp network error", null, Map.of());
        }
    }

    private Map<String, Object> buildPayload(JourneyStep step, Map<String, Object> context, String to) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        if (step.getMetadata().containsKey("templateName")) {
            payload.put("type", "template");
            Map<String, Object> template = new HashMap<>();
            template.put("name", step.getMetadata().get("templateName"));
            Map<String, Object> language = new HashMap<>();
            language.put("code", step.getMetadata().getOrDefault("templateLanguage", "en_US"));
            template.put("language", language);
            payload.put("template", template);
        } else {
            payload.put("type", "text");
            Map<String, Object> text = new HashMap<>();
            Object body = step.getMetadata().getOrDefault("body", context.get("message"));
            if (body == null) {
                throw new IllegalArgumentException("Missing WhatsApp message body");
            }
            text.put("preview_url", Boolean.FALSE);
            text.put("body", body.toString());
            payload.put("text", text);
        }
        return payload;
    }

    private String resolvePhone(Map<String, Object> context) {
        Object phone = context.get("phone");
        if (phone instanceof String str && !str.isBlank()) {
            return str;
        }
        Object whatsapp = context.get("whatsapp");
        if (whatsapp instanceof String whats && !whats.isBlank()) {
            return whats;
        }
        Object nested = context.get("lead");
        if (nested instanceof Map<?, ?> map) {
            Object nestedPhone = map.get("phone");
            if (nestedPhone instanceof String nestedStr && !nestedStr.isBlank()) {
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

    private record WhatsAppResponse(List<Message> messages) {
        private record Message(String id) {
        }
    }
}
