package com.marketinghub.oprm.integration.client;

import com.marketinghub.oprm.integration.contract.OprmArtifactPublishRequest;
import com.marketinghub.oprm.integration.contract.OprmArtifactPublishResponse;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BackendArtifactPublishClient {
    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public BackendArtifactPublishClient(RestTemplate restTemplate,
                                        @Value("${oprm.backend.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendBaseUrl = backendBaseUrl;
    }

    public OprmArtifactPublishResponse publish(OprmArtifactPublishRequest request) {
        String endpoint = backendBaseUrl + "/api/oprm/artifacts";
        ResponseEntity<OprmArtifactPublishResponse> response = restTemplate.postForEntity(
                URI.create(endpoint),
                request,
                OprmArtifactPublishResponse.class
        );
        return response.getBody();
    }
}
