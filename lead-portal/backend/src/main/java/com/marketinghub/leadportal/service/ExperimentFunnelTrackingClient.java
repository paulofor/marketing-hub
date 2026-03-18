package com.marketinghub.leadportal.service;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
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

        HttpEntity<Map<String, String>> entity = buildPayload(visitorId);
        try {
            ResponseEntity<Void> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return TrackingResult.FORWARDED;
            }
            log.warn("Marketing Hub retornou status {} ao registrar render-complete do fluxo {}", response.getStatusCode(), slug);
            return TrackingResult.FAILED;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                log.info(
                        "Ignorando render-complete para fluxo '{}' (status {} retornado pelo Marketing Hub)",
                        slug,
                        ex.getStatusCode());
                return TrackingResult.SKIPPED;
            }
            log.warn("Falha ao registrar render-complete do fluxo '{}' no Marketing Hub (status {})", slug, ex.getStatusCode(), ex);
            return TrackingResult.FAILED;
        } catch (RestClientException ex) {
            log.warn("Erro de comunicação ao registrar render-complete do fluxo '{}'", slug, ex);
            return TrackingResult.FAILED;
        }
    }

    private HttpEntity<Map<String, String>> buildPayload(String visitorId) {
        String sanitizedVisitor = visitorId != null ? visitorId.trim() : null;
        Map<String, String> body = StringUtils.hasText(sanitizedVisitor)
                ? Map.of("visitorId", sanitizedVisitor)
                : Collections.emptyMap();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
