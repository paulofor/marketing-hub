package com.marketinghub.oprm.integration.client;

import com.marketinghub.oprm.integration.contract.OprmJobStatusUpdateRequest;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BackendStatusClient {
    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public BackendStatusClient(RestTemplate restTemplate,
                               @Value("${oprm.backend.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendBaseUrl = backendBaseUrl;
    }

    public void updateStatus(String jobId, OprmJobStatusUpdateRequest request) {
        String endpoint = backendBaseUrl + "/api/oprm/jobs/" + jobId + "/status";
        restTemplate.exchange(URI.create(endpoint), HttpMethod.POST, new HttpEntity<>(request), Void.class);
    }
}
