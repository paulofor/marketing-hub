package com.marketinghub.leadportal.payments;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LeadPortalPaymentsClient {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPaymentsClient.class);

    private final RestTemplate restTemplate;
    private final LeadPortalPaymentsProperties properties;

    public LeadPortalPaymentsClient(RestTemplateBuilder restTemplateBuilder,
                                    LeadPortalPaymentsProperties properties) {
        this.properties = properties;
        RestTemplateBuilder builder = restTemplateBuilder;
        if (properties.getConnectTimeout() != null) {
            builder = builder.setConnectTimeout(properties.getConnectTimeout());
        }
        if (properties.getReadTimeout() != null) {
            builder = builder.setReadTimeout(properties.getReadTimeout());
        }
        this.restTemplate = builder.build();
    }

    public boolean isEnabled() {
        return properties.isEnabled() && StringUtils.hasText(properties.getBaseUrl());
    }

    public Optional<LeadPortalCheckoutResponse> createCheckout(long packageId, String buyerEmail, String buyerName) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getCheckoutPath())
                .build()
                .toUri();

        LeadPortalCheckoutRequest payload = new LeadPortalCheckoutRequest(packageId, buyerEmail, buyerName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (StringUtils.hasText(properties.getAuthToken())) {
            headers.setBearerAuth(properties.getAuthToken());
        }

        try {
            ResponseEntity<LeadPortalCheckoutResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    LeadPortalCheckoutResponse.class);
            LeadPortalCheckoutResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.checkoutUrl())) {
                log.warn("Serviço de pagamentos respondeu sem checkoutUrl para o pacote {}", packageId);
                return Optional.empty();
            }
            return Optional.of(body);
        } catch (RestClientResponseException ex) {
            log.error("Erro {} ao criar checkout no serviço de pagamentos para o pacote {}: {}",
                    ex.getStatusCode().value(), packageId, ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            log.error("Falha ao chamar o serviço de pagamentos para o pacote {}", packageId, ex);
        }
        return Optional.empty();
    }
}
