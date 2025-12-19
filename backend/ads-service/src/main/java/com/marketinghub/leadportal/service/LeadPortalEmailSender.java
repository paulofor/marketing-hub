package com.marketinghub.leadportal.service;

import com.marketinghub.journey.execution.config.SendGridProperties;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Sends transactional e-mails via SendGrid for lead portal notifications.
 */
@Service
public class LeadPortalEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalEmailSender.class);

    private final RestTemplate restTemplate;
    private final SendGridProperties properties;

    public LeadPortalEmailSender(RestTemplateBuilder restTemplateBuilder, SendGridProperties properties) {
        this.restTemplate = restTemplateBuilder.build();
        this.properties = properties;
    }

    public void sendEmail(String toEmail, String subject, String plainContent, String htmlContent,
                          List<Attachment> attachments) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("SendGrid integration disabled");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("SendGrid API key not configured");
        }
        if (properties.getFromEmail() == null || properties.getFromEmail().isBlank()) {
            throw new IllegalStateException("SendGrid sender e-mail not configured");
        }

        Map<String, Object> payload = buildPayload(toEmail, subject, plainContent, htmlContent, attachments);

        log.info("Sending watermarked sample e-mail to {} (attachments={}, totalBytes={})",
                toEmail,
                attachments != null ? attachments.size() : 0,
                totalAttachmentBytes(attachments));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String url = properties.getBaseUrl() + "/mail/send";
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            if (response.getStatusCode().value() != 202 && !response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("SendGrid responded with status " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "SendGrid error " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            throw new IllegalStateException("Failed to reach SendGrid", ex);
        }
    }

    private Map<String, Object> buildPayload(String toEmail, String subject, String plainContent, String htmlContent,
                                             List<Attachment> attachments) {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> from = new HashMap<>();
        from.put("email", properties.getFromEmail());
        if (properties.getFromName() != null && !properties.getFromName().isBlank()) {
            from.put("name", properties.getFromName());
        }
        payload.put("from", from);
        payload.put("subject", subject);

        Map<String, Object> personalization = new HashMap<>();
        personalization.put("to", List.of(Map.of("email", toEmail)));
        payload.put("personalizations", List.of(personalization));

        if (plainContent != null && !plainContent.isBlank()) {
            payload.computeIfAbsent("content", key -> new ArrayList<Map<String, String>>());
            @SuppressWarnings("unchecked")
            List<Map<String, String>> content = (List<Map<String, String>>) payload.get("content");
            content.add(Map.of(
                    "type", "text/plain",
                    "value", plainContent
            ));
        }
        if (htmlContent != null && !htmlContent.isBlank()) {
            payload.computeIfAbsent("content", key -> new ArrayList<Map<String, String>>());
            @SuppressWarnings("unchecked")
            List<Map<String, String>> content = (List<Map<String, String>>) payload.get("content");
            content.add(Map.of(
                    "type", "text/html",
                    "value", htmlContent
            ));
        }

        if (!CollectionUtils.isEmpty(attachments)) {
            List<Map<String, Object>> attachmentPayload = new ArrayList<>();
            for (Attachment attachment : attachments) {
                if (attachment == null || attachment.content() == null || attachment.content().length == 0) {
                    continue;
                }
                Map<String, Object> attachmentData = new HashMap<>();
                attachmentData.put("content", Base64.getEncoder().encodeToString(attachment.content()));
                attachmentData.put("filename", attachment.fileName());
                attachmentData.put("type", attachment.contentType() != null ? attachment.contentType() : "application/octet-stream");
                attachmentData.put("disposition", "attachment");
                attachmentPayload.add(attachmentData);
            }
            if (!attachmentPayload.isEmpty()) {
                payload.put("attachments", attachmentPayload);
            }
        }

        return payload;
    }

    private long totalAttachmentBytes(List<Attachment> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return 0L;
        }
        long total = 0L;
        for (Attachment attachment : attachments) {
            if (attachment != null && attachment.content() != null) {
                total += attachment.content().length;
            }
        }
        return total;
    }

    public record Attachment(String fileName, String contentType, byte[] content) {
    }
}
