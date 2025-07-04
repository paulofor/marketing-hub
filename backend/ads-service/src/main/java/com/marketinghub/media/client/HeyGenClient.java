package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for HeyGen API.
 */
@Component
public class HeyGenClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HeyGenClient(RestTemplate restTemplate,
                        @Value("${heygen.base-url:https://api.heygen.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> createVideo(Map<String, Object> request) {
        // TODO: auth header
        String url = baseUrl + "/v2/video/generate";
        return restTemplate.postForObject(url, request, Map.class);
    }

    public Map<String, Object> getVideo(String id) {
        String url = baseUrl + "/v2/video/" + id;
        return restTemplate.getForObject(url, Map.class);
    }
}
