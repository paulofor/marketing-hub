package com.marketinghub.leadportal.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ExperimentFunnelTrackingClient {

    private static final Logger log = LoggerFactory.getLogger(ExperimentFunnelTrackingClient.class);

    public enum TrackingResult {
        FORWARDED,
        SKIPPED,
        FAILED
    }

    private final RestTemplate restTemplate;
    private final URI baseUri;

    public ExperimentFunnelTrackingClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${lead-portal.marketing-hub.funnel-tracking-base-url}") String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("lead-portal.marketing-hub.funnel-tracking-base-url não configurado");
        }
        String sanitized = baseUrl.trim();
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        this.baseUri = URI.create(sanitized);
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public TrackingResult registerRenderComplete(String slug, String visitorId) {
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("O slug do fluxo é obrigatório");
        }

        URI endpoint = UriComponentsBuilder.fromUri(baseUri)
                .path("/flows/{slug}/render-complete")
                .buildAndExpand(slug.trim())
                .toUri();

        HttpEntity<Map<String, String>> entity = buildRenderPayload(visitorId);
        return sendTrackingRequest(endpoint, entity, "render-complete", slug);
    }

    public TrackingResult registerSubmission(String slug, UUID submissionId, Instant submittedAt) {
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("O slug do fluxo é obrigatório");
        }
        if (submissionId == null) {
            throw new IllegalArgumentException("O ID da submissão é obrigatório");
        }

        URI endpoint = UriComponentsBuilder.fromUri(baseUri)
                .path("/flows/{slug}/submission")
                .buildAndExpand(slug.trim())
                .toUri();

        HttpEntity<Map<String, Object>> entity = buildSubmissionPayload(submissionId, submittedAt);
        return sendTrackingRequest(endpoint, entity, "submission", slug);
    }

    private TrackingResult sendTrackingRequest(URI endpoint, HttpEntity<?> entity, String action, String slug) {
        try {
            ResponseEntity<Void> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return TrackingResult.FORWARDED;
            }
            log.warn("Marketing Hub retornou status {} ao registrar {} do fluxo {}",
                    response.getStatusCode(), action, slug);
            return TrackingResult.FAILED;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                log.info("Ignorando {} para fluxo '{}' (status {} retornado pelo Marketing Hub)",
                        action, slug, ex.getStatusCode());
                return TrackingResult.SKIPPED;
            }
            log.warn("Falha ao registrar {} do fluxo '{}' no Marketing Hub (status {})",
                    action, slug, ex.getStatusCode(), ex);
            return TrackingResult.FAILED;
        } catch (RestClientException ex) {
            log.warn("Erro de comunicação ao registrar {} do fluxo '{}'", action, slug, ex);
            return TrackingResult.FAILED;
        }
    }

    private HttpEntity<Map<String, String>> buildRenderPayload(String visitorId) {
        String sanitizedVisitor = visitorId != null ? visitorId.trim() : null;
        Map<String, String> body = StringUtils.hasText(sanitizedVisitor)
                ? Map.of("visitorId", sanitizedVisitor)
                : Collections.emptyMap();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Map<String, Object>> buildSubmissionPayload(UUID submissionId, Instant submittedAt) {
        Map<String, Object> body = new HashMap<>();
        body.put("submissionId", submissionId.toString());
        if (submittedAt != null) {
            body.put("submittedAt", submittedAt.toString());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
