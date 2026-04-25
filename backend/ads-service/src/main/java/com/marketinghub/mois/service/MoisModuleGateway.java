package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MoisModuleGateway {

    private final MoisModuleProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    public MoisModuleGateway(MoisModuleProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse createDiscoveryRequest(MoisDiscoveryDtos.CreateDiscoveryRequest request) {
        return exchange("/api/v1/mois/discovery-requests", HttpMethod.POST, request,
                MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse.class).getBody();
    }

    public MoisDiscoveryDtos.DiscoveryRequestListResponse listDiscoveryRequests(String status, String nicheName, String marketTheme) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/mois/discovery-requests");
        maybeAddQuery(uri, "status", status);
        maybeAddQuery(uri, "nicheName", nicheName);
        maybeAddQuery(uri, "marketTheme", marketTheme);
        return exchange(uri.toUriString(), HttpMethod.GET, null, MoisDiscoveryDtos.DiscoveryRequestListResponse.class)
                .getBody();
    }

    public Optional<MoisDiscoveryDtos.DiscoveryRequestDetailResponse> getDiscoveryRequest(String requestId) {
        return optionalGet("/api/v1/mois/discovery-requests/" + requestId, MoisDiscoveryDtos.DiscoveryRequestDetailResponse.class);
    }

    public MoisDiscoveryDtos.AsyncAcceptedResponse runDiscoveryRequest(String requestId) {
        return exchange("/api/v1/mois/discovery-requests/" + requestId + "/run", HttpMethod.POST, null,
                MoisDiscoveryDtos.AsyncAcceptedResponse.class).getBody();
    }

    public MoisOfferDtos.OfferCardListResponse listOffers(String requestId, String nicheName, String sellerOrBrand) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/mois/offers");
        maybeAddQuery(uri, "requestId", requestId);
        maybeAddQuery(uri, "nicheName", nicheName);
        maybeAddQuery(uri, "sellerOrBrand", sellerOrBrand);
        return exchange(uri.toUriString(), HttpMethod.GET, null, MoisOfferDtos.OfferCardListResponse.class).getBody();
    }

    public Optional<MoisOfferDtos.OfferCardResponse> getOffer(String offerId) {
        return optionalGet("/api/v1/mois/offers/" + offerId, MoisOfferDtos.OfferCardResponse.class);
    }

    public MoisInsightDtos.InsightReportListResponse listInsightReports(String requestId, String nicheName, String category) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/mois/insight-reports");
        maybeAddQuery(uri, "requestId", requestId);
        maybeAddQuery(uri, "nicheName", nicheName);
        maybeAddQuery(uri, "category", category);
        return exchange(uri.toUriString(), HttpMethod.GET, null, MoisInsightDtos.InsightReportListResponse.class).getBody();
    }

    public Optional<MoisInsightDtos.InsightReportResponse> getInsightReport(String reportId) {
        return optionalGet("/api/v1/mois/insight-reports/" + reportId, MoisInsightDtos.InsightReportResponse.class);
    }

    public Optional<MoisInsightDtos.InsightExecutiveSummaryResponse> getInsightExecutiveSummary(String reportId) {
        return optionalGet("/api/v1/mois/insight-reports/" + reportId + "/executive-summary",
                MoisInsightDtos.InsightExecutiveSummaryResponse.class);
    }

    public Optional<MoisArtifactDtos.ArtifactEnvelopeResponse> getArtifact(String artifactId) {
        return optionalGet("/api/v1/mois/artifacts/" + artifactId, MoisArtifactDtos.ArtifactEnvelopeResponse.class);
    }

    public MoisWorkspaceDtos.CollectionJobResponse createCollectionJob(MoisWorkspaceDtos.CreateCollectionJobRequest request) {
        return exchange("/api/v1/mois/collection-jobs", HttpMethod.POST, request, MoisWorkspaceDtos.CollectionJobResponse.class)
                .getBody();
    }

    public MoisWorkspaceDtos.CollectionJobListResponse listCollectionJobs(String workspaceId, String status) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/mois/collection-jobs");
        maybeAddQuery(uri, "workspaceId", workspaceId);
        maybeAddQuery(uri, "status", status);
        return exchange(uri.toUriString(), HttpMethod.GET, null, MoisWorkspaceDtos.CollectionJobListResponse.class).getBody();
    }

    public Optional<MoisWorkspaceDtos.CollectedReferenceListResponse> listCollectedReferencesByJob(String jobId) {
        return optionalGet("/api/v1/mois/collection-jobs/" + jobId + "/references",
                MoisWorkspaceDtos.CollectedReferenceListResponse.class);
    }

    public Map<String, String> health() {
        return exchange("/api/v1/mois/health", HttpMethod.GET, null, Map.class).getBody();
    }

    private <T> Optional<T> optionalGet(String path, Class<T> responseType) {
        try {
            return Optional.ofNullable(exchange(path, HttpMethod.GET, null, responseType).getBody());
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
    }

    private <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
        try {
            return buildRestTemplate().exchange(path, method, new HttpEntity<>(body), responseType);
        } catch (HttpClientErrorException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "mois module unavailable",
                    ex
            );
        }
    }

    private RestTemplate buildRestTemplate() {
        return restTemplateBuilder
                .rootUri(properties.getBaseUrl())
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }

    private void maybeAddQuery(UriComponentsBuilder uri, String key, String value) {
        if (StringUtils.hasText(value)) {
            uri.queryParam(key, value);
        }
    }
}
