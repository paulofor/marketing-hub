package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for ElevenLabs API.
 */
@Component
public class ElevenLabsClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ElevenLabsClient(RestTemplate restTemplate,
                            @Value("${elevenlabs.base-url:https://api.elevenlabs.io}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> createSpeech(String voiceId, Map<String, Object> request) {
        // TODO: auth header
        String url = baseUrl + "/v1/text-to-speech/" + voiceId;
        return restTemplate.postForObject(url, request, Map.class);
    }
}
