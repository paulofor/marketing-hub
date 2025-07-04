package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for Runway API.
 */
@Component
public class RunwayClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RunwayClient(RestTemplate restTemplate,
                        @Value("${runway.base-url:https://api.runwayml.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> createClip(Map<String, Object> request) {
        // TODO: auth header
        String url = baseUrl + "/v1/generate";
        return restTemplate.postForObject(url, request, Map.class);
    }

    public Map<String, Object> getJob(String id) {
        String url = baseUrl + "/v1/jobs/" + id;
        return restTemplate.getForObject(url, Map.class);
    }
}
