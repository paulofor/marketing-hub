package com.marketinghub.mois.service;

import com.marketinghub.mois.MoisDiscoveryRequest;
import com.marketinghub.mois.service.MoisResearchGateway.MoisDiscoveredSource;
import com.marketinghub.mois.service.MoisResearchGateway.MoisResearchResult;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class MoisMarketResearchGateway implements MoisResearchGateway {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final MoisMarketResearchProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    public MoisMarketResearchGateway(MoisMarketResearchProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Override
    public MoisResearchResult discoverSources(MoisDiscoveryRequest request, List<String> seedUrls, List<String> seedQueries) {
        List<String> operationalErrors = new ArrayList<>();
        List<String> candidateUrls = new ArrayList<>(sanitize(seedUrls));

        if (properties.isEnabled()) {
            try {
                MarketResearchResponse response = executeMarketResearch(request, candidateUrls, seedQueries);
                if (response != null && response.sources() != null) {
                    candidateUrls.addAll(response.sources());
                }
                if (response != null && response.errorMessage() != null && !response.errorMessage().isBlank()) {
                    operationalErrors.add("market-research-service: " + response.errorMessage().trim());
                }
            } catch (RestClientException ex) {
                operationalErrors.add("market-research-service unavailable: " + ex.getMessage());
            }
        }

        List<MoisDiscoveredSource> captures = new ArrayList<>();
        for (String url : sanitize(candidateUrls)) {
            captures.add(captureUrl(url));
            if (captures.size() >= properties.getMaxSources()) {
                break;
            }
        }

        if (captures.isEmpty()) {
            operationalErrors.add("no source captured for request " + request.getRequestId());
        }

        return new MoisResearchResult(captures, operationalErrors);
    }

    private MarketResearchResponse executeMarketResearch(
            MoisDiscoveryRequest request,
            List<String> seedUrls,
            List<String> seedQueries
    ) {
        String query = String.join(" | ", sanitize(List.of(
                request.getNicheName(),
                request.getMarketTheme(),
                request.getPainOrOutcomeFocus(),
                firstNonBlank(seedQueries)
        )));

        MarketResearchRequest payload = new MarketResearchRequest(
                query,
                sanitize(seedUrls).stream().limit(properties.getMaxSources()).toList(),
                "MOIS discovery for market offer mapping"
        );

        RestTemplate restTemplate = buildRestTemplate();
        URI endpoint = URI.create(properties.getBaseUrl() + "/api/v1/market-research");
        ResponseEntity<MarketResearchResponse> response = restTemplate.postForEntity(endpoint, payload, MarketResearchResponse.class);
        return response.getBody();
    }

    private MoisDiscoveredSource captureUrl(String sourceUrl) {
        RestTemplate restTemplate = buildRestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(sourceUrl, String.class);
            String raw = response.getBody();
            String normalized = normalizeText(raw);
            boolean success = normalized != null && !normalized.isBlank();
            String notes = success
                    ? "captured via mois.market-research gateway"
                    : "empty content from source";
            return new MoisDiscoveredSource(
                    sourceUrl,
                    extractTitle(raw),
                    "landing-page",
                    response.getStatusCode().value(),
                    normalized,
                    notes,
                    success
            );
        } catch (RestClientException ex) {
            return new MoisDiscoveredSource(
                    sourceUrl,
                    null,
                    "landing-page",
                    null,
                    null,
                    "capture error: " + ex.getMessage(),
                    false
            );
        }
    }

    private RestTemplate buildRestTemplate() {
        Duration connectTimeout = properties.getConnectTimeout();
        Duration readTimeout = properties.getReadTimeout();
        return restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    private List<String> sanitize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private String firstNonBlank(List<String> values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalizeText(String html) {
        if (html == null) {
            return "";
        }
        return html
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractTitle(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private record MarketResearchRequest(String query, List<String> sources, String analysisObjective) {
    }

    private record MarketResearchResponse(Long id, String status, List<String> sources, String errorMessage, Map<String, Object> extra) {
    }
}
