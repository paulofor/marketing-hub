package com.marketinghub.oprm.integration.client;

import com.marketinghub.oprm.integration.contract.OprmHeartbeatRequest;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BackendHeartbeatClient {
    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public BackendHeartbeatClient(RestTemplate restTemplate,
                                  @Value("${oprm.backend.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendBaseUrl = backendBaseUrl;
    }

    public void publish(OprmHeartbeatRequest request) {
        String endpoint = backendBaseUrl + "/api/oprm/heartbeat";
        restTemplate.exchange(URI.create(endpoint), HttpMethod.POST, new HttpEntity<>(request), Void.class);
    }
}

