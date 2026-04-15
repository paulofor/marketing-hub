package com.marketinghub.oprm.integration.client;

import com.marketinghub.oprm.integration.contract.OprmFeedbackHistoryEntryResponse;
import com.marketinghub.oprm.integration.contract.OprmFeedbackPublishRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class BackendFeedbackClient {
    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public BackendFeedbackClient(RestTemplate restTemplate,
                                 @Value("${oprm.backend.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendBaseUrl = backendBaseUrl;
    }

    public void publish(OprmFeedbackPublishRequest request) {
        String endpoint = backendBaseUrl + "/api/oprm/feedback";
        restTemplate.exchange(URI.create(endpoint), HttpMethod.POST, new HttpEntity<>(request), Void.class);
    }

    public List<OprmFeedbackHistoryEntryResponse> loadHistory(String occupationName, String personaLabel) {
        URI endpoint = UriComponentsBuilder.fromHttpUrl(backendBaseUrl)
                .path("/api/oprm/feedback/history")
                .queryParam("occupationName", occupationName)
                .queryParam("personaLabel", personaLabel)
                .build(true)
                .toUri();

        OprmFeedbackHistoryEntryResponse[] response = restTemplate.getForObject(endpoint, OprmFeedbackHistoryEntryResponse[].class);
        if (response == null || response.length == 0) {
            return List.of();
        }
        return Arrays.asList(response);
    }
}
