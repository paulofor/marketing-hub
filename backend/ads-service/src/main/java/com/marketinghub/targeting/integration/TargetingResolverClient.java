package com.marketinghub.targeting.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Cliente responsável por enviar candidatos pendentes para o Facebook Ads Worker.
 */
@Component
public class TargetingResolverClient {
    private static final Logger log = LoggerFactory.getLogger(TargetingResolverClient.class);

    private final RestTemplate restTemplate;
    private final TargetingResolverIntegrationProperties properties;

    public TargetingResolverClient(RestTemplateBuilder restTemplateBuilder,
                                   TargetingResolverIntegrationProperties properties) {
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

    public void requestResolution(TargetingRequest request, List<TargetingCandidate> candidates) {
        if (!properties.isEnabled()) {
            log.debug("Targeting resolver integration disabled; skipping request {}", request != null ? request.getId() : null);
            return;
        }
        if (request == null || CollectionUtils.isEmpty(candidates)) {
            return;
        }
        List<CandidatePayload> payloadCandidates = candidates.stream()
                .filter(this::isPending)
                .map(this::toPayload)
                .toList();
        if (payloadCandidates.isEmpty()) {
            return;
        }
        URI uri = buildUri(request.getId());
        if (uri == null) {
            log.warn("Targeting resolver base URL is not configured; cannot resolve request {}", request.getId());
            return;
        }
        TargetingResolutionPayload payload = new TargetingResolutionPayload(
                resolveAdAccountId(),
                request.getLocale(),
                request.getCountry(),
                properties.getSearchLimit(),
                payloadCandidates
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.postForEntity(uri, new HttpEntity<>(payload, headers), Void.class);
            log.info("Sent {} targeting candidates for resolution (request {})", payloadCandidates.size(), request.getId());
        } catch (RestClientException ex) {
            log.warn("Failed to send targeting request {} for resolution: {}", request.getId(), ex.getMessage(), ex);
        }
    }

    private boolean isPending(TargetingCandidate candidate) {
        return candidate != null && candidate.getStatus() == TargetingCandidateStatus.PENDING_FACEBOOK_MATCH;
    }

    private CandidatePayload toPayload(TargetingCandidate candidate) {
        return new CandidatePayload(
                candidate.getId(),
                candidate.getTextoSugerido(),
                candidate.getType(),
                candidate.getIdioma(),
                candidate.getCountry(),
                candidate.getOrigem(),
                candidate.getScore(),
                candidate.getRationale(),
                candidate.getIntentTag()
        );
    }

    private String resolveAdAccountId() {
        if (StringUtils.hasText(properties.getAdAccountId())) {
            return properties.getAdAccountId();
        }
        return null;
    }

    private URI buildUri(UUID requestId) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            return null;
        }
        String prefix = StringUtils.hasText(properties.getApiPrefix()) ? properties.getApiPrefix() : "";
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(prefix)
                .path("/{requestId}/resolve")
                .buildAndExpand(requestId)
                .toUri();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record TargetingResolutionPayload(
            @JsonProperty("ad_account_id") String adAccountId,
            String locale,
            String country,
            Integer limit,
            List<CandidatePayload> candidates
    ) {
        private TargetingResolutionPayload {
            if (candidates == null) {
                candidates = Collections.emptyList();
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CandidatePayload(
            Long id,
            @JsonProperty("texto_sugerido") String textoSugerido,
            @JsonProperty("tipo") TargetingCandidateType tipo,
            @JsonProperty("idioma") String idioma,
            @JsonProperty("pais") String pais,
            @JsonProperty("origem") String origem,
            @JsonProperty("score") java.math.BigDecimal score,
            @JsonProperty("rationale") String rationale,
            @JsonProperty("intent_tag") String intentTag
    ) {}
}
