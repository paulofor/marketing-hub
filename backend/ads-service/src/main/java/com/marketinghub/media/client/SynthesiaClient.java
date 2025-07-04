package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for Synthesia API.
 */
@Component
public class SynthesiaClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SynthesiaClient(RestTemplate restTemplate,
                           @Value("${synthesia.base-url:https://api.synthesia.io}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Create video.
     */
    public Map<String, Object> createVideo(Map<String, Object> request) {
        // TODO: add authentication header
        String url = baseUrl + "/v2/videos";
        return restTemplate.postForObject(url, request, Map.class);
    }

    public Map<String, Object> getVideo(String id) {
        String url = baseUrl + "/v2/videos/" + id;
        return restTemplate.getForObject(url, Map.class);
    }
}
