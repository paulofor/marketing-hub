package com.marketinghub.leadportal.integration;

import com.marketinghub.leadportal.LeadPortalFlow;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Publishes approved lead portal flows to the public lead portal application.
 */
@Component
public class LeadPortalFlowPublisher {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalFlowPublisher.class);
    private static final String SIMPLE_FLOW_MANAGED_MESSAGE =
            "Fluxos simples são gerenciados automaticamente e não podem ser editados.";

    private final RestTemplate restTemplate;
    private final LeadPortalIntegrationProperties properties;
    private final ExperimentHeroImageResolver heroImageResolver;

    public LeadPortalFlowPublisher(RestTemplate restTemplate,
                                   LeadPortalIntegrationProperties properties,
                                   ExperimentHeroImageResolver heroImageResolver) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.heroImageResolver = heroImageResolver;
    }

    public void publish(LeadPortalFlow flow) {
        if (!properties.isEnabled()) {
            return;
        }
        URI uri = buildUri(flow.getSlug());
        String heroImageOverride = heroImageResolver.resolve(flow.getExperiment()).orElse(null);
        LeadPortalFlowPublicationRequest payload = LeadPortalFlowPublicationRequest.from(flow, heroImageOverride);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.put(uri, new HttpEntity<>(payload, headers));
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST && isManagedSimpleFlowRejection(ex)) {
                log.info(
                        "Skipping publication for managed simple flow '{}': {}",
                        flow.getSlug(),
                        SIMPLE_FLOW_MANAGED_MESSAGE);
                return;
            }
            throw new LeadPortalPublicationException("Failed to publish lead portal flow " + flow.getSlug(), ex);
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

    private boolean isManagedSimpleFlowRejection(HttpClientErrorException ex) {
        String body = ex.getResponseBodyAsString();
        return body != null && body.contains(SIMPLE_FLOW_MANAGED_MESSAGE);
    }
}
