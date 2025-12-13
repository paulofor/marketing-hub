package com.marketinghub.emailservice.service.client;

import com.marketinghub.emailservice.config.MarketingHubClientProperties;
import com.marketinghub.emailservice.exception.RemoteServiceException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LeadPortalImagePackageClient {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageClient.class);

    private final RestClient leadPortalRestClient;
    private final MarketingHubClientProperties properties;

    public LeadPortalImagePackageClient(@Qualifier("leadPortalRestClient") RestClient leadPortalRestClient,
                                        MarketingHubClientProperties properties) {
        this.leadPortalRestClient = leadPortalRestClient;
        this.properties = properties;
    }

    public List<LeadPortalImagePackageExportResponse> fetchPackages(int limit) {
        int normalizedLimit = Math.max(1, limit);
        try {
            ResponseEntity<List<LeadPortalImagePackageExportResponse>> response = leadPortalRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.leadPortalPackagesBasePath())
                            .path("/export")
                            .queryParam("limit", normalizedLimit)
                            .build())
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {
                    });
            List<LeadPortalImagePackageExportResponse> body = response.getBody();
            return body != null ? body : List.of();
        } catch (RestClientException ex) {
            log.error("Falha ao consultar pacotes de imagens concluídos", ex);
            throw new RemoteServiceException("Falha ao consultar pacotes de imagens concluídos", ex);
        }
    }

    public void acknowledge(long packageId, boolean success, String errorMessage) {
        try {
            leadPortalRestClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.leadPortalPackagesBasePath())
                            .path("/")
                            .path(String.valueOf(packageId))
                            .path("/ack")
                            .build())
                    .body(new LeadPortalImagePackageAckRequest(success, errorMessage))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Falha ao confirmar processamento do pacote {}", packageId, ex);
            throw new RemoteServiceException("Falha ao confirmar processamento do pacote " + packageId, ex);
        }
    }
}
