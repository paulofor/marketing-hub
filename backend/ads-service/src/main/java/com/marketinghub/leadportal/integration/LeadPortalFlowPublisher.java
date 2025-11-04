package com.marketinghub.leadportal.integration;

import com.marketinghub.leadportal.LeadPortalFlow;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Publishes approved lead portal flows to the public lead portal application.
 */
@Component
public class LeadPortalFlowPublisher {

    private final RestTemplate restTemplate;
    private final LeadPortalIntegrationProperties properties;

    public LeadPortalFlowPublisher(RestTemplate restTemplate, LeadPortalIntegrationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public void publish(LeadPortalFlow flow) {
        if (!properties.isEnabled()) {
            return;
        }
        URI uri = buildUri(flow.getSlug());
        LeadPortalFlowPublicationRequest payload = LeadPortalFlowPublicationRequest.from(flow);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.put(uri, new HttpEntity<>(payload, headers));
        } catch (RestClientException ex) {
            throw new LeadPortalPublicationException("Failed to publish lead portal flow " + flow.getSlug(), ex);
        }
    }

    public void remove(String slug) {
        if (!properties.isEnabled()) {
            return;
        }
        URI uri = buildUri(slug);
        try {
            restTemplate.delete(uri);
        } catch (RestClientException ex) {
            throw new LeadPortalPublicationException("Failed to remove lead portal flow " + slug, ex);
        }
    }

    private URI buildUri(String slug) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new LeadPortalPublicationException("Lead portal base URL is not configured");
        }
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("/api/flows/{slug}")
                .buildAndExpand(slug)
                .toUri();
    }
}
