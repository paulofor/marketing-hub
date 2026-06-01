package com.marketinghub.whatsapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.journey.execution.config.WhatsAppProperties;
import com.marketinghub.whatsapp.*;
import com.marketinghub.repository.jpa.whatsapp.WhatsAppMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

/**
 * Encapsulates low-level interactions with the Meta WhatsApp Cloud API.
 */
@Service
@Slf4j
public class WhatsAppMessagingService {
    private final RestTemplate restTemplate;
    private final WhatsAppAccountService accountService;
    private final WhatsAppMessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final WhatsAppProperties properties;

    @Autowired
    public WhatsAppMessagingService(RestTemplateBuilder restTemplateBuilder,
                                    WhatsAppAccountService accountService,
                                    WhatsAppMessageRepository messageRepository,
                                    ObjectMapper objectMapper,
                                    WhatsAppProperties properties) {
        this(restTemplateBuilder.build(), accountService, messageRepository, objectMapper, properties);
    }

    WhatsAppMessagingService(RestTemplate restTemplate,
                              WhatsAppAccountService accountService,
                              WhatsAppMessageRepository messageRepository,
                              ObjectMapper objectMapper,
                              WhatsAppProperties properties) {
        this.restTemplate = restTemplate;
        this.accountService = accountService;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled() && accountService.findActiveAccount().isPresent();
    }

    public Optional<WhatsAppAccount> findActiveAccount() {
        return accountService.findActiveAccount();
    }

    public WhatsAppMessage sendTextMessage(String to, String body, Map<String, Object> metadata) {
        WhatsAppAccount account = accountService.requireActiveAccount();
        return sendTextMessage(account, to, body, metadata);
    }

    public WhatsAppMessage sendTextMessage(WhatsAppAccount account, String to, String body, Map<String, Object> metadata) {
        Map<String, Object> payload = basePayload(to);
        payload.put("type", "text");
        Map<String, Object> text = new HashMap<>();
        text.put("preview_url", Boolean.FALSE);
        text.put("body", body);
        payload.put("text", text);
        return sendPayload(account, payload, WhatsAppMessageType.TEXT, to, body, null, null, metadata);
    }

    public WhatsAppMessage sendTemplateMessage(WhatsAppAccount account,
                                               String to,
                                               String templateName,
                                               String templateLanguage,
                                               Map<String, Object> templateData,
                                               Map<String, Object> metadata) {
        Map<String, Object> payload = basePayload(to);
        payload.put("type", "template");
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        Map<String, Object> language = new HashMap<>();
        language.put("code", StringUtils.hasText(templateLanguage) ? templateLanguage : "en_US");
        template.put("language", language);
        if (templateData != null && !templateData.isEmpty()) {
            template.putAll(templateData);
        }
        payload.put("template", template);
        return sendPayload(account, payload, WhatsAppMessageType.TEMPLATE, to, null, null, null, metadata);
    }

    public WhatsAppMessage sendImageMessage(String to, String imageUrl, String caption, Map<String, Object> metadata) {
        WhatsAppAccount account = accountService.requireActiveAccount();
        return sendImageMessage(account, to, imageUrl, caption, metadata);
    }

    public WhatsAppMessage sendImageMessage(WhatsAppAccount account, String to, String imageUrl, String caption, Map<String, Object> metadata) {
        Map<String, Object> payload = basePayload(to);
        payload.put("type", "image");
        Map<String, Object> image = new HashMap<>();
        image.put("link", imageUrl);
        if (StringUtils.hasText(caption)) {
            image.put("caption", caption);
        }
        payload.put("image", image);
        return sendPayload(account, payload, WhatsAppMessageType.IMAGE, to, null, imageUrl, caption, metadata);
    }

    @Transactional
    public void handleWebhook(JsonNode payload) {
        if (payload == null || !payload.has("entry")) {
            return;
        }
        for (JsonNode entry : payload.get("entry")) {
            JsonNode changes = entry.path("changes");
            for (JsonNode change : changes) {
                JsonNode value = change.path("value");
                String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                Optional<WhatsAppAccount> accountOpt = accountService.findByPhoneNumberId(phoneNumberId);
                if (accountOpt.isEmpty()) {
                    continue;
                }
                WhatsAppAccount account = accountOpt.get();
                if (value.has("messages")) {
                    for (JsonNode messageNode : value.get("messages")) {
                        recordInboundMessage(account, messageNode, value);
                    }
                }
                if (value.has("statuses")) {
                    for (JsonNode statusNode : value.get("statuses")) {
                        updateMessageStatus(account, statusNode);
                    }
                }
            }
        }
    }

    private void recordInboundMessage(WhatsAppAccount account, JsonNode messageNode, JsonNode value) {
        String messageId = messageNode.path("id").asText(null);
        if (StringUtils.hasText(messageId) && messageRepository.findByMessageId(messageId).isPresent()) {
            return;
        }
        WhatsAppMessage message = new WhatsAppMessage();
        message.setAccount(account);
        message.setDirection(WhatsAppMessageDirection.INBOUND);
        message.setMessageType(resolveType(messageNode.path("type").asText(null)));
        message.setMessageId(messageId);
        message.setFromNumber(messageNode.path("from").asText(null));
        message.setToNumber(account.getPhoneNumber());
        message.setStatus("RECEIVED");
        message.setTextBody(messageNode.path("text").path("body").asText(null));
        JsonNode imageNode = messageNode.path("image");
        if (!imageNode.isMissingNode() && !imageNode.isNull()) {
            message.setImageId(imageNode.path("id").asText(null));
            message.setImageUrl(imageNode.path("link").asText(null));
            message.setMimeType(imageNode.path("mime_type").asText(null));
            message.setCaption(imageNode.path("caption").asText(null));
        }
        message.setConversationId(value.path("conversation").path("id").asText(null));
        message.setContextJson(extractMetadataJson(value));
        message.setPayloadJson(messageNode.toString());
        message.setMessageTimestamp(parseTimestamp(messageNode.path("timestamp").asText(null)));
        message.setReceivedAt(Instant.now());
        messageRepository.save(message);
    }

