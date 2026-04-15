package com.marketinghub.oprm.integration.client;

import com.marketinghub.oprm.integration.contract.OprmJobClaimRequest;
import com.marketinghub.oprm.integration.contract.OprmJobClaimResponse;
import com.marketinghub.oprm.integration.contract.OprmJobDetailResponse;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BackendJobClient {
    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public BackendJobClient(RestTemplate restTemplate,
                            @Value("${oprm.backend.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendBaseUrl = backendBaseUrl;
    }

    public Optional<OprmJobClaimResponse> claimNextJob(OprmJobClaimRequest request) {
        String endpoint = backendBaseUrl + "/api/oprm/jobs/claim";
        ResponseEntity<OprmJobClaimResponse> response = restTemplate.postForEntity(
                URI.create(endpoint),
                request,
                OprmJobClaimResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Optional.empty();
        }

        return Optional.of(response.getBody());
    }

    public OprmJobDetailResponse getJobDetail(String jobId) {
        String endpoint = backendBaseUrl + "/api/oprm/jobs/" + jobId;
        return restTemplate.getForObject(URI.create(endpoint), OprmJobDetailResponse.class);
    }
}
