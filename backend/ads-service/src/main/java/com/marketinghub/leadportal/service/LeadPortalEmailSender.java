package com.marketinghub.leadportal.service;

import com.marketinghub.journey.execution.config.SendGridProperties;
import java.nio.charset.StandardCharsets;
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
                          byte[] attachment, String attachmentName) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("SendGrid integration disabled");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("SendGrid API key not configured");
        }
        if (properties.getFromEmail() == null || properties.getFromEmail().isBlank()) {
            throw new IllegalStateException("SendGrid sender e-mail not configured");
        }

        Map<String, Object> payload = buildPayload(toEmail, subject, plainContent, htmlContent, attachment, attachmentName);

        log.info("Sending watermarked sample e-mail to {}", toEmail);

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
                                             byte[] attachment, String attachmentName) {
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
            payload.computeIfAbsent("content", key -> new java.util.ArrayList<Map<String, String>>());
            @SuppressWarnings("unchecked")
            List<Map<String, String>> content = (List<Map<String, String>>) payload.get("content");
            content.add(Map.of(
                    "type", "text/plain",
                    "value", plainContent
            ));
        }
        if (htmlContent != null && !htmlContent.isBlank()) {
            payload.computeIfAbsent("content", key -> new java.util.ArrayList<Map<String, String>>());
            @SuppressWarnings("unchecked")
            List<Map<String, String>> content = (List<Map<String, String>>) payload.get("content");
            content.add(Map.of(
                    "type", "text/html",
                    "value", htmlContent
            ));
        }

        if (attachment != null && attachment.length > 0) {
            Map<String, Object> attachmentPayload = new HashMap<>();
            attachmentPayload.put("content", Base64.getEncoder().encodeToString(attachment));
            attachmentPayload.put("filename", attachmentName);
            attachmentPayload.put("type", "application/zip");
            attachmentPayload.put("disposition", "attachment");
            payload.put("attachments", List.of(attachmentPayload));
        }

        return payload;
    }
}