    private void updateMessageStatus(WhatsAppAccount account, JsonNode statusNode) {
        String messageId = statusNode.path("id").asText(null);
        if (!StringUtils.hasText(messageId)) {
            return;
        }
        messageRepository.findByMessageId(messageId).ifPresent(message -> {
            message.setStatus(statusNode.path("status").asText(null));
            message.setConversationId(statusNode.path("conversation").path("id").asText(message.getConversationId()));
            message.setMessageTimestamp(parseTimestamp(statusNode.path("timestamp").asText(null)));
            if (statusNode.has("errors") && statusNode.get("errors").isArray() && statusNode.get("errors").size() > 0) {
                JsonNode error = statusNode.get("errors").get(0);
                message.setErrorCode(error.path("code").asText(null));
                message.setErrorMessage(error.path("message").asText(null));
            }
            message.setStatusPayloadJson(statusNode.toString());
            messageRepository.save(message);
        });
    }

    private WhatsAppMessageType resolveType(String type) {
        if (!StringUtils.hasText(type)) {
            return WhatsAppMessageType.UNKNOWN;
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "text" -> WhatsAppMessageType.TEXT;
            case "image" -> WhatsAppMessageType.IMAGE;
            case "document" -> WhatsAppMessageType.DOCUMENT;
            case "template" -> WhatsAppMessageType.TEMPLATE;
            default -> WhatsAppMessageType.UNKNOWN;
        };
    }

    private Map<String, Object> basePayload(String to) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        return payload;
    }

    private WhatsAppMessage sendPayload(WhatsAppAccount account,
                                        Map<String, Object> payload,
                                        WhatsAppMessageType messageType,
                                        String to,
                                        String textBody,
                                        String imageUrl,
                                        String caption,
                                        Map<String, Object> metadata) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("WhatsApp integration disabled");
        }
        if (!StringUtils.hasText(account.getAccessToken()) || !StringUtils.hasText(account.getPhoneNumberId())) {
            throw new IllegalStateException("WhatsApp credentials missing");
        }
        WhatsAppMessage message = WhatsAppMessage.builder()
                .account(account)
                .direction(WhatsAppMessageDirection.OUTBOUND)
                .messageType(messageType)
                .fromNumber(account.getPhoneNumber())
                .toNumber(to)
                .textBody(textBody)
                .imageUrl(imageUrl)
                .caption(caption)
                .status("PENDING")
                .contextJson(writeJson(metadata))
                .payloadJson(writeJson(payload))
                .build();
        message = messageRepository.save(message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(account.getAccessToken());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        String baseUrl = StringUtils.hasText(account.getBaseUrl()) ? account.getBaseUrl() : properties.getBaseUrl();
        String url = baseUrl + "/" + account.getPhoneNumberId() + "/messages";

        try {
            ResponseEntity<WhatsAppResponse> response = restTemplate.exchange(url, HttpMethod.POST, request, WhatsAppResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().messages() != null) {
                List<WhatsAppResponse.Message> messages = response.getBody().messages();
                if (!messages.isEmpty()) {
                    message.setMessageId(messages.get(0).id());
                }
            }
            message.setStatus("SENT");
            message.setSentAt(Instant.now());
            message.setMessageTimestamp(message.getSentAt());
            return messageRepository.save(message);
        } catch (HttpStatusCodeException ex) {
            updateMessageWithError(message, "HTTP_" + ex.getStatusCode().value(), ex.getStatusCode().toString(), ex.getResponseBodyAsString());
            throw ex;
        } catch (ResourceAccessException ex) {
            updateMessageWithError(message, "NETWORK_ERROR", null, ex.getMessage());
            throw ex;
        }
    }

    private void updateMessageWithError(WhatsAppMessage message, String status, String errorCode, String errorMessage) {
        message.setStatus(status);
        message.setErrorCode(errorCode);
        message.setErrorMessage(errorMessage);
        messageRepository.save(message);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Unable to serialize WhatsApp payload", ex);
            return null;
        }
    }

    private String extractMetadataJson(JsonNode value) {
        ObjectNode node = objectMapper.createObjectNode();
        if (value.has("metadata")) {
            node.set("metadata", value.get("metadata"));
        }
        if (value.has("contacts")) {
            node.set("contacts", value.get("contacts"));
        }
        return node.isEmpty() ? null : node.toString();
    }

    private Instant parseTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return null;
        }
        try {
            long epochSeconds = Long.parseLong(timestamp);
            return Instant.ofEpochSecond(epochSeconds);
        } catch (NumberFormatException ex) {
            try {
                return Instant.parse(timestamp);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private record WhatsAppResponse(List<Message> messages) {
        private record Message(String id) {
        }
    }
}
