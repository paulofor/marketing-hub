package com.marketinghub.journey.execution.channel;

import com.marketinghub.journey.execution.config.WhatsAppProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.whatsapp.WhatsAppAccount;
import com.marketinghub.whatsapp.WhatsAppMessage;
import com.marketinghub.whatsapp.WhatsAppMessageType;
import com.marketinghub.whatsapp.service.WhatsAppAccountService;
import com.marketinghub.whatsapp.service.WhatsAppMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles WhatsApp messaging through Meta Cloud API.
 */
@Component
@Slf4j
public class WhatsAppChannelHandler implements JourneyChannelHandler {
    private final WhatsAppMessagingService messagingService;
    private final WhatsAppAccountService accountService;
    private final WhatsAppProperties properties;

    public WhatsAppChannelHandler(WhatsAppMessagingService messagingService,
                                  WhatsAppAccountService accountService,
                                  WhatsAppProperties properties) {
        this.messagingService = messagingService;
        this.accountService = accountService;
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
        Optional<WhatsAppAccount> accountOpt = accountService.findActiveAccount();
        if (accountOpt.isEmpty()) {
            return ChannelDispatchResult.permanentFailure("WhatsApp integration disabled", Map.of());
        }
        WhatsAppAccount account = accountOpt.get();
        if (!StringUtils.hasText(account.getAccessToken()) || !StringUtils.hasText(account.getPhoneNumberId())) {
            return ChannelDispatchResult.permanentFailure("WhatsApp credentials missing", Map.of());
        }
        String to = resolvePhone(context);
        if (!StringUtils.hasText(to)) {
            return ChannelDispatchResult.permanentFailure("Missing WhatsApp phone in context", Map.of());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "journey");
        metadata.put("assignmentId", assignment.getId());
        metadata.put("stepId", step.getId());
        if (step.getStimulusType() != null) {
            metadata.put("stimulus", step.getStimulusType().name());
        }

        try {
            WhatsAppMessage message = dispatchMessage(account, step, context, to, metadata);
            Map<String, Object> resultMetadata = new HashMap<>();
            resultMetadata.put("to", message.getToNumber());
            WhatsAppMessageType messageType = message.getMessageType();
            if (messageType != null) {
                resultMetadata.put("type", messageType.name());
            }
            if (StringUtils.hasText(message.getMessageId())) {
                resultMetadata.put("messageId", message.getMessageId());
            }
            return ChannelDispatchResult.success(message.getMessageId(), resultMetadata);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError() || ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Instant retryAt = parseRetryAfter(ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null);
                return ChannelDispatchResult.transientFailure(
                        "WhatsApp error: " + ex.getStatusCode(),
                        retryAt,
                        Map.<String, Object>of("body", ex.getResponseBodyAsString()));
            }
            return ChannelDispatchResult.permanentFailure(
                    "WhatsApp error: " + ex.getStatusCode(),
                    Map.<String, Object>of("body", ex.getResponseBodyAsString()));
        } catch (ResourceAccessException ex) {
            log.warn("WhatsApp network error", ex);
            return ChannelDispatchResult.transientFailure("WhatsApp network error", null, Map.of());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ChannelDispatchResult.permanentFailure(ex.getMessage(), Map.of());
        }
    }

    private WhatsAppMessage dispatchMessage(WhatsAppAccount account,
                                            JourneyStep step,
                                            Map<String, Object> context,
                                            String to,
                                            Map<String, Object> metadata) {
        if (step.getMetadata().containsKey("templateName")) {
            String templateName = String.valueOf(step.getMetadata().get("templateName"));
            String templateLanguage = String.valueOf(step.getMetadata().getOrDefault("templateLanguage", "en_US"));
            Map<String, Object> templateData = extractTemplateData(step.getMetadata());
            return messagingService.sendTemplateMessage(account, to, templateName, templateLanguage, templateData, metadata);
        }
        if (step.getMetadata().containsKey("imageUrl")) {
            Object imageUrlObj = step.getMetadata().get("imageUrl");
            if (!(imageUrlObj instanceof String imageUrl) || !StringUtils.hasText(imageUrl)) {
                throw new IllegalArgumentException("Missing WhatsApp image URL");
            }
            String caption = null;
            Object captionObj = step.getMetadata().get("imageCaption");
            if (captionObj instanceof String str && StringUtils.hasText(str)) {
                caption = str;
            }
            return messagingService.sendImageMessage(account, to, imageUrl, caption, metadata);
        }
        String body = resolveBody(step, context);
        return messagingService.sendTextMessage(account, to, body, metadata);
    }

    private Map<String, Object> extractTemplateData(Map<String, Object> metadata) {
        Map<String, Object> templateData = new HashMap<>();
        Object components = metadata.get("templateComponents");
        if (components instanceof List<?> list && !list.isEmpty()) {
            templateData.put("components", list);
        } else if (components instanceof Map<?, ?> map && !map.isEmpty()) {
            templateData.put("components", map);
        }
        Object templateOverrides = metadata.get("templateData");
        if (templateOverrides instanceof Map<?, ?> map && !map.isEmpty()) {
            map.forEach((key, value) -> templateData.put(String.valueOf(key), value));
        }
        return templateData;
    }

    private String resolveBody(JourneyStep step, Map<String, Object> context) {
        Object body = step.getMetadata().get("body");
        if (body instanceof String str && StringUtils.hasText(str)) {
            return str;
        }
        Object message = context.get("message");
        if (message != null) {
            String resolved = message.toString();
            if (StringUtils.hasText(resolved)) {
                return resolved;
            }
        }
        throw new IllegalArgumentException("Missing WhatsApp message body");
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
}
